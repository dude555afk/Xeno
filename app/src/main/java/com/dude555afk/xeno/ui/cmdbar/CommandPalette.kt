package com.dude555afk.xeno.ui.cmdbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.SearchHit
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.ui.arc.LocalArcMotionEnabled
import kotlinx.coroutines.delay

/**
 * Arc Search's compact search surface.
 *
 * The input and suggested searches live in one light/dark panel at the bottom
 * of the gradient canvas. There is deliberately no title header or stack of
 * floating suggestion cards: Arc's search surface is visually quiet.
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
    onDismiss: () -> Unit
) {
    val motionEnabled = LocalArcMotionEnabled.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val ideas = remember {
        listOf(
            "Summarize today's news",
            "museum shows near me",
            "quick chicken recipes",
            "fun things to do in NYC"
        )
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val panel = if (dark) Color(0xFF24222B) else Color(0xFFF8F7FA)
    val ink = if (dark) Color(0xFFF5F3F8) else Color(0xFF24212A)
    val muted = if (dark) Color(0xFFAAA5B3) else Color(0xFF85808D)
    val arcBlue = Color(0xFF5046D8)

    BackHandler(enabled = visible) {
        keyboard?.hide()
        onDismiss()
    }

    LaunchedEffect(visible) {
        if (visible) {
            delay(100)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (motionEnabled) fadeIn() + scaleIn(initialScale = 0.99f)
        else EnterTransition.None,
        exit = if (motionEnabled) fadeOut() + scaleOut(targetScale = 0.99f)
        else ExitTransition.None
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF2914A2),
                                Color(0xFF5A3CC7),
                                Color(0xFFB978B9),
                                Color(0xFFF09AA7)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
            ) {
                Spacer(Modifier.weight(1f))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(23.dp),
                    color = panel,
                    border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.82f)),
                    shadowElevation = 18.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .padding(start = 17.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = muted,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(11.dp))
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                                cursorBrush = SolidColor(arcBlue),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (query.isNotBlank()) {
                                            keyboard?.hide()
                                            onNavigate(query.trim())
                                        }
                                    },
                                    onGo = {
                                        if (query.isNotBlank()) {
                                            keyboard?.hide()
                                            onNavigate(query.trim())
                                        }
                                    }
                                ),
                                decorationBox = { inner ->
                                    Box {
                                        if (query.isBlank()) {
                                            Text(
                                                "Search...",
                                                color = muted,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                            if (query.isNotBlank()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Clear search",
                                        tint = muted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Surface(
                                    onClick = {
                                        keyboard?.hide()
                                        onAsk(query.trim())
                                    },
                                    shape = CircleShape,
                                    color = arcBlue.copy(alpha = if (dark) 0.24f else 0.10f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (dark) Color(0xFFB9B3FF) else arcBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            "Browse for me",
                                            color = if (dark) Color(0xFFB9B3FF) else arcBlue,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        if (query.isBlank()) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 210.dp),
                                contentPadding = PaddingValues(bottom = 7.dp)
                            ) {
                                items(ideas, key = { it }) { idea ->
                                    BrowseSuggestionRow(
                                        text = idea,
                                        ink = ink,
                                        muted = muted,
                                        arcBlue = arcBlue,
                                        onNavigate = { onNavigate(idea) },
                                        onBrowse = { onAsk(idea) }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 220.dp),
                                contentPadding = PaddingValues(bottom = 7.dp)
                            ) {
                                items(tabHints, key = { it.id }) { tab ->
                                    SearchHintRow(
                                        icon = Icons.Rounded.Article,
                                        title = tab.displayTitle,
                                        subtitle = tab.url,
                                        ink = ink,
                                        muted = muted,
                                        onClick = {
                                            keyboard?.hide()
                                            onOpenTab(tab)
                                        }
                                    )
                                }
                                items(historyHints, key = { it.url }) { hit ->
                                    SearchHintRow(
                                        icon = Icons.Rounded.History,
                                        title = hit.title.ifBlank { hit.url },
                                        subtitle = hit.url,
                                        ink = ink,
                                        muted = muted,
                                        onClick = {
                                            keyboard?.hide()
                                            onNavigate(hit.url)
                                        }
                                    )
                                }
                                if (tabHints.isEmpty() && historyHints.isEmpty()) {
                                    item {
                                        BrowseSuggestionRow(
                                            text = "Search the web",
                                            ink = ink,
                                            muted = muted,
                                            arcBlue = arcBlue,
                                            onNavigate = { onNavigate(query.trim()) },
                                            onBrowse = { onAsk(query.trim()) }
                                        )
                                    }
                                }
                            }
                        }

                        if (loading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = arcBlue,
                                trackColor = panel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseSuggestionRow(
    text: String,
    ink: Color,
    muted: Color,
    arcBlue: Color,
    onNavigate: () -> Unit,
    onBrowse: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clickable(onClick = onNavigate),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = muted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(11.dp))
            Text(
                text,
                color = ink,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            onClick = onBrowse,
            shape = CircleShape,
            color = arcBlue.copy(alpha = 0.09f)
        ) {
            Text(
                "Browse for me",
                color = arcBlue,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SearchHintRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    ink: Color,
    muted: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = muted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                color = ink,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
