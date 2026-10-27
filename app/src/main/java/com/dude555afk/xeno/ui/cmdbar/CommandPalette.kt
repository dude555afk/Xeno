package com.dude555afk.xeno.ui.cmdbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.dude555afk.xeno.core.model.SearchHit
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.ui.theme.XenoMotion

/**
 * Full-screen Arc Search-style query surface. Ordinary search is the default
 * action; Browse for Me is an explicit action attached to a query suggestion.
 */
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
    onDismiss: () -> Unit,
    recentHistory: List<SearchHit> = emptyList(),
    onVoiceSearch: () -> Unit = {}
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            withFrameNanos { }
            runCatching { focusRequester.requestFocus() }
            withFrameNanos { }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(XenoMotion.backdropFade()) +
            scaleIn(initialScale = 0.985f, animationSpec = XenoMotion.overlaySpring()),
        exit = fadeOut(androidx.compose.animation.core.tween(140)) +
            scaleOut(targetScale = 0.985f, animationSpec = androidx.compose.animation.core.tween(140))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF251126),
                            Color(0xFF30154D),
                            Color(0xFF542A82),
                            Color(0xFFB64D8F)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = {
                        keyboard?.hide()
                        onDismiss()
                    },
                    modifier = Modifier
                        .padding(start = 1.dp, top = 1.dp)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Close search",
                        tint = Color.White.copy(alpha = 0.92f)
                    )
                }

                Spacer(Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(27.dp),
                    color = Color(0xFF25232D),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 7.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = Color(0xFFB9B3C5),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(10.dp))
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFFF7F4FC),
                                    fontSize = 16.sp
                                ),
                                cursorBrush = SolidColor(Color(0xFF8A79FF)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboard?.hide()
                                        if (query.isNotBlank()) onNavigate(query.trim())
                                    },
                                    onGo = {
                                        keyboard?.hide()
                                        if (query.isNotBlank()) onNavigate(query.trim())
                                    }
                                ),
                                decorationBox = { inner ->
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "Search or enter a URL",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                            color = Color(0xFFB2ACBF)
                                        )
                                    }
                                    inner()
                                }
                            )
                            if (query.isNotBlank()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.size(35.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search",
                                        tint = Color(0xFFB9B3C5),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = onVoiceSearch,
                                    modifier = Modifier.size(35.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = "Voice search",
                                        tint = Color(0xFFB9B3C5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (query.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        keyboard?.hide()
                                        onNavigate(query.trim())
                                    }
                                    .padding(start = 16.dp, end = 9.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = Color(0xFFB9B3C5),
                                    modifier = Modifier.size(19.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 11.dp, end = 5.dp)
                                ) {
                                    Text(
                                        text = query.trim(),
                                        color = Color(0xFFF4F0FA),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Search the web",
                                        color = Color(0xFFAAA3B8),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp
                                    )
                                }
                                BrowseForMeButton(
                                    enabled = !loading,
                                    onClick = {
                                        keyboard?.hide()
                                        onAsk(query.trim())
                                    }
                                )
                            }
                        }

                        val matchingTabs = tabHints.filter {
                            query.isBlank() ||
                                it.displayTitle.contains(query, ignoreCase = true) ||
                                it.url.contains(query, ignoreCase = true)
                        }.take(4)
                        if (matchingTabs.isNotEmpty()) {
                            PaletteSectionLabel(if (query.isBlank()) "Continue browsing" else "Open tabs")
                            matchingTabs.forEach { tab ->
                                PaletteSuggestionRow(
                                    icon = Icons.Rounded.Article,
                                    title = tab.displayTitle.ifBlank { tab.url },
                                    subtitle = tab.url,
                                    onClick = {
                                        keyboard?.hide()
                                        onOpenTab(tab)
                                    }
                                )
                            }
                        }

                        val availableHistory = if (query.isBlank()) recentHistory else historyHints
                        if (availableHistory.isNotEmpty()) {
                            PaletteSectionLabel(if (query.isBlank()) "Recent pages" else "Suggestions")
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 220.dp),
                                contentPadding = PaddingValues(bottom = 7.dp)
                            ) {
                                items(availableHistory.take(6), key = { "history-${it.url}" }) { hit ->
                                    PaletteSuggestionRow(
                                        icon = Icons.Rounded.History,
                                        title = hit.title.ifBlank { hit.url },
                                        subtitle = hit.url,
                                        browseQuery = if (query.isBlank()) null else hit.title.ifBlank { hit.url },
                                        onBrowseForMe = {
                                            keyboard?.hide()
                                            onAsk(hit.title.ifBlank { hit.url })
                                        },
                                        onClick = {
                                            keyboard?.hide()
                                            onNavigate(hit.url)
                                        }
                                    )
                                }
                            }
                        }

                        if (query.isBlank() && matchingTabs.isEmpty() && availableHistory.isEmpty()) {
                            PaletteSectionLabel("Try Browse for Me")
                            listOf(
                                "Summarize today's news",
                                "Explain a complex topic",
                                "Compare the best options"
                            ).forEach { suggestion ->
                                PaletteSuggestionRow(
                                    icon = Icons.Rounded.Search,
                                    title = suggestion,
                                    subtitle = "",
                                    browseQuery = suggestion,
                                    onBrowseForMe = {
                                        keyboard?.hide()
                                        onAsk(suggestion)
                                    },
                                    onClick = {
                                        keyboard?.hide()
                                        onNavigate(suggestion)
                                    }
                                )
                            }
                        }

                        if (loading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF8A79FF),
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
            }
        }
    }
}

@Composable
private fun PaletteSectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 17.dp, top = 9.dp, bottom = 4.dp),
        color = Color(0xFFA7A0B4),
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun PaletteSuggestionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    browseQuery: String? = null,
    onBrowseForMe: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFAAA3B8),
            modifier = Modifier.size(18.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFF7F4FC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA7A0B4),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (browseQuery != null && onBrowseForMe != null) {
            BrowseForMeButton(enabled = true, onClick = onBrowseForMe)
        }
    }
}

@Composable
private fun BrowseForMeButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF5D4BE8),
        contentColor = Color.White
    ) {
        Text(
            text = "Browse for me",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
