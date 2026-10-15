package com.dude555afk.xeno.ui.browser

import android.app.Application
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dude555afk.xeno.core.ai.AiClient
import com.dude555afk.xeno.core.blocking.FilterListEngine
import com.dude555afk.xeno.core.blocking.HostsList
import com.dude555afk.xeno.core.blocking.ResourceType
import com.dude555afk.xeno.core.engine.SessionListener
import com.dude555afk.xeno.core.engine.SessionPool
import com.dude555afk.xeno.core.engine.XenoAssetLoader
import com.dude555afk.xeno.core.engine.XenoWebViewClient
import com.dude555afk.xeno.core.fetch.AnswerEngine
import com.dude555afk.xeno.core.fetch.PageFetcher
import com.dude555afk.xeno.core.fetch.SearchEngine
import com.dude555afk.xeno.core.model.AnswerResult
import com.dude555afk.xeno.core.model.PageLoadState
import com.dude555afk.xeno.core.model.Space
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.core.reader.ReadabilityInjector
import com.dude555afk.xeno.core.storage.HistoryEntity
import com.dude555afk.xeno.core.storage.SettingsStore
import com.dude555afk.xeno.core.storage.SpaceEntity
import com.dude555afk.xeno.core.storage.TabEntity
import com.dude555afk.xeno.core.storage.XenoDatabase
import com.dude555afk.xeno.core.summarize.Prompts
import com.dude555afk.xeno.core.summarize.Summarizer
import com.dude555afk.xeno.core.summarize.SummaryOutcome
import com.dude555afk.xeno.XenoApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Browser UI state — every overlay surface lives here. */
data class BrowserUiState(
    val spaces: List<Space> = emptyList(),
    val activeSpace: Space? = null,
    val tabs: List<Tab> = emptyList(),
    val activeTab: Tab? = null,
    val load: PageLoadState = PageLoadState(),
    val paletteVisible: Boolean = false,
    val query: String = "",
    val isQuestion: Boolean = false,
    val tabStackVisible: Boolean = false,
    val answer: AnswerResult? = null,
    val answerStreaming: Boolean = false,
    val answerError: String? = null,
    val summaryVisible: Boolean = false,
    val summaryText: String = "",
    val summaryOffline: Boolean = false,
    val summaryError: String? = null,
    val summaryTone: Prompts.Tone = Prompts.Tone.Terse,
    val blockingEnabled: Boolean = true,
    val hasApiKey: Boolean = false
)

/**
 * Owns the whole app's behaviour.
 *
 * Deliberately one ViewModel. Splitting into per-screen ViewModels would mean
 * several places holding the session pool, which is exactly how the 2-session
 * invariant gets quietly broken.
 */
class BrowserViewModel(app: Application) : AndroidViewModel(app) {

    private val db = XenoDatabase.get(app)
    private val settings = SettingsStore(app)

    /**
     * Reuse the filter engine the Application built rather than making a second
     * one. Parsing the bundled list twice because an Activity got recreated is
     * the kind of tax this app exists to avoid.
     */
    private val filters: FilterListEngine =
        (app as? XenoApplication)?.filters ?: FilterListEngine().also { engine ->
            viewModelScope.launch(Dispatchers.Default) { engine.load(HostsList.rules) }
        }

    private val assetLoader = XenoAssetLoader.create(app)

    private val pool = SessionPool(app)
    private val injector = ReadabilityInjector(app)
    private val ai = AiClient()
    private val summarizer = Summarizer(ai)
    private val fetcher = PageFetcher(app, injector)
    private val engine = AnswerEngine(SearchEngine(app), fetcher, ai)

    private val _state = MutableStateFlow(
        BrowserUiState(
            blockingEnabled = settings.blockingEnabled,
            hasApiKey = settings.hasApiKey
        )
    )
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    private var answerJob: Job? = null
    private var summaryJob: Job? = null

    /** The tab the WebView session is currently showing. */
    private var loadedTabId: Long? = null

