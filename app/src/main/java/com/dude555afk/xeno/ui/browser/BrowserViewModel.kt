package com.dude555afk.xeno.ui.browser

import android.app.Application
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.dude555afk.xeno.core.model.SearchHit
import com.dude555afk.xeno.core.model.Space
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.core.model.defaultSpace
import com.dude555afk.xeno.core.model.toEntity
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Browser UI state — every overlay surface lives here. */
data class BrowserUiState(
    val spaces: List<Space> = emptyList(),
    val activeSpace: Space? = null,
    val tabs: List<Tab> = emptyList(),
    val activeTab: Tab? = null,
    val load: PageLoadState = PageLoadState(),
    val canGoBack: Boolean = false,
    val paletteVisible: Boolean = false,
    val query: String = "",
    val isQuestion: Boolean = false,
    val historyHints: List<SearchHit> = emptyList(),
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
    private var hintsJob: Job? = null

    /** A lightweight WebView is retained per recently used tab (pool cap: two). */
    private var activeSessionKey: String? = null
    private var webHost: ViewGroup? = null

    init {
        // The bundled list is already loading in XenoApplication; refresh the
        // real uBO lists in the background afterwards. Best-effort — on failure
        // the bundled list still covers the common ad infrastructure.
        refreshFilterLists()

        viewModelScope.launch {
            if (db.spaceDao().count() == 0) {
                db.spaceDao().insert(defaultSpace().toEntity())
            }
            db.tabDao().deletePeekTabs()
        }

        viewModelScope.launch {
            db.spaceDao().observeAll().collect { entities ->
                val spaces = entities.map {
                    Space(
                        id = it.id,
                        name = it.name,
                        accent = Color(it.accentArgb),
                        position = it.position,
                        pinned = it.pinned
                    )
                }
                _state.update { it.copy(spaces = spaces) }
                if (_state.value.activeSpace == null && spaces.isNotEmpty()) {
                    val restored = spaces.firstOrNull { it.id == settings.activeSpaceId }
                        ?: spaces.first()
                    selectSpace(restored)
                }
            }
        }

        // Switch the observed tab stream when the active Space changes. The
        // previous nested collect never returned, so selecting another Space
        // kept observing the old one forever.
        viewModelScope.launch {
            _state
                .map { it.activeSpace?.id }
                .filterNotNull()
                .distinctUntilChanged()
                .flatMapLatest { spaceId ->
                    db.tabDao().observeForSpace(spaceId).map { entities -> spaceId to entities }
                }
                .collect { (spaceId, entities) ->
                    val tabs = entities.map {
                        Tab(
                            id = it.id,
                            spaceId = it.spaceId,
                            url = it.url,
                            title = it.title,
                            isPeek = it.isPeek,
                            position = it.position,
                            createdAt = it.createdAt,
                            lastActiveAt = it.lastActiveAt
                        )
                    }
                    _state.update { current ->
                        if (current.activeSpace?.id != spaceId) {
                            current
                        } else {
                            val active = current.activeTab
                                ?.takeIf { old -> old.spaceId == spaceId && tabs.any { it.id == old.id } }
                                ?: settings.activeTabId(spaceId)?.let { id ->
                                    tabs.firstOrNull { it.id == id }
                                }
                                ?: tabs.firstOrNull()
                            current.copy(tabs = tabs, activeTab = active)
                        }
                    }
                    _state.value.activeTab
                        ?.takeIf { it.spaceId == spaceId }
                        ?.let(::syncSessionToTab)
                    refreshBackState()
                }
        }
    }

    // MARK: spaces & tabs

    fun selectSpace(space: Space) {
        settings.activeSpaceId = space.id
        _state.update {
            it.copy(
                activeSpace = space,
                tabs = emptyList(),
                activeTab = null,
                load = PageLoadState(),
                canGoBack = false
            )
        }
    }

    fun selectTab(tab: Tab) {
        settings.setActiveTabId(tab.spaceId, tab.id)
        _state.update {
            it.copy(
                activeTab = tab,
                load = PageLoadState(url = tab.url),
                canGoBack = false
            )
        }
        syncSessionToTab(tab)
    }

    fun newTab() {
        viewModelScope.launch {
            val spaceId = _state.value.activeSpace?.id ?: 1L
            val position = (_state.value.tabs.maxOfOrNull { it.position } ?: -1) + 1
            val id = db.tabDao().insert(
                TabEntity(
                    spaceId = spaceId,
                    url = "",
                    position = position,
                    lastActiveAt = System.currentTimeMillis()
                )
            )
            selectTab(Tab(id = id, spaceId = spaceId, position = position))
            _state.update { it.copy(tabStackVisible = false, paletteVisible = true) }
        }
    }

    fun closeTab(tab: Tab) {
        viewModelScope.launch {
            db.tabDao().deleteById(tab.id)
            pool.destroy(sessionKey(tab))
            if (activeSessionKey == sessionKey(tab)) activeSessionKey = null

            val remaining = _state.value.tabs.filterNot { it.id == tab.id }
            when {
                remaining.isEmpty() -> newTab()
                _state.value.activeTab?.id == tab.id -> selectTab(remaining.first())
            }
        }
    }

    fun toggleTabStack() {
        _state.update { it.copy(tabStackVisible = !it.tabStackVisible) }
    }

    /** Called from the Activity. See [SessionPool.onTrimMemory]. */
    fun trimMemory(level: Int) = pool.onTrimMemory(level)

    // MARK: palette

    fun openPalette() {
        _state.update { it.copy(paletteVisible = true) }
    }

    fun closePalette() {
        _state.update { it.copy(paletteVisible = false) }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q, isQuestion = engine.isQuestion(q)) }
        refreshHistoryHints(q)
    }

    /**
     * History suggestions for the palette.
     *
     * Debounced because the query changes on every keystroke and a DB round
     * trip per keypress is the kind of thing that makes a text field feel
     * broken. The job is cancelled on each change rather than sleeping a fixed
     * interval, so the last keystroke always lands.
     */
    private fun refreshHistoryHints(q: String) {
        hintsJob?.cancel()
        hintsJob = viewModelScope.launch {
            delay(140)
            if (q.isBlank()) {
                _state.update { it.copy(historyHints = emptyList()) }
                return@launch
            }
            val hits = withContext(Dispatchers.IO) {
                try {
                    db.historyDao().search(q, limit = 8)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    emptyList()
                }
            }
            _state.update { it.copy(historyHints = hits.map { h -> SearchHit(h.url, h.title) }) }
        }
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

        _state.update {
            it.copy(
                answer = AnswerResult(query, emptyList()),
                answerStreaming = true,
                answerError = null
            )
        }

        answerJob = viewModelScope.launch {
            try {
                engine.answer(query, settings.providerConfig).collect { result ->
                    _state.update {
                        it.copy(
                            answer = result,
                            answerStreaming = result.markdown.isBlank()
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        answer = (it.answer ?: AnswerResult(query, emptyList())).copy(
                            markdown = AnswerEngine.UNREADABLE
                        ),
                        answerStreaming = false,
                        answerError = e.message
                    )
                }
            }
            _state.update { it.copy(answerStreaming = false) }
        }
    }

    fun dismissAnswer() {
        answerJob?.cancel()
        _state.update { it.copy(answer = null, answerStreaming = false) }
    }

    /**
     * Navigate, promoting a bare search term to a search URL.
     *
     * Writes the tab to the database *and* pushes the URL into the live session
     * — the two have to move together or the user sees a URL in the bar that
     * the WebView below it is not showing.
     *
     * When there is no active tab (first run, or every tab closed) this creates
     * the row first and uses the id the database returns. The previous version
     * fabricated a Tab with `id = 0L` and called `update()`, which matched
     * nothing, so a cold-start navigate updated no row and loaded no page.
     */
    fun navigate(raw: String) {
        val q = raw.trim()
        if (q.isEmpty()) return

        val url = when {
            q.equals("about:blank", ignoreCase = true) -> "about:blank"
            q.startsWith("http://", ignoreCase = true) || q.startsWith("https://", ignoreCase = true) -> q
            q.matches(
                Regex("^(?:[\\w-]+\\.)+[a-z]{2,}(?::[0-9]{1,5})?(?:[/?#].*)?$", RegexOption.IGNORE_CASE)
            ) || q.matches(Regex("^localhost(?::[0-9]{1,5})?(?:[/?#].*)?$", RegexOption.IGNORE_CASE)) ||
                q.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]{1,5})?(?:[/?#].*)?$")) ->
                "https://$q"
            else -> "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode(q, "UTF-8")
        }

        closePalette()

        viewModelScope.launch {
            val current = _state.value.activeTab
            val spaceId = current?.spaceId ?: _state.value.activeSpace?.id ?: 1L
            val now = System.currentTimeMillis()

            val id = if (current != null && current.id > 0L) {
                val stored = db.tabDao().byId(current.id)
                if (stored != null) {
                    db.tabDao().update(stored.copy(url = url, title = "", lastActiveAt = now))
                    current.id
                } else {
                    val position = (_state.value.tabs.maxOfOrNull { it.position } ?: -1) + 1
                    db.tabDao().insert(
                        TabEntity(
                            spaceId = spaceId,
                            url = url,
                            position = position,
                            lastActiveAt = now
                        )
                    )
                }
            } else {
                val position = (_state.value.tabs.maxOfOrNull { it.position } ?: -1) + 1
                db.tabDao().insert(
                    TabEntity(
                        spaceId = spaceId,
                        url = url,
                        position = position,
                        lastActiveAt = now
                    )
                )
            }

            val storedTab = db.tabDao().byId(id)
            selectTab(
                Tab(
                    id = id,
                    spaceId = spaceId,
                    url = url,
                    title = "",
                    position = storedTab?.position ?: current?.position ?: 0,
                    createdAt = storedTab?.createdAt ?: now,
                    lastActiveAt = now
                )
            )
        }
    }

    // MARK: summarizing

    fun summarizeCurrentPage(tone: Prompts.Tone = _state.value.summaryTone) {
        val tab = _state.value.activeTab ?: return
        if (!tab.hasContent) return

        summaryJob?.cancel()
        _state.update {
            it.copy(
                summaryVisible = true,
                summaryText = "",
                summaryOffline = false,
                summaryError = null,
                summaryTone = tone
            )
        }

        summaryJob = viewModelScope.launch {
            // Prefer the live session: it already has the page loaded and warm,
            // so this is nearly instant and costs no extra engine. A page that
            // navigated on since is re-extracted from a scratch WebView rather
            // than silently summarising the wrong thing.
            val existing = activeSessionKey?.let(pool::peek)
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

            // [Summarizer.summarize] chunks long pages and runs TextRank, both
            // of which are real work. On the main dispatcher that is a jank
            // spike visible before a single token arrives.
            val outcome = withContext(Dispatchers.Default) {
                summarizer.summarize(content, settings.providerConfig, tone)
            }

            when (outcome) {
                is SummaryOutcome.Unreadable -> _state.update {
                    it.copy(
                        summaryError = "This page couldn't be read — it may be paywalled or JavaScript-only."
                    )
                }

                is SummaryOutcome.Offline -> _state.update {
                    it.copy(
                        summaryText = outcome.text,
                        summaryOffline = true
                    )
                }

                is SummaryOutcome.Streaming -> outcome.deltas.collect { delta ->
                    _state.update {
                        it.copy(
                            summaryText = it.summaryText + delta.text,
                            // Sticks once the local fallback has been used.
                            summaryOffline = it.summaryOffline || delta.offlineFallback
                        )
                    }
                }
            }
        }
    }

    fun setSummaryTone(tone: Prompts.Tone) = summarizeCurrentPage(tone)

    fun dismissSummary() {
        summaryJob?.cancel()
        _state.update { it.copy(summaryVisible = false) }
    }

    /**
     * Put the streamed summary on the clipboard.
     *
     * No-op on an empty summary rather than copying a zero-length string, which
     * Android records as "something was copied" and then paste yields nothing.
     */
    fun copySummary() {
        val text = _state.value.summaryText
        if (text.isBlank()) return

        val manager = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

        manager.setPrimaryClip(
            ClipData.newPlainText("Xeno summary", text)
        )
    }

    // MARK: WebView plumbing

    /**
     * Bind the WebView host. Called once from the composable.
     *
     * The session lives under a stable key so re-composition reuses the same
     * view instead of allocating a fresh ~200 MB engine.
     */
    fun attachHost(host: ViewGroup) {
        webHost = host
        _state.value.activeTab?.let(::syncSessionToTab)
        refreshBackState()
    }

    fun detachHost() {
        pool.detachAll()
        webHost = null
    }

    private fun sessionKey(tab: Tab): String =
        if (tab.id > 0L) "tab-${tab.id}" else SESSION_KEY

    /**
     * Activate the selected tab's retained WebView. The pool keeps at most two
     * sessions; older tabs are cheaply restored from their persisted URL when
     * selected again instead of allocating a WebView for every tab.
     */
    private fun syncSessionToTab(tab: Tab) {
        val key = sessionKey(tab)
        val host = webHost ?: run {
            activeSessionKey = key
            return
        }

        val view = pool.acquire(key, host)
        activeSessionKey = key
        view.webViewClient = XenoWebViewClient(
            listener = sessionListener(tab.id, key),
            blocker = { request ->
                if (!settings.blockingEnabled) false else runCatching {
                    filters.shouldBlock(
                        url = request.url.toString(),
                        documentUrl = view.url ?: "",
                        type = ResourceType.fromRequest(request),
                        thirdParty = request.requestHeaders.keys.none { it.equals("Referer", true) } ||
                            runCatching {
                                val requestHost = request.url.host
                                val pageHost = Uri.parse(view.url.orEmpty()).host
                                requestHost != null && pageHost != null &&
                                    !requestHost.equals(pageHost, ignoreCase = true) &&
                                    !requestHost.endsWith(".$pageHost", ignoreCase = true)
                            }.getOrDefault(false)
                    )
                }.getOrDefault(false)
            },
            assetLoader = assetLoader,
            cosmeticSelectors = { filters.cosmeticFor(view.url ?: "") }
        )
        view.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            enqueueDownload(url, userAgent, contentDisposition, mimeType)
        }

        if (tab.url.isBlank()) {
            if (!view.url.isNullOrBlank() && view.url != "about:blank") view.loadUrl("about:blank")
        } else if (view.url != tab.url) {
            view.loadUrl(tab.url)
        }

        refreshBackState()
    }

    private fun enqueueDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        val uri = runCatching { Uri.parse(url) }.getOrNull()
        if (uri == null || (uri.scheme != "https" && uri.scheme != "http") || uri.host.isNullOrBlank()) {
            Toast.makeText(getApplication(), "This download URL isn't supported", Toast.LENGTH_SHORT).show()
            return
        }

        val manager = getApplication<Application>()
            .getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (manager == null) {
            Toast.makeText(getApplication(), "Download manager unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val guessed = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val fileName = java.io.File(guessed)
            .name
            .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .take(180)
            .ifBlank { "download" }

        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription(uri.host ?: "Download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
        if (!mimeType.isNullOrBlank()) request.setMimeType(mimeType)
        // WebView's DownloadListener does not transfer the current session's
        // cookies to DownloadManager automatically. Use only cookies for the
        // exact HTTPS/HTTP download host, with no logging or persistence.
        CookieManager.getInstance().getCookie(url)
            ?.takeIf { it.isNotBlank() }
            ?.let { request.addRequestHeader("Cookie", it) }

        runCatching { manager.enqueue(request) }
            .onSuccess {
                Toast.makeText(getApplication(), "Download started", Toast.LENGTH_SHORT).show()
            }
            .onFailure {
                Toast.makeText(getApplication(), "Couldn't start download", Toast.LENGTH_SHORT).show()
            }
    }

    /** Mirror the active session's history depth into state. */
    private fun refreshBackState() {
        val back = activeSessionKey?.let(pool::peek)?.canGoBack() == true
        if (_state.value.canGoBack != back) {
            _state.update { it.copy(canGoBack = back) }
        }
    }

    private fun sessionListener(tabId: Long, key: String) = object : SessionListener {
        override fun onPageStarted(url: String) {
            if (activeSessionKey != key || _state.value.activeTab?.id != tabId) return
            _state.update {
                it.copy(load = PageLoadState(loading = true, progress = 0.05f, url = url))
            }
            refreshBackState()
        }

        override fun onPageProgress(url: String, progress: Int) {
            if (activeSessionKey != key || _state.value.activeTab?.id != tabId) return
            _state.update { it.copy(load = it.load.copy(progress = progress / 100f, url = url)) }
        }

        override fun onPageFinished(url: String, title: String) {
            if (url.isBlank() || url == "about:blank") return
            // Redirects can deliver a late finish callback for an earlier URL.
            // Ignore it if this tab's WebView has already moved on.
            if (pool.peek(key)?.url != url) return

            viewModelScope.launch {
                val existing = db.historyDao().byUrl(url)
                if (existing == null) {
                    db.historyDao().insert(HistoryEntity(url = url, title = title))
                } else {
                    db.historyDao().bump(url, title, System.currentTimeMillis())
                }

                db.tabDao().byId(tabId)?.let { entity ->
                    val active = _state.value.activeTab?.id == tabId
                    if (entity.url != url || entity.title != title) {
                        db.tabDao().update(
                            entity.copy(
                                url = url,
                                title = title,
                                lastActiveAt = if (active) System.currentTimeMillis() else entity.lastActiveAt
                            )
                        )
                    }
                }
            }

            if (activeSessionKey != key || _state.value.activeTab?.id != tabId) return
            _state.update {
                it.copy(
                    load = PageLoadState(progress = 1f, loading = false, url = url, title = title)
                )
            }
            refreshBackState()
        }

        override fun onFavicon(url: String, icon: android.graphics.Bitmap?) = Unit

        override fun onExternalNavigation(requestedUrl: String): Boolean {
            val uri = Uri.parse(requestedUrl)
            if (uri.scheme != "http" && uri.scheme != "https" &&
                uri.scheme != "mailto" && uri.scheme != "tel" &&
                uri.scheme != "geo" && uri.scheme != "intent"
            ) return true

            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                uri
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { getApplication<Application>().startActivity(intent) }
            return true
        }
    }

    fun goBack() {
        activeSessionKey?.let(pool::peek)?.takeIf { it.canGoBack() }?.goBack()
    }

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
