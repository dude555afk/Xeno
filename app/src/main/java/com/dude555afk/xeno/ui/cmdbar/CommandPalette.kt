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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.SearchHit
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.ui.theme.Dimens
import com.dude555afk.xeno.ui.theme.PillShape
import com.dude555afk.xeno.ui.theme.XenoMotion

/**
 * The command palette — the centre of the whole UI.
 *
 * Dual-purpose by design: hit **Go** and it navigates, hit **Ask Xeno** and the
 * same query routes to the answer engine. One keystroke apart, no mode
 * switching, exactly the way Arc's bar feels.
 *
 * Enters with a spring, never a slide — see docs/DESIGN.md.
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

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(XenoMotion.backdropFade()) +
            scaleIn(initialScale = 0.92f, animationSpec = XenoMotion.overlaySpring()),
        exit = fadeOut(androidx.compose.animation.core.tween(200)) +
            scaleOut(
                targetScale = 0.95f,
                animationSpec = androidx.compose.animation.core.tween(200)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceXl)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* swallow so the backdrop isn't dismissed */ }
            ) {
                // The bar itself — a pill, docked at the bottom.
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.spaceL, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(Dimens.spaceM))

                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = { keyboard?.hide(); onNavigate(query.trim()) },
                                onSearch = { keyboard?.hide(); onNavigate(query.trim()) }
                            ),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search or ask anything",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                inner()
                            }
                        )

                        if (query.isNotBlank()) {
                            // The dual mode — this button is the whole trick.
                            // The label reflects what the heuristic thinks, so
                            // the user knows which one the keyboard Go key is
                            // about to do. Both paths stay reachable regardless.
                            TextButton(
                                onClick = { keyboard?.hide(); onAsk(query.trim()) },
                                shape = PillShape,
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = accent,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(
                                    text = if (isQuestion) "Ask" else "Ask anyway",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Dimens.spaceM))

                // Suggestions — tabs and history, fuzzy filtered by the caller.
                if (query.isNotBlank() && (tabHints.isNotEmpty() || historyHints.isNotEmpty())) {
                    Surface(
                        shape = RoundedCornerShape(Dimens.cardRadius),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 320.dp),
                            contentPadding = PaddingValues(vertical = Dimens.spaceS)
                        ) {
                            items(tabHints, key = { "tab-${it.id}" }) { tab ->
                                SuggestionRow(
                                    icon = Icons.Rounded.Tab,
                                    title = tab.displayTitle,
                                    subtitle = tab.url,
                                    onClick = { onOpenTab(tab) }
                                )
                            }
                            items(historyHints, key = { "hist-${it.url}" }) { hit ->
                                SuggestionRow(
                                    icon = Icons.Rounded.History,
                                    title = hit.title.ifBlank { hit.url },
                                    subtitle = hit.url,
                                    onClick = { onNavigate(hit.url) }
                                )
                            }
                        }
                    }
                }

                if (loading) {
                    Spacer(Modifier.height(Dimens.spaceL))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(PillShape),
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(Dimens.spaceM))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
