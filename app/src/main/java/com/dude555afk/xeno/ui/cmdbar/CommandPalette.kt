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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
 * Arc Search Android-style search surface: pale acrylic panel, dark type,
 * violet Browse for Me action, compact suggestions and an ordinary-search path.
 * The callbacks remain Xeno's own browser/search implementation.
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
    onVoiceSearch: () -> Unit,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .imePadding()
            ) {
                AcrylicGlow(modifier = Modifier.align(Alignment.BottomCenter))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .background(Color(0xF2F3F2F7))
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { }
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
                    color = Color(0xFFFFFFFF),
                    border = BorderStroke(1.dp, Color(0xFF524C60).copy(alpha = 0.10f)),
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
                            tint = Color(0xFF5F5A68).copy(alpha = 0.85f),
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.width(11.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0xFF25222D),
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color(0xFF6155D9)),
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
                                            "Search or ask anything",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color(0xFF6E6879)
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        if (query.isBlank()) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Voice search",
                                tint = Color(0xFF625C6D),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(onClick = onVoiceSearch)
                                    .padding(7.dp)
                                    .size(19.dp)
                            )
                        }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                keyboard?.hide()
                                onAsk(query.trim())
                            },
                            shape = RoundedCornerShape(19.dp),
                            color = Color(0xFF6155D9),
                            contentColor = Color.White,
                            modifier = Modifier.weight(1.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Browse for me",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "AI answer + sources",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.82f)
                                    )
                                }
                            }
                        }
                        Surface(
                            onClick = {
                                keyboard?.hide()
                                onNavigate(query.trim())
                            },
                            shape = RoundedCornerShape(19.dp),
                            color = Color(0xFF303033),
                            contentColor = Color.White,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(0.82f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Search web", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                        color = Color(0xFFE7E4ED),
                        border = BorderStroke(1.dp, Color(0xFF514B5E).copy(alpha = 0.08f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = accent)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Your next stop", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                                Text("A website, a question, or a rabbit hole", color = Color(0xFF6A6574), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (loading) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(CircleShape),
                        color = Color(0xFF6155D9),
                        trackColor = Color(0xFF6155D9).copy(alpha = 0.14f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "XENO  ·  PRIVATE BY DESIGN",
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF777181)
                )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF777181),
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
            color = Color(0xFFEBE8F0),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF6155D9),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF282530),
                maxLines = 1
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF747080),
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
private fun AcrylicGlow(modifier: Modifier = Modifier) {
    // This composable is inside AnimatedVisibility, so its infinite animation
    // is disposed when the palette closes instead of running in the background.
    val glowMotion = rememberInfiniteTransition(label = "xeno-acrylic-glow")
    val glowOffset by glowMotion.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xeno-glow-offset"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 22.dp)
            .padding(bottom = 4.dp)
            .fillMaxWidth(0.82f)
            .height(104.dp)
            .offset(x = glowOffset.dp)
            .blur(18.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x334C55DA),
                        Color(0x444B6DCE),
                        Color(0x554E55D8),
                        Color(0x334C55DA)
                    )
                ),
                shape = RoundedCornerShape(52.dp)
            )
    )
}
