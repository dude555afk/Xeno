package com.dude555afk.xeno.ui.browser

import android.app.Application
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dude555afk.xeno.core.ai.AiClient
import com.dude555afk.xeno.core.blocking.FilterListEngine
import com.dude555afk.xeno.core.engine.SessionListener
import com.dude555afk.xeno.core.engine.SessionPool
import com.dude555afk.xeno.core.engine.XenoAssetLoader
import com.dude555afk.xeno.core.engine.XenoWebViewClient
import com.dude555afk.xeno.core.fetch.AnswerEngine
import com.dude555afk.xeno.core.fetch.PageFetcher
import com.dude555afk.xeno.core.fetch.SearchEngine
import com.dude555afk.xeno.core.model.*
import com.dude555afk.xeno.core.reader.ReadabilityInjector
import com.dude555afk.xeno.core.storage.SettingsStore
import com.dude555afk.xeno.core.storage.SpaceEntity
import com.dude555afk.xeno.core.storage.TabEntity
import com.dude555afk.xeno.core.storage.HistoryEntity
import com.dude555afk.xeno.core.storage.XenoDatabase
import com.dude555afk.xeno.core.summarize.Prompts
import com.dude555afk.xeno.core.summarize.Summarizer
import com.dude555afk.xeno.core.summarize.SummaryOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val filters = FilterListEngine()
    private val assetLoader = XenoAssetLoader.create(app)

    private val pool = SessionPool(app)
    private val injector = ReadabilityInjector()
    private val ai = AiClient()
    private val summarizer = Summarizer(ai)
    private val fetcher = PageFetcher(app, injector)
    private val engine = AnswerEngine(SearchEngine(app), fetcher, ai)

    private val _state = MutableStateFlow(
        BrowserUiState(blockingEnabled = settings.blockingEnabled, hasApiKey = settings.hasApiKey)
    )
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    private var answerJob: Job? = null
    private var summaryJob: Job? = null

    init {
        viewModelScope.launch {
            if (db.spaceDao().count() == 0) {
                val seed = defaultSpace()
                db.spaceDao().insert(
                    SpaceEntity(name = seed.name, accentArgb = seed.accent.value.toLong().toInt())
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

            val remaining = _state.value.tabs.filterNot { it.id == tab.id }
            when {
                remaining.isEmpty() -> newTab()
                _state.value.activeTab?.id == tab.id -> selectTab(remaining.first())
            }
        }
    }

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
            _state.value = _state.value.copy(answerStreaming = false)
        }
    }

    fun dismissAnswer() {
        answerJob?.cancel()
        _state.value = _state.value.copy(answer = null, answerStreaming = false)
    }

    /** Navigate, promoting a bare search term to a search URL. */
    fun navigate(raw: String) {
        val q = raw.trim()
        if (q.isEmpty()) return

        val url = when {
            q.startsWith("http://") || q.startsWith("https://") ->
 q
            q.matches(Regex("^[\\w.-]+\\.[a-z]{2,}(/.*)?$", RegexOption.IGNORE_CASE)) -> "https://$q"
            else -> "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode(q, "UTF-8")
        }

        viewModelScope.launch {
            val tab = _state.value.activeTab ?: return@launch
            db.tabDao().update(
                TabEntity(id = tab.id, spaceId = tab.spaceId, url = url, position = tab.position)
            )
            selectTab(tab.copy(url = url))
        }
        closePalette()
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
            val existing = pool.peek("tab-${tab.id}")
            val content = if (existing != null) {
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
     * view instead of allocating a fresh 200 MB engine.
     */
    fun attachHost(host: ViewGroup) {
        val view = pool.acquire(SESSION_KEY, host)

        view.webViewClient = XenoWebViewClient(
            listener = sessionListener(),
            blocker = { request ->
                if (!settings.blockingEnabled) return@XenoWebViewClient false
                runCatching {
                    filters.shouldBlock(
                        url = request.url.toString(),
                        documentUrl = view.url ?: "",
                        type = com.dude555afk.xeno.core.blocking.ResourceType.fromRequest(request),
                        thirdParty = true
                    )
                }.getOrDefault(false)
            },
            assetLoader = assetLoader
        )

        val activeUrl = _state.value.activeTab?.url
        if (!activeUrl.isNullOrBlank() && view.url != activeUrl) view.loadUrl(activeUrl)
    }

    fun detachHost() = pool.park(SESSION_KEY)

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
            viewModelScope.launch {
                val existing = db.historyDao().byUrl(url)
                if (existing == null) {
                    db.historyDao().insert(HistoryEntity(url = url, title = title))
                } else {
                    db.historyDao().bump(url, title, System.currentTimeMillis())
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

    fun loadFilterLists(context: android.content.Context, enabledIds: Set<String>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.dude555afk.xeno.core.blocking.FilterListSources.defaults
                .filter { it.id in enabledIds }
                .forEach { spec ->
                    runCatching {
                        context.assets.open(spec.assetName).bufferedReader().use { reader ->
                            filters.load(reader.readText())
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
