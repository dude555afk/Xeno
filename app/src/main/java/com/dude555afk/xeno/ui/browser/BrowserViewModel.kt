package com.dude555afk.xeno.ui.browser

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import android.view.ScaleGestureDetector
import android.webkit.WebView
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
import com.dude555afk.xeno.core.model.ArchivedTab
import com.dude555afk.xeno.core.model.PinnedPage
import com.dude555afk.xeno.core.model.PageLoadState
import com.dude555afk.xeno.core.model.SearchHit
import com.dude555afk.xeno.core.model.Space
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.core.model.defaultSpace
import com.dude555afk.xeno.core.model.toEntity
import com.dude555afk.xeno.core.reader.ReadabilityInjector
import com.dude555afk.xeno.core.storage.HistoryEntity
import com.dude555afk.xeno.core.storage.ArchivedTabEntity
import com.dude555afk.xeno.core.storage.PinnedTabEntity
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val canGoForward: Boolean = false,
    val isCurrentPagePinned: Boolean = false,
    val pageTextZoom: Int = 100,
    val paletteVisible: Boolean = false,
    val query: String = "",
    val isQuestion: Boolean = false,
    val historyHints: List<SearchHit> = emptyList(),
    val tabStackVisible: Boolean = false,
    val tabPreviews: Map<Long, Bitmap> = emptyMap(),
    val answer: AnswerResult? = null,
    val answerStreaming: Boolean = false,
    val answerError: String? = null,
    val summaryVisible: Boolean = false,
    val summaryText: String = "",
    val summaryOffline: Boolean = false,
    val summaryError: String? = null,
    val summaryTone: Prompts.Tone = Prompts.Tone.Terse,
    val blockingEnabled: Boolean = true,
    val autoOpenKeyboard: Boolean = true,
    val themeMode: String = "system",
    val accentOverrideArgb: Int = 0,
    val animationsEnabled: Boolean = true,
    val chromeVisible: Boolean = true,
    val hasApiKey: Boolean = false,
    val incognitoMode: Boolean = false,
    val archivedTabs: List<ArchivedTab> = emptyList(),
    val pinnedTabs: List<PinnedPage> = emptyList(),
    val searchEngine: String = "duckduckgo",
    val languageMode: String = "infer",
    val archiveAfter: String = "1d",
    val alwaysOpenAppLinks: Boolean = false,
    val allowGoogleTranslate: Boolean = true,
    val forceDark: Boolean = true,
    val appIcon: String = "default",
    val desktopSiteEnabled: Boolean = false,
    val blockedAdsCount: Int = 0
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
            autoOpenKeyboard = settings.autoOpenKeyboard,
            themeMode = settings.themeMode,
            accentOverrideArgb = settings.accentOverrideArgb,
            animationsEnabled = settings.animationsEnabled,
            hasApiKey = settings.hasApiKey,
            searchEngine = settings.searchEngine,
            languageMode = settings.languageMode,
            archiveAfter = settings.archiveAfter,
            alwaysOpenAppLinks = settings.alwaysOpenAppLinks,
            allowGoogleTranslate = settings.allowGoogleTranslate,
            forceDark = settings.forceDark,
            appIcon = settings.appIcon
        )
    )
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    private var answerJob: Job? = null
    private var summaryJob: Job? = null
    private var hintsJob: Job? = null
    private var startupSearchHandled = false

    /** The tab the WebView session is currently showing. */
    private var loadedTabId: Long? = null
    private val inMemoryIncognitoTabs = mutableListOf<Tab>()
    private var nextIncognitoTabId = -1L
    private var recentlyArchivedId: Long? = null

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
            archiveInactiveTabs()
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
                    selectSpace(spaces.first())
                }
            }
        }

        viewModelScope.launch {
            db.archiveDao().observeAll().collect { rows ->
                _state.update { current -> current.copy(archivedTabs = rows.map { row ->
                    ArchivedTab(
                        id = row.id,
                        originalTabId = row.originalTabId,
                        spaceId = row.spaceId,
                        url = row.url,
                        title = row.title,
                        faviconUrl = row.faviconUrl,
                        type = row.type,
                        query = row.query,
                        desktopSite = row.desktopSite,
                        zoomLevel = row.zoomLevel,
                        blockedAdsCount = row.blockedAdsCount,
                        archivedAt = row.archivedAt
                    )
                }) }
            }
        }

        viewModelScope.launch {
            db.pinnedTabDao().observeAll().collect { rows ->
                _state.update { current -> current.copy(pinnedTabs = rows.map { row ->
                    PinnedPage(row.url, row.title, row.faviconUrl, row.createdAt)
                }) }
            }
        }

        // Observe only the currently selected space. collectLatest cancels the
        // previous Room Flow when the space id changes; nesting collect inside
        // _state.collect would otherwise pin the ViewModel to the first space
        // forever and keep the old observer alive.
        viewModelScope.launch {
            _state
                .map { it.activeSpace?.id }
                .distinctUntilChanged()
                .collectLatest { spaceId ->
                    if (spaceId == null) {
                        _state.update { it.copy(tabs = emptyList(), activeTab = null) }
                        return@collectLatest
                    }
                    db.tabDao().observeForSpace(spaceId).collect { entities ->
                        val tabs = entities.map {
                            Tab(
                                id = it.id,
                                spaceId = it.spaceId,
                                url = it.url,
                                title = it.title,
                                faviconUrl = it.faviconUrl,
                                isPeek = it.isPeek,
                                position = it.position,
                                createdAt = it.createdAt,
                                lastActiveAt = it.lastActiveAt,
                                type = it.type,
                                query = it.query,
                                desktopSite = it.desktopSite,
                                zoomLevel = it.zoomLevel,
                                blockedAdsCount = it.blockedAdsCount
                            )
                        } + inMemoryIncognitoTabs.filter { it.spaceId == spaceId }
                        _state.update { current ->
                            val active = current.activeTab
                                ?.let { old -> tabs.firstOrNull { it.id == old.id } }
                                ?: tabs.firstOrNull()
                            current.copy(tabs = tabs, activeTab = active)
                        }
                        _state.value.activeTab?.let(::syncSessionToTab)
                        refreshBackState()
                    }
                }
        }
    }

    fun showStartupSearchIfConfigured() {
        if (startupSearchHandled) return
        startupSearchHandled = true
        if (settings.autoOpenKeyboard) openPalette()
    }

    fun setSearchEngine(value: String) {
        val safe = value.takeIf { it in setOf("duckduckgo", "google", "bing") } ?: "duckduckgo"
        settings.searchEngine = safe
        _state.update { it.copy(searchEngine = safe) }
    }

    fun setLanguageMode(value: String) {
        val safe = value.takeIf { it in setOf("infer", "en", "hi", "bn") } ?: "infer"
        settings.languageMode = safe
        _state.update { it.copy(languageMode = safe) }
    }

    fun setArchiveAfter(value: String) {
        val safe = value.takeIf { it in setOf("never", "1d", "7d", "30d") } ?: "1d"
        settings.archiveAfter = safe
        _state.update { it.copy(archiveAfter = safe) }
        archiveInactiveTabs()
    }

    fun setAlwaysOpenAppLinks(value: Boolean) {
        settings.alwaysOpenAppLinks = value
        _state.update { it.copy(alwaysOpenAppLinks = value) }
    }

    fun setAllowGoogleTranslate(value: Boolean) {
        settings.allowGoogleTranslate = value
        _state.update { it.copy(allowGoogleTranslate = value) }
    }

    fun setForceDark(value: Boolean) {
        settings.forceDark = value
        _state.update { it.copy(forceDark = value) }
        pool.peek(SESSION_KEY)?.settings?.let { web ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                web.forceDark = if (value) android.webkit.WebSettings.FORCE_DARK_ON
                else android.webkit.WebSettings.FORCE_DARK_OFF
            }
        }
    }

    fun setAppIcon(value: String) {
        val safe = value.takeIf { it in setOf("default", "dark", "light", "purple") } ?: "default"
        settings.appIcon = safe
        _state.update { it.copy(appIcon = safe) }
    }

    fun toggleIncognitoMode() {
        _state.update { current ->
            val enabled = !current.incognitoMode
            current.copy(incognitoMode = enabled, historyHints = if (enabled) emptyList() else current.historyHints)
        }
        if (!_state.value.incognitoMode) refreshHistoryHints(_state.value.query)
    }

    fun setAutoOpenKeyboard(enabled: Boolean) {
        settings.autoOpenKeyboard = enabled
        _state.update { it.copy(autoOpenKeyboard = enabled) }
    }

    fun setThemeMode(mode: String) {
        val safeMode = mode.takeIf { it in setOf("system", "dark", "light") } ?: "system"
        settings.themeMode = safeMode
        _state.update { it.copy(themeMode = safeMode) }
    }

    fun setAccentOverride(argb: Int) {
        settings.accentOverrideArgb = argb
        _state.update { it.copy(accentOverrideArgb = argb) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        settings.animationsEnabled = enabled
        _state.update { it.copy(animationsEnabled = enabled) }
    }

    private fun setChromeVisible(visible: Boolean) {
        if (_state.value.chromeVisible != visible) {
            _state.update { it.copy(chromeVisible = visible) }
        }
    }

    // MARK: spaces & tabs

    fun selectSpace(space: Space) {
        settings.activeSpaceId = space.id
        _state.update { it.copy(activeSpace = space) }
    }

    fun selectTab(tab: Tab) {
        if (loadedTabId != null && loadedTabId != tab.id) {
            captureTabPreview(loadedTabId!!)
        }
        _state.update {
            val replacedTabs = it.tabs.map { existing -> if (existing.id == tab.id) tab else existing }
            val visibleTabs = if (replacedTabs.any { existing -> existing.id == tab.id }) {
                replacedTabs
            } else {
                replacedTabs + tab
            }
            it.copy(
                tabs = visibleTabs,
                activeTab = tab,
                load = PageLoadState(url = tab.url),
                isCurrentPagePinned = settings.isPinned(tab.url),
                incognitoMode = tab.isIncognito,
                pageTextZoom = tab.zoomLevel,
                desktopSiteEnabled = tab.desktopSite,
                blockedAdsCount = tab.blockedAdsCount
            )
        }
        if (!tab.isIncognito) {
            viewModelScope.launch {
                db.tabDao().byId(tab.id)?.let { entity ->
                    db.tabDao().update(entity.copy(lastActiveAt = System.currentTimeMillis()))
                }
            }
        }
        syncSessionToTab(tab)
        if (tab.type == "ai") {
            if (_state.value.answer?.query != tab.query || _state.value.answer == null) {
                generateAnswer(tab.query)
            }
        } else if (_state.value.answer != null || _state.value.answerStreaming) {
            answerJob?.cancel()
            _state.update { it.copy(answer = null, answerStreaming = false, answerError = null) }
        }
    }

    fun newTab() {
        viewModelScope.launch {
            val spaceId = _state.value.activeSpace?.id ?: 1L
            if (_state.value.incognitoMode) {
                val tab = Tab(id = nextIncognitoTabId--, spaceId = spaceId, isIncognito = true)
                inMemoryIncognitoTabs.add(tab)
                _state.update { it.copy(tabs = it.tabs + tab) }
                selectTab(tab)
            } else {
                val id = db.tabDao().insert(TabEntity(spaceId = spaceId, url = ""))
                selectTab(Tab(id = id, spaceId = spaceId))
            }
            _state.update {
                it.copy(
                    tabStackVisible = false,
                    paletteVisible = true,
                    query = "",
                    isQuestion = false,
                    historyHints = emptyList()
                )
            }
            refreshHistoryHints("")
        }
    }

    fun closeTab(tab: Tab) {
        recentlyClosedTab = tab
        recentlyArchivedId = null
        viewModelScope.launch {
            if (tab.isIncognito) {
                inMemoryIncognitoTabs.removeAll { it.id == tab.id }
            } else {
                db.tabDao().deleteById(tab.id)
            }
            _state.update { current ->
                current.copy(tabPreviews = current.tabPreviews - tab.id)
            }
            if (loadedTabId == tab.id) {
                pool.peek(SESSION_KEY)?.loadUrl("about:blank")
                loadedTabId = null
            }

            val remaining = _state.value.tabs.filterNot { it.id == tab.id }
            _state.update { it.copy(tabs = remaining) }
            when {
                remaining.isEmpty() -> {
                    _state.update {
                        it.copy(
                            activeTab = null,
                            incognitoMode = false,
                            paletteVisible = false,
                            load = PageLoadState(),
                            tabs = emptyList(),
                            canGoBack = false,
                            canGoForward = false,
                            answer = null,
                            answerStreaming = false,
                            answerError = null
                        )
                    }
                }
                _state.value.activeTab?.id == tab.id -> selectTab(remaining.first())
            }
        }
    }

    /** Archive a regular tab; incognito tabs are destroyed instead of stored. */
    fun archiveTab(tab: Tab) {
        if (tab.isIncognito) {
            closeTab(tab)
            return
        }
        recentlyClosedTab = tab
        recentlyArchivedId = null
        viewModelScope.launch {
            val archiveId = db.archiveDao().insert(
                ArchivedTabEntity(
                    originalTabId = tab.id,
                    spaceId = tab.spaceId,
                    url = tab.url,
                    title = tab.title,
                    faviconUrl = tab.faviconUrl,
                    type = tab.type,
                    query = tab.query,
                    desktopSite = tab.desktopSite,
                    zoomLevel = tab.zoomLevel,
                    blockedAdsCount = tab.blockedAdsCount
                )
            )
            recentlyArchivedId = archiveId
            db.tabDao().deleteById(tab.id)
            _state.update { it.copy(tabPreviews = it.tabPreviews - tab.id) }
            if (loadedTabId == tab.id) {
                pool.peek(SESSION_KEY)?.loadUrl("about:blank")
                loadedTabId = null
            }
            val remaining = _state.value.tabs.filterNot { it.id == tab.id }
            _state.update { it.copy(tabs = remaining) }
            if (_state.value.activeTab?.id == tab.id) {
                if (remaining.isNotEmpty()) selectTab(remaining.first())
                else _state.update {
                    it.copy(
                        activeTab = null,
                        incognitoMode = false,
                        paletteVisible = false,
                        load = PageLoadState(),
                        tabs = emptyList(),
                        canGoBack = false,
                        canGoForward = false,
                        answer = null,
                        answerStreaming = false,
                        answerError = null
                    )
                }
            }
        }
    }

    fun restoreArchivedTab(archived: ArchivedTab) {
        viewModelScope.launch {
            val newId = db.tabDao().insert(
                TabEntity(
                    spaceId = archived.spaceId,
                    url = archived.url,
                    title = archived.title,
                    faviconUrl = archived.faviconUrl,
                    type = archived.type,
                    query = archived.query,
                    desktopSite = archived.desktopSite,
                    zoomLevel = archived.zoomLevel,
                    blockedAdsCount = archived.blockedAdsCount
                )
            )
            db.archiveDao().deleteById(archived.id)
            _state.value.spaces.firstOrNull { it.id == archived.spaceId }?.let(::selectSpace)
            val tab = Tab(
                id = newId,
                spaceId = archived.spaceId,
                url = archived.url,
                title = archived.title,
                faviconUrl = archived.faviconUrl,
                type = archived.type,
                query = archived.query,
                desktopSite = archived.desktopSite,
                zoomLevel = archived.zoomLevel,
                blockedAdsCount = archived.blockedAdsCount
            )
            selectTab(tab)
            _state.update { it.copy(tabStackVisible = false, paletteVisible = false) }
        }
    }

    fun deleteArchivedTab(id: Long) {
        viewModelScope.launch { db.archiveDao().deleteById(id) }
    }

    private fun archiveInactiveTabs() {
        val after = settings.archiveAfter
        val age = when (after) {
            "1d" -> 24L * 60 * 60 * 1000
            "7d" -> 7L * 24 * 60 * 60 * 1000
            "30d" -> 30L * 24 * 60 * 60 * 1000
            else -> return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val cutoff = System.currentTimeMillis() - age
            db.tabDao().all().filter {
                it.lastActiveAt < cutoff && (it.url.isNotBlank() || (it.type == "ai" && it.query.isNotBlank()))
            }.forEach { row ->
                db.archiveDao().insert(
                    ArchivedTabEntity(
                        originalTabId = row.id,
                        spaceId = row.spaceId,
                        url = row.url,
                        title = row.title,
                        faviconUrl = row.faviconUrl,
                        type = row.type,
                        query = row.query,
                        desktopSite = row.desktopSite,
                        zoomLevel = row.zoomLevel,
                        blockedAdsCount = row.blockedAdsCount
                    )
                )
                db.tabDao().deleteById(row.id)
            }
        }
    }

    fun toggleTabStack() {
        if (!_state.value.tabStackVisible) {
            loadedTabId?.let(::captureTabPreview)
        }
        _state.update { it.copy(tabStackVisible = !it.tabStackVisible) }
    }

    /** Move one step through the open tabs from a horizontal dock scrub. */
    fun selectAdjacentTab(direction: Int) {
        val snapshot = _state.value
        if (snapshot.tabs.size < 2 || direction == 0) return
        val currentIndex = snapshot.tabs.indexOfFirst { it.id == snapshot.activeTab?.id }
            .coerceAtLeast(0)
        val nextIndex = (currentIndex + direction.compareTo(0)).coerceIn(0, snapshot.tabs.lastIndex)
        if (nextIndex != currentIndex) selectTab(snapshot.tabs[nextIndex])
    }

    private var recentlyClosedTab: Tab? = null

    /** Restore the most recently closed tab from the tab-stack Undo affordance. */
    fun undoCloseLastTab() {
        val tab = recentlyClosedTab ?: return
        recentlyClosedTab = null
        viewModelScope.launch {
            _state.value.spaces.firstOrNull { it.id == tab.spaceId }?.let(::selectSpace)
            if (tab.isIncognito) {
                inMemoryIncognitoTabs.add(tab)
                _state.update { it.copy(tabs = it.tabs + tab) }
                selectTab(tab)
                return@launch
            }
            recentlyArchivedId?.let { id ->
                db.archiveDao().deleteById(id)
                recentlyArchivedId = null
            }
            val restoredId = db.tabDao().insert(
                TabEntity(
                    spaceId = tab.spaceId,
                    url = tab.url,
                    title = tab.title,
                    faviconUrl = tab.faviconUrl,
                    type = tab.type,
                    query = tab.query,
                    desktopSite = tab.desktopSite,
                    zoomLevel = tab.zoomLevel,
                    blockedAdsCount = tab.blockedAdsCount,
                    position = tab.position
                )
            )
            selectTab(tab.copy(id = restoredId))
        }
    }

    /** Called from the Activity. See [SessionPool.onTrimMemory]. */
    fun trimMemory(level: Int) = pool.onTrimMemory(level)

    // MARK: palette

    fun openPalette() {
        _state.update {
            it.copy(
                paletteVisible = true,
                query = "",
                isQuestion = false,
                historyHints = emptyList()
            )
        }
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
            if (q.isBlank() || _state.value.incognitoMode) {
                _state.update { it.copy(historyHints = emptyList()) }
                return@launch
            }
            val hits = withContext(Dispatchers.IO) {
                runCatching { db.historyDao().search(q, limit = 8) }.getOrDefault(emptyList())
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
        val q = query.trim()
        if (q.isEmpty()) return
        closePalette()

        viewModelScope.launch {
            val current = _state.value.activeTab
            val spaceId = current?.spaceId ?: _state.value.activeSpace?.id ?: 1L
            val privateTab = _state.value.incognitoMode || current?.isIncognito == true

            val aiTab = when {
                privateTab && current?.isIncognito == true &&
                    (current.url.isBlank() || current.type == "ai") -> {
                    current.copy(url = "", title = q, type = "ai", query = q)
                        .also { updated ->
                            inMemoryIncognitoTabs.replaceAll { if (it.id == updated.id) updated else it }
                            _state.update { state ->
                                state.copy(tabs = state.tabs.map { if (it.id == updated.id) updated else it })
                            }
                        }
                }
                privateTab -> Tab(
                    id = nextIncognitoTabId--,
                    spaceId = spaceId,
                    title = q,
                    type = "ai",
                    query = q,
                    isIncognito = true
                ).also { created ->
                    inMemoryIncognitoTabs.add(created)
                    _state.update { it.copy(tabs = it.tabs + created) }
                }
                current != null && !current.isIncognito &&
                    (current.url.isBlank() || current.type == "ai") -> {
                    val row = db.tabDao().byId(current.id)
                    if (row != null) {
                        db.tabDao().update(row.copy(url = "", title = q, type = "ai", query = q))
                    }
                    current.copy(url = "", title = q, type = "ai", query = q)
                }
                else -> {
                    val id = db.tabDao().insert(
                        TabEntity(spaceId = spaceId, url = "", title = q, type = "ai", query = q)
                    )
                    Tab(id = id, spaceId = spaceId, title = q, type = "ai", query = q)
                }
            }
            _state.update { it.copy(tabStackVisible = false, paletteVisible = false) }
            selectTab(aiTab)
        }
    }

    private fun generateAnswer(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        answerJob?.cancel()
        _state.update {
            it.copy(
                answer = AnswerResult(q, emptyList()),
                answerStreaming = true,
                answerError = null
            )
        }

        answerJob = viewModelScope.launch {
            try {
                engine.answer(q, settings.providerConfig).collect { result ->
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
                        answer = (it.answer ?: AnswerResult(q, emptyList())).copy(
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

    /**
     * Return the active tab from DuckDuckGo SERP mode to its AI answer mode.
     * The query is retained so switching back to web can rebuild the SERP.
     */
    fun returnToAnswerMode() {
        val tab = _state.value.activeTab ?: return
        if (tab.query.isBlank()) return
        val updated = tab.copy(type = "ai", url = "")
        if (tab.isIncognito) {
            inMemoryIncognitoTabs.replaceAll { if (it.id == tab.id) updated else it }
        } else {
            viewModelScope.launch {
                db.tabDao().byId(tab.id)?.let { db.tabDao().update(it.copy(type = "ai", url = "")) }
            }
        }
        _state.update { current ->
            current.copy(
                activeTab = updated,
                tabs = current.tabs.map { if (it.id == updated.id) updated else it },
                load = PageLoadState(),
                isCurrentPagePinned = false
            )
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
            q.startsWith("http://") || q.startsWith("https://") -> q
            q.matches(Regex("^[\\w.-]+\\.[a-z]{2,}(/.*)?$", RegexOption.IGNORE_CASE)) -> "https://$q"
            else -> searchUrl(q)
        }

        closePalette()

        viewModelScope.launch {
            val current = _state.value.activeTab
            val useIncognito = _state.value.incognitoMode
            val spaceId = current?.spaceId ?: _state.value.activeSpace?.id ?: 1L
            val target = when {
                useIncognito && current?.isIncognito != true ->
                    Tab(id = nextIncognitoTabId--, spaceId = spaceId, isIncognito = true)
                        .also { inMemoryIncognitoTabs.add(it) }
                !useIncognito && current?.isIncognito == true -> null
                else -> current
            }

            if (target?.isIncognito == true) {
                val updated = target.copy(
                    url = url,
                    title = "",
                    type = "web",
                    query = q,
                    blockedAdsCount = 0,
                    lastActiveAt = System.currentTimeMillis()
                )
                inMemoryIncognitoTabs.replaceAll { if (it.id == updated.id) updated else it }
                _state.update { state -> state.copy(tabs = state.tabs.map { if (it.id == updated.id) updated else it }) }
                selectTab(updated)
            } else {
                val id = if (target != null && target.id != 0L) {
                    db.tabDao().byId(target.id)?.let { row ->
                        db.tabDao().update(row.copy(url = url, title = "", query = q, type = "web", blockedAdsCount = 0))
                    }
                    target.id
                } else {
                    db.tabDao().insert(TabEntity(spaceId = spaceId, url = url, query = q))
                }
                selectTab(Tab(id = id, spaceId = spaceId, url = url, query = q))
            }
        }
    }

    private fun searchUrl(query: String): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return when (settings.searchEngine) {
            "google" -> "https://www.google.com/search?q=$encoded"
            "bing" -> "https://www.bing.com/search?q=$encoded"
            else -> "https://duckduckgo.com/?q=$encoded"
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
        val view = pool.acquire(SESSION_KEY, host)
        view.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val delta = scrollY - oldScrollY
            if (scrollY <= 12) {
                setChromeVisible(true)
            } else if (kotlin.math.abs(delta) >= 7) {
                setChromeVisible(delta < 0)
            }
        }
        var pinchTriggered = false
        val pinchDetector = ScaleGestureDetector(
            getApplication(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (!pinchTriggered && detector.scaleFactor < 0.90f &&
                        detector.currentSpan < detector.previousSpan
                    ) {
                        pinchTriggered = true
                        summarizeCurrentPage()
                    }
                    return pinchTriggered
                }
            }
        )
        view.setOnTouchListener { _, event ->
            if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                setChromeVisible(true)
                pinchTriggered = false
            }
            pinchDetector.onTouchEvent(event)
            if (event.actionMasked == android.view.MotionEvent.ACTION_UP ||
                event.actionMasked == android.view.MotionEvent.ACTION_CANCEL
            ) {
                val consumed = pinchTriggered
                pinchTriggered = false
                consumed
            } else {
                pinchTriggered
            }
        }
        view.webViewClient = XenoWebViewClient(
            listener = sessionListener(),
            blocker = { request ->
                val blocked = if (!settings.blockingEnabled) false else runCatching {
                    filters.shouldBlock(
                        url = request.url.toString(),
                        documentUrl = view.url ?: "",
                        type = ResourceType.fromRequest(request),
                        thirdParty = true
                    )
                }.getOrDefault(false)
                if (blocked) recordBlockedRequest()
                blocked
            },
            assetLoader = assetLoader,
            cosmeticSelectors = { filters.cosmeticFor(view.url ?: "") }
        )

        view.settings.apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                forceDark = if (settings.forceDark) android.webkit.WebSettings.FORCE_DARK_ON
                else android.webkit.WebSettings.FORCE_DARK_OFF
            }
        }
        val activeUrl = _state.value.activeTab?.url
        if (!activeUrl.isNullOrBlank() && view.url != activeUrl) {
            view.loadUrl(activeUrl)
            loadedTabId = _state.value.activeTab?.id
        }
        refreshBackState()
    }

    fun detachHost() = pool.park(SESSION_KEY)

    /**
     * Capture a small, in-memory snapshot for the horizontal tab carousel.
     * The full-resolution page stays in Chromium; the preview is capped at
     * 360×720 and eight tabs so this never becomes a second copy of web pages.
     */
    private fun captureTabPreview(tabId: Long) {
        val view = pool.peek(SESSION_KEY) ?: return
        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) return

        runCatching {
            val scale = minOf(360f / width, 720f / height, 1f)
            val bitmap = Bitmap.createBitmap(
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            // Called on the UI thread before tab selection navigates the shared
            // WebView, so this snapshot belongs to the tab being left.
            view.draw(canvas)

            _state.update { current ->
                val previews = LinkedHashMap(current.tabPreviews)
                previews[tabId] = bitmap
                while (previews.size > 8) {
                    previews.remove(previews.keys.first())
                }
                current.copy(tabPreviews = previews)
            }
        }
    }

    /**
     * Point the live session at [tab] if it is not already showing it.
     *
     * Without this, selecting a tab from the stack updates the address bar and
     * leaves the WebView showing the previous page — the single most confusing
     * state a browser can be in.
     */
    private fun syncSessionToTab(tab: Tab) {
        // AI tabs are rendered by the answer surface, not by the WebView. Keep
        // the last web session parked underneath so returning to it is instant.
        if (tab.type == "ai") return
        val view = pool.peek(SESSION_KEY) ?: return
        view.settings.apply {
            useWideViewPort = tab.desktopSite
            loadWithOverviewMode = tab.desktopSite
            textZoom = tab.zoomLevel
            userAgentString = if (tab.desktopSite) {
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
            } else {
                android.webkit.WebSettings.getDefaultUserAgent(getApplication())
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                forceDark = if (settings.forceDark) android.webkit.WebSettings.FORCE_DARK_ON
                else android.webkit.WebSettings.FORCE_DARK_OFF
            }
        }

        when {
            tab.url.isBlank() -> {
                if (loadedTabId != tab.id) {
                    view.loadUrl("about:blank")
                    loadedTabId = tab.id
                }
            }

            view.url != tab.url || loadedTabId != tab.id -> {
                view.loadUrl(tab.url)
                loadedTabId = tab.id
            }
        }
    }

    /** Mirror the session's history depth into state so the bar can show it. */
    private fun refreshBackState() {
        val view = pool.peek(SESSION_KEY)
        val back = view?.canGoBack() == true
        val forward = view?.canGoForward() == true
        if (_state.value.canGoBack != back || _state.value.canGoForward != forward) {
            _state.update { it.copy(canGoBack = back, canGoForward = forward) }
        }
    }

    private fun recordBlockedRequest() {
        val tab = _state.value.activeTab ?: return
        if (loadedTabId != null && loadedTabId != tab.id) return
        val updated = tab.copy(blockedAdsCount = tab.blockedAdsCount + 1)
        _state.update { current ->
            current.copy(
                tabs = current.tabs.map { if (it.id == tab.id) updated else it },
                activeTab = updated,
                blockedAdsCount = updated.blockedAdsCount
            )
        }
        if (!tab.isIncognito) viewModelScope.launch {
            db.tabDao().byId(tab.id)?.let { row ->
                db.tabDao().update(row.copy(blockedAdsCount = updated.blockedAdsCount))
            }
        } else {
            inMemoryIncognitoTabs.replaceAll { if (it.id == tab.id) updated else it }
        }
    }

    private fun sessionListener() = object : SessionListener {
        override fun onPageStarted(url: String) {
            val tabId = loadedTabId ?: _state.value.activeTab?.id
            if (tabId != null) {
                _state.update { it.copy(tabPreviews = it.tabPreviews - tabId) }
            }
            setChromeVisible(true)
            _state.update {
                it.copy(load = it.load.copy(loading = true, progress = 0.05f, url = url))
            }
            refreshBackState()
        }

        override fun onPageProgress(url: String, progress: Int) {
            _state.update { it.copy(load = it.load.copy(progress = progress / 100f)) }
        }

        override fun onPageFinished(url: String, title: String) {
            if (url.isBlank() || url == "about:blank") return
            (loadedTabId ?: _state.value.activeTab?.id)?.let(::captureTabPreview)

            val finishedTabId = loadedTabId ?: _state.value.activeTab?.id
            val currentTab = _state.value.tabs.firstOrNull { it.id == finishedTabId }
            if (currentTab?.isIncognito != true) {
                viewModelScope.launch {
                    val existing = db.historyDao().byUrl(url)
                    if (existing == null) {
                        db.historyDao().insert(HistoryEntity(url = url, title = title))
                    } else {
                        db.historyDao().bump(url, title, System.currentTimeMillis())
                    }
                }
            }

            // Keep the tab's stored title current, so the tab stack is not full
            // of URLs where titles should be.
            val tabId = finishedTabId
            if (tabId != null && title.isNotBlank()) {
                if (currentTab?.isIncognito == true) {
                    val updated = currentTab.copy(title = title)
                    inMemoryIncognitoTabs.replaceAll { if (it.id == tabId) updated else it }
                    _state.update { state ->
                        state.copy(
                            tabs = state.tabs.map { if (it.id == tabId) updated else it },
                            activeTab = if (state.activeTab?.id == tabId) updated else state.activeTab
                        )
                    }
                } else {
                    viewModelScope.launch {
                        db.tabDao().byId(tabId)?.let { entity ->
                            if (entity.title != title) db.tabDao().update(entity.copy(title = title))
                        }
                    }
                }
            }

            setChromeVisible(true)
            _state.update {
                it.copy(
                    load = PageLoadState(progress = 1f, loading = false, url = url, title = title)
                )
            }
            refreshBackState()
        }

        override fun onFavicon(url: String, icon: android.graphics.Bitmap?) = Unit

        override fun onExternalNavigation(requestedUrl: String): Boolean {
            if (!settings.alwaysOpenAppLinks) return false
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

    fun goForward() {
        pool.peek(SESSION_KEY)?.takeIf { it.canGoForward() }?.goForward()
    }

    fun reloadPage() {
        pool.peek(SESSION_KEY)?.reload()
    }

    fun findInPage(query: String) {
        val view = pool.peek(SESSION_KEY) ?: return
        if (query.isBlank()) view.clearMatches() else view.findAllAsync(query)
    }

    fun findNext(forward: Boolean) {
        pool.peek(SESSION_KEY)?.findNext(forward)
    }

    fun clearFindInPage() {
        pool.peek(SESSION_KEY)?.clearMatches()
    }

    fun togglePinCurrentPage() {
        val url = _state.value.load.url.ifBlank { _state.value.activeTab?.url.orEmpty() }
        if (url.isBlank()) return
        val pinned = settings.togglePinned(url)
        val tab = _state.value.activeTab
        viewModelScope.launch {
            if (pinned) db.pinnedTabDao().insert(
                PinnedTabEntity(url = url, title = tab?.title.orEmpty(), faviconUrl = tab?.faviconUrl)
            ) else db.pinnedTabDao().deleteByUrl(url)
        }
        _state.update { it.copy(isCurrentPagePinned = pinned) }
    }

    fun setBlockingEnabled(enabled: Boolean) {
        settings.blockingEnabled = enabled
        _state.update { it.copy(blockingEnabled = enabled) }
    }

    /** Clear visit history while keeping open tabs alive. */
    fun clearBrowsingData() {
        hintsJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { db.historyDao().clear() }
            _state.update { it.copy(historyHints = emptyList()) }
        }
    }

    fun changePageTextZoom(delta: Int) {
        val zoom = (_state.value.pageTextZoom + delta).coerceIn(50, 200)
        pool.peek(SESSION_KEY)?.settings?.textZoom = zoom
        val tab = _state.value.activeTab
        if (tab != null) {
            val updated = tab.copy(zoomLevel = zoom)
            _state.update { current ->
                current.copy(
                    activeTab = updated,
                    tabs = current.tabs.map { if (it.id == updated.id) updated else it },
                    pageTextZoom = zoom
                )
            }
            if (!tab.isIncognito) viewModelScope.launch {
                db.tabDao().byId(tab.id)?.let { db.tabDao().update(it.copy(zoomLevel = zoom)) }
            } else inMemoryIncognitoTabs.replaceAll { if (it.id == tab.id) updated else it }
        } else _state.update { it.copy(pageTextZoom = zoom) }
    }

    fun setDesktopSite(enabled: Boolean) {
        val view = pool.peek(SESSION_KEY)
        view?.settings?.apply {
            useWideViewPort = enabled
            loadWithOverviewMode = enabled
            userAgentString = if (enabled) {
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
            } else {
                android.webkit.WebSettings.getDefaultUserAgent(getApplication())
            }
        }
        view?.reload()
        val tab = _state.value.activeTab ?: return
        val updated = tab.copy(desktopSite = enabled)
        _state.update { current ->
            current.copy(
                activeTab = updated,
                tabs = current.tabs.map { if (it.id == updated.id) updated else it },
                desktopSiteEnabled = enabled
            )
        }
        if (!tab.isIncognito) viewModelScope.launch {
            db.tabDao().byId(tab.id)?.let { db.tabDao().update(it.copy(desktopSite = enabled)) }
        } else inMemoryIncognitoTabs.replaceAll { if (it.id == tab.id) updated else it }
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
