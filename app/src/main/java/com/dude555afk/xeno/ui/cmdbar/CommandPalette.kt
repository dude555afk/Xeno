package com.dude555afk.xeno.ui.cmdbar

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let(onQueryChange)
        }
    }

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
            slideInVertically(
                initialOffsetY = { it / 5 },
                animationSpec = XenoMotion.sheetSpring()
            ) +
            scaleIn(initialScale = 0.985f, animationSpec = XenoMotion.sheetSpring()),
        exit = fadeOut(androidx.compose.animation.core.tween(150)) +
            slideOutVertically(
                targetOffsetY = { it / 7 },
                animationSpec = androidx.compose.animation.core.tween(170)
            ) +
            scaleOut(targetScale = 0.99f, animationSpec = androidx.compose.animation.core.tween(170))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.10f))
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xF20F0F12))
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
                                            "Search...",
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
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = "Voice search",
                            tint = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    keyboard?.hide()
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your search")
                                    }
                                    runCatching { voiceSearchLauncher.launch(intent) }
                                        .onFailure {
                                            Toast.makeText(
                                                context,
                                                "Voice search isn't available on this device",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                .padding(7.dp)
                                .size(19.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (query.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                keyboard?.hide()
                                onNavigate(query.trim())
                            }
                            .padding(start = 8.dp, end = 5.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.56f),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = query.trim(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            onClick = {
                                keyboard?.hide()
                                onAsk(query.trim())
                            },
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF5757E8),
                            contentColor = Color.White
                        ) {
                            Text(
                                "Browse for me",
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    Text(
                        "SEARCH OR GO TO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.40f),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 5.dp, bottom = 10.dp)
                    )
                }

                if (query.isBlank() || tabHints.isNotEmpty() || historyHints.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 290.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        if (query.isBlank()) {
                            item(key = "suggested-heading") {
                                SectionLabel("SUGGESTED")
                            }
                            items(
                                listOf(
                                    "Summarize today's news",
                                    "museum shows near me",
                                    "quick chicken recipes",
                                    "fun things to do in NYC"
                                ),
                                key = { "prompt-$it" }
                            ) { prompt ->
                                QuickPromptRow(
                                    title = prompt,
                                    onBrowse = {
                                        keyboard?.hide()
                                        onAsk(prompt)
                                    },
                                    onSearch = {
                                        keyboard?.hide()
                                        onNavigate(prompt)
                                    }
                                )
                            }
                        }
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
}

@Composable
private fun QuickPromptRow(
    title: String,
    onBrowse: () -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSearch)
            .padding(start = 7.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.62f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.91f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            onClick = onBrowse,
            shape = RoundedCornerShape(50),
            color = Color(0xFF5757E8),
            contentColor = Color.White
        ) {
            Text(
                "Browse for me",
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
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
                        Color(0x335D4BFF),
                        Color(0x4450DDB8),
                        Color(0x55B8FF3D),
                        Color(0x335D4BFF)
                    )
                ),
                shape = RoundedCornerShape(52.dp)
            )
    )
}