    init {
        // The bundled list is already loading in XenoApplication; refresh the
        // real uBO lists in the background afterwards. Best-effort — on failure
        // the bundled list still covers the common ad infrastructure.
        refreshFilterLists()

        viewModelScope.launch {
            if (db.spaceDao().count() == 0) {
                val seed = com.dude555afk.xeno.core.model.defaultSpace()
                db.spaceDao().insert(
                    SpaceEntity(
                        name = seed.name,
                        accentArgb = seed.accent.toArgb()
                    )
                )
            }
            db.tabDao().deletePeekTabs()
        }

        viewModelScope.launch {
            db.spaceDao().observeAll().collect { entities ->
                val spaces = entities.map {
                    Space(
                        id = it.id,
                        name = it.name,
                        accent = androidx.compose.ui.graphics.Color(it.accentArgb),
                        position = it.position,
                        pinned = it.pinned
                    )
                }
                _state.value = _state.value.copy(spaces = spaces)
                if (_state.value.activeSpace == null && spaces.isNotEmpty()) {
                    selectSpace(spaces.first())
                }
            }
        }

        // Tabs for whichever space is active.
        viewModelScope.launch {
            _state.collect { snapshot ->
                val space = snapshot.activeSpace ?: return@collect
                db.tabDao().observeForSpace(space.id).collect { entities ->
                    val tabs = entities.map {
                        Tab(
                            id = it.id,
                            spaceId = it.spaceId,
                            url = it.url,
                            title = it.title,
                            isPeek = it.isPeek,
                            position = it.position
                        )
                    }
                    _state.value = _state.value.copy(tabs = tabs)
                    if (_state.value.activeTab == null && tabs.isNotEmpty()) {
                        selectTab(tabs.first())
                    }
                }
            }
        }
    }

    // MARK: spaces & tabs

    fun selectSpace(space: Space) {
        settings.activeSpaceId = space.id
        _state.value = _state.value.copy(activeSpace = space)
    }

    fun selectTab(tab: Tab) {
        _state.value = _state.value.copy(activeTab = tab, load = PageLoadState(url = tab.url))
        syncSessionToTab(tab)
    }

    fun newTab() {
        viewModelScope.launch {
            val spaceId = _state.value.activeSpace?.id ?: 1L
            val id = db.tabDao().insert(TabEntity(spaceId = spaceId, url = ""))
            selectTab(Tab(id = id, spaceId = spaceId))
            _state.value = _state.value.copy(tabStackVisible = false, paletteVisible = true)
        }
    }

    fun closeTab(tab: Tab) {
        viewModelScope.launch {
            db.tabDao().deleteById(tab.id)
            pool.destroy("tab-${tab.id}")
            if (loadedTabId == tab.id) loadedTabId = null

            val remaining = _state.value.tabs.filterNot { it.id == tab.id }
            when {
                remaining.isEmpty() -> newTab()
                _state.value.activeTab?.id == tab.id -> selectTab(remaining.first())
            }
        }
    }

    fun toggleTabStack() {
        _state.value = _state.value.copy(tabStackVisible = !_state.value.tabStackVisible)
    }

    /** Called from the Activity. See [SessionPool.onTrimMemory]. */
    fun trimMemory(level: Int) = pool.onTrimMemory(level)

    // MARK: palette

    fun openPalette() {
        _state.value = _state.value.copy(paletteVisible = true)
    }

