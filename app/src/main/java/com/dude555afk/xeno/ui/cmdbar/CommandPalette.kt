package com.dude555afk.xeno.ui.cmdbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.SearchHit
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.ui.theme.XenoMotion

/**
 * Arc Search-inspired search sheet: dark floating surface, a focused search
 * field, contextual Browse for Me action, and compact recent-page suggestions.
 * Keeps Xeno's lime accent and existing navigation/answer callbacks.
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
        enter = fadeIn(XenoMotion.backdropFade()) +
            scaleIn(initialScale = 0.96f, animationSpec = XenoMotion.overlaySpring()),
        exit = fadeOut(androidx.compose.animation.core.tween(170)) +
            scaleOut(targetScale = 0.98f, animationSpec = androidx.compose.animation.core.tween(170))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    keyboard?.hide()
                    onDismiss()
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .imePadding()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF202022), Color(0xFF111113), Color(0xFF09090A))
                        )
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { }
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
            ) {
                // Small grabber gives the sheet a native, tactile feel.
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 18.dp)
                        .size(width = 34.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f))
                )

                Surface(
                    shape = RoundedCornerShape(23.dp),
                    color = Color(0xFF303033),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.075f)),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.64f),
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.width(11.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    keyboard?.hide()
                                    if (isQuestion) onAsk(query.trim()) else onNavigate(query.trim())
                                },
                                onSearch = {
                                    keyboard?.hide()
                                    if (isQuestion) onAsk(query.trim()) else onNavigate(query.trim())
                                }
                            ),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isEmpty()) {
                                        Text(
                                            "Search or ask Xeno",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White.copy(alpha = 0.45f)
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = Color.White.copy(alpha = 0.56f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onQueryChange("") }
                                    .padding(7.dp)
                                    .size(17.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (query.isNotBlank()) {
                    Surface(
                        onClick = {
                            keyboard?.hide()
                            onAsk(query.trim())
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFB8FF3D),
                        contentColor = Color(0xFF10110A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(9.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Browse for me",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Search the web and build an answer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xCC10110A)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                } else {
                    Text(
                        "SEARCH OR GO TO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.40f),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 5.dp, bottom = 10.dp)
                    )
                }

                if (tabHints.isNotEmpty() || historyHints.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 290.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        if (tabHints.isNotEmpty()) {
                            item(key = "open-tabs-heading") {
                                SectionLabel("OPEN TABS")
                            }
                            items(tabHints, key = { "tab-${it.id}" }) { tab ->
                                SuggestionRow(
                                    icon = Icons.Rounded.Article,
                                    title = tab.displayTitle,
                                    subtitle = tab.url,
                                    accent = accent,
                                    onClick = { keyboard?.hide(); onOpenTab(tab) }
                                )
                            }
                        }
                        if (historyHints.isNotEmpty()) {
                            item(key = "history-heading") {
                                SectionLabel("RECENT")
                            }
                            items(historyHints, key = { "hist-${it.url}" }) { hit ->
                                SuggestionRow(
                                    icon = Icons.Rounded.History,
                                    title = hit.title.ifBlank { hit.url },
                                    subtitle = hit.url,
                                    accent = accent,
                                    onClick = { keyboard?.hide(); onNavigate(hit.url) }
                                )
                            }
                        }
                    }
                } else if (query.isBlank()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.035f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.055f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = accent)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Your next stop", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                                Text("Type a website, search, or question", color = Color.White.copy(alpha = 0.48f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (loading) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(CircleShape),
                        color = accent,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "XENO  ·  PRIVATE BY DESIGN",
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.26f)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.38f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 5.dp)
    )
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.055f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.9f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.43f),
                    maxLines = 1
                )
            }
        }
    }
}
