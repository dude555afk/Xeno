package com.dude555afk.xeno.ui.cmdbar

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dude555afk.xeno.core.model.SearchHit
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.ui.arc.LocalArcMotionEnabled
import com.dude555afk.xeno.ui.arc.PixelArcAmbientBackground
import kotlinx.coroutines.delay

private val Sheet = Color(0xFF2D2E32)
private val SearchPill = Color(0xFF4F5052)
private val PrimaryText = Color.White
private val SecondaryText = Color(0xFFA4A5A8)
private val BrowseBlue = Color(0xFF4E5BCD)

/** Arc Search's rising search drawer, with a real keyboard-aware input and recent history. */
@Composable
fun CommandPalette(
    visible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    tabHints: List<Tab>,
    historyHints: List<SearchHit>,
    loading: Boolean,
    isQuestion: Boolean,
    accent: Color,
    onNavigate: (String) -> Unit,
    onAsk: (String) -> Unit,
    onOpenTab: (Tab) -> Unit,
    onDismiss: () -> Unit
) {
    val motion = LocalArcMotionEnabled.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val requester = remember { FocusRequester() }
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) onQueryChange(spoken)
    }

    BackHandler(enabled = visible) {
        keyboard?.hide()
        onDismiss()
    }
    LaunchedEffect(visible) {
        if (visible) {
            delay(110)
            runCatching { requester.requestFocus() }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (motion) fadeIn() + scaleIn(initialScale = 0.985f) else EnterTransition.None,
        exit = if (motion) fadeOut() + scaleOut(targetScale = 0.985f) else ExitTransition.None
    ) {
        Box(Modifier.fillMaxSize()) {
            PixelArcAmbientBackground()
            Column(Modifier.fillMaxSize().imePadding()) {
                Spacer(Modifier.height(255.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = Sheet,
                    shape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Spacer(Modifier.height(19.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 19.dp),
                            shape = CircleShape,
                            color = SearchPill
                        ) {
                            Row(
                                modifier = Modifier.height(58.dp).padding(start = 25.dp, end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = query,
                                    onValueChange = onQueryChange,
                                    modifier = Modifier.weight(1f).focusRequester(requester),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = PrimaryText,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF4E66F8)),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            val term = query.trim()
                                            if (term.isNotEmpty()) {
                                                keyboard?.hide()
                                                if (isQuestion) onAsk(term) else onNavigate(term)
                                            }
                                        },
                                        onGo = {
                                            val term = query.trim()
                                            if (term.isNotEmpty()) {
                                                keyboard?.hide()
                                                onNavigate(term)
                                            }
                                        }
                                    ),
                                    decorationBox = { inner ->
                                        Box {
                                            if (query.isBlank()) {
                                                Text("Search...", color = SecondaryText, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                                            }
                                            inner()
                                        }
                                    }
                                )
                                if (query.isBlank()) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                                            }
                                            try {
                                                voiceLauncher.launch(intent)
                                            } catch (_: ActivityNotFoundException) {
                                                keyboard?.show()
                                            }
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(Icons.Rounded.Mic, contentDescription = "Voice search", tint = SecondaryText, modifier = Modifier.size(21.dp))
                                    }
                                    Icon(
                                        Icons.Rounded.VisibilityOff,
                                        contentDescription = null,
                                        tint = SecondaryText,
                                        modifier = Modifier.padding(start = 3.dp).size(23.dp)
                                    )
                                } else {
                                    Surface(
                                        onClick = { onQueryChange("") },
                                        color = Color(0xFFA5A6A8),
                                        shape = CircleShape,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = SearchPill, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(if (query.isBlank()) 20.dp else 31.dp))
                        if (query.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(48.dp).padding(start = 22.dp, end = 19.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).height(48.dp).clickable {
                                        keyboard?.hide()
                                        onNavigate(query.trim())
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF9A9B9F), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(20.dp))
                                    Text(query, color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Surface(
                                    onClick = {
                                        keyboard?.hide()
                                        onAsk(query.trim())
                                    },
                                    color = BrowseBlue,
                                    shape = CircleShape,
                                    modifier = Modifier.width(110.dp).height(41.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("Browse for me", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (tabHints.isNotEmpty() || historyHints.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
                                ) {
                                    items(tabHints, key = { "tab-${it.id}" }) { tab ->
                                        HistoryRow(
                                            title = tab.displayTitle,
                                            subtitle = tab.url,
                                            isAi = false,
                                            onClick = { keyboard?.hide(); onOpenTab(tab) }
                                        )
                                    }
                                    items(historyHints, key = { "history-${it.url}" }) { hit ->
                                        HistoryRow(
                                            title = hit.title.ifBlank { hit.url },
                                            subtitle = hit.url,
                                            isAi = false,
                                            onClick = { keyboard?.hide(); onNavigate(hit.url) }
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 5.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                items(historyHints, key = { it.url }) { hit ->
                                    HistoryRow(
                                        title = hit.title.ifBlank { hit.url },
                                        subtitle = hit.url,
                                        isAi = false,
                                        onClick = { keyboard?.hide(); onNavigate(hit.url) }
                                    )
                                }
                                if (historyHints.isEmpty()) {
                                    item {
                                        Text(
                                            "Your recent pages will appear here",
                                            color = SecondaryText,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (loading) {
                            androidx.compose.material3.LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = BrowseBlue,
                                trackColor = Sheet
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    title: String,
    subtitle: String,
    isAi: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp).clickable(onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (isAi) Color(0xFF873EA7) else Color(0xFF44454A),
            modifier = Modifier.size(25.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isAi) Icons.Rounded.AutoAwesome else Icons.Rounded.History,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(Modifier.width(17.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, color = PrimaryText, fontSize = 15.7.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = SecondaryText.copy(alpha = 0.78f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