    fun closePalette() {
        _state.value = _state.value.copy(paletteVisible = false)
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q, isQuestion = engine.isQuestion(q))
    }

    /**
     * Go pressed — navigate or ask, depending on the query.
     *
     * This single branch is the entire "a bar that answers" idea.
     */
    fun submit() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        if (engine.isQuestion(q)) ask(q) else navigate(q)
    }

    fun ask(query: String) {
        if (query.isBlank()) return
        closePalette()
        answerJob?.cancel()

        _state.value = _state.value.copy(
            answer = AnswerResult(query, emptyList()),
            answerStreaming = true,
            answerError = null
        )

        answerJob = viewModelScope.launch {
            try {
                engine.answer(query, settings.providerConfig).collect { result ->
                    _state.value = _state.value.copy(
                        answer = result,
                        answerStreaming = result.markdown.isBlank()
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    answer = (_state.value.answer ?: AnswerResult(query, emptyList())).copy(
                        markdown = AnswerEngine.UNREADABLE
                    ),
                    answerStreaming = false,
                    answerError = e.message
                )
            }
        }
    }

    fun dismissAnswer() {
        answerJob?.cancel()
        _state.value = _state.value.copy(answer = null, answerStreaming = false)
    }

    /**
     * Navigate, promoting a bare search term to a search URL.
     *
     * Writes the tab to the database *and* pushes the URL into the live session
     * — the two have to move together or the user sees a URL in the bar that
     * the WebView below it is not showing.
     */
    fun navigate(raw: String) {
        val q = raw.trim()
        if (q.isEmpty()) return

        val url = when {
            q.startsWith("http://") || q.startsWith("https://") -> q
            q.matches(Regex("^[\\w.-]+\\.[a-z]{2,}(/.*)?$", RegexOption.IGNORE_CASE)) -> "https://$q"
            else -> "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode(q, "UTF-8")
        }

        closePalette()

        val tab = _state.value.activeTab ?: newBlankTabReturning(url)

        viewModelScope.launch {
            val updated = db.tabDao().let {
                it.update(
                    TabEntity(id = tab.id, spaceId = tab.spaceId, url = url, position = tab.position)
                )
                tab.copy(url = url)
            }
            selectTab(updated)
            pool.peek(SESSION_KEY)?.let { view ->
                if (view.url != url) view.loadUrl(url)
            }
        }
    }

    /** Create an empty tab synchronously so navigate() has something to write to. */
    private fun newBlankTabReturning(url: String): Tab {
        val spaceId = _state.value.activeSpace?.id ?: 1L
        val tab = Tab(id = 0L, spaceId = spaceId, url = url)
        _state.value = _state.value.copy(activeTab = tab)
        return tab
    }

    // MARK: summarizing

    fun summarizeCurrentPage(tone: Prompts.Tone = _state.value.summaryTone) {
        val tab = _state.value.activeTab ?: return
        if (!tab.hasContent) return

        summaryJob?.cancel()
        _state.value = _state.value.copy(
            summaryVisible = true,
            summaryText = "",
            summaryOffline = false,
            summaryError = null,
            summaryTone = tone
        )

        summaryJob = viewModelScope.launch {
            // Prefer the live session: it already has the page loaded and warm,
            // so this is nearly instant and costs no extra engine.
            val existing = pool.peek(SESSION_KEY)
            val content = if (existing != null && existing.url == tab.url) {
                injector.extract(existing, tab.url)
            } else {
                val scratch = WebView(getApplication())
                try {
                    injector.extract(scratch, tab.url)
                } finally {
                    runCatching { scratch.destroy() }
                }
            }

            when (val outcome = summarizer.summarize(content, settings.providerConfig, tone)) {
                is SummaryOutcome.Unreadable -> _state.value = _state.value.copy(
                    summaryError = "This page couldn't be read — it may be paywalled or JavaScript-only."
                )

                is SummaryOutcome.Offline -> _state.value = _state.value.copy(
                    summaryText = outcome.text,
                    summaryOffline = true
                )

                is SummaryOutcome.Streaming -> outcome.deltas.collect { delta ->
                    _state.value = _state.value.copy(
                        summaryText = _state.value.summaryText + delta
                    )
                }
            }
        }
    }

    fun setSummaryTone(tone: Prompts.Tone) = summarizeCurrentPage(tone)

    fun dismissSummary() {
        summaryJob?.cancel()
        _state.value = _state.value.copy(summaryVisible = false)
    }

    // MARK: WebView plumbing

    /**
     * Bind the WebView host. Called once from the composable.
     *
     * The session lives under a stable key so re-composition reuses the same
     * view instead of allocating a fresh ~200 MB engine.
     */
    fun attachHost(host: ViewGroup) {
        val view = pool.acquire(SESSION_KEY, host)
        view.webViewClient = XenoWebViewClient(
            listener = sessionListener(),
            blocker = { request ->
                if (!settings.blockingEnabled) false else runCatching {
                    filters.shouldBlock(
                        url = request.url.toString(),
                        documentUrl = view.url ?: "",
                        type = ResourceType.fromRequest(request),
                        thirdParty = true
                    )
                }.getOrDefault(false)
            },
            assetLoader = assetLoader,
            cosmeticSelectors = { filters.cosmeticFor(view.url ?: "") }
        )

        val activeUrl = _state.value.activeTab?.url
        if (!activeUrl.isNullOrBlank() && view.url != activeUrl) {
            view.loadUrl(activeUrl)
            loadedTabId = _state.value.activeTab?.id
        }
    }

    fun detachHost() = pool.park(SESSION_KEY)

    /**
     * Point the live session at [tab] if it is not already showing it.
     *
     * Without this, selecting a tab from the stack updates the address bar and
     * leaves the WebView showing the previous page — the single most confusing
     * state a browser can be in.
     */
    private fun syncSessionToTab(tab: Tab) {
        val view = pool.peek(SESSION_KEY) ?: return
        val target = if (tab.url.isBlank()) null else tab.url

        if (target == null) {
            if (loadedTabId != tab.id) {
                view.loadUrl("about:blank")
                loadedTabId = tab.id
            }
            return
        }

        if (view.url != target || loadedTabId != tab.id) {
            view.loadUrl(target)
            loadedTabId = tab.id
        }
    }

    private fun sessionListener() = object : SessionListener {
        override fun onPageStarted(url: String) {
            _state.value = _state.value.copy(
                load = _state.value.load.copy(loading = true, progress = 0.05f, url = url)
            )
        }

        override fun onPageProgress(url: String, progress: Int) {
            _state.value = _state.value.copy(
                load = _state.value.load.copy(progress = progress / 100f)
            )
        }

        override fun onPageFinished(url: String, title: String) {
            if (url.isBlank() || url == "about:blank") return

            viewModelScope.launch {
                val existing = db.historyDao().byUrl(url)
                if (existing == null) {
                    db.historyDao().insert(HistoryEntity(url = url, title = title))
                } else {
                    db.historyDao().bump(url, title, System.currentTimeMillis())
                }
            }

            // Keep the tab's stored title current, so the tab stack is not full
            // of URLs where titles should be.
            val tabId = loadedTabId ?: _state.value.activeTab?.id
            if (tabId != null && title.isNotBlank()) {
                viewModelScope.launch {
                    db.tabDao().byId(tabId)?.let { entity ->
                        if (entity.title != title) {
                            db.tabDao().update(entity.copy(title = title))
                        }
                    }
                }
            }

            _state.value = _state.value.copy(
                load = PageLoadState(progress = 1f, loading = false, url = url, title = title)
            )
        }

        override fun onFavicon(url: String, icon: android.graphics.Bitmap?) = Unit

        override fun onExternalNavigation(requestedUrl: String): Boolean {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(requestedUrl)
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { getApplication<Application>().startActivity(intent) }
            return true
        }
    }

    fun goBack() {
        pool.peek(SESSION_KEY)?.takeIf { it.canGoBack() }?.goBack()
    }

    /** True when the active session can go back — drives the bar's back button. */
    fun canGoBack(): Boolean = pool.peek(SESSION_KEY)?.canGoBack() == true

    /**
     * Pull the real uBO lists from the network and merge them in.
     *
     * Best-effort: on failure we keep the bundled host list, which is why the
     * first-run experience never depends on the network at all.
     */
    fun refreshFilterLists() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                com.dude555afk.xeno.core.blocking.FilterListSources.remote.forEach { spec ->
                    runCatching {
                        val text = java.net.URL(spec.url).readText()
                        filters.load(text)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        pool.destroyAll()
        super.onCleared()
    }

    companion object {
        /** The active tab's session key. */
        const val SESSION_KEY = "tab-active"
    }
}
