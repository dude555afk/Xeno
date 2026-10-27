package com.dude555afk.xeno.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Translate
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.ui.theme.StrokeSubtle
import com.dude555afk.xeno.ui.theme.TextPrimary
import com.dude555afk.xeno.ui.theme.TextSecondary

@Composable
fun TranslationSheet(
    visible: Boolean,
    loading: Boolean,
    language: String,
    title: String,
    text: String,
    error: String?,
    notice: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize()
                .background(Color(0xFFFCFAFE))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { },
                shape = RoundedCornerShape(0.dp),
                color = Color(0xFFFCFAFE),
                border = androidx.compose.foundation.BorderStroke(1.dp, StrokeSubtle),
                shadowElevation = 0.dp
            ) {
                Column(
                    Modifier.fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, top = 14.dp, end = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                "Translation · $language",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                title,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close translation", tint = TextSecondary)
                        }
                    }
                    if (loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(
                        Modifier.weight(1f).fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 20.dp)
                    ) {
                        if (!notice.isNullOrBlank()) {
                            Text(notice, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                        }
                        if (!error.isNullOrBlank()) {
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                        } else if (text.isBlank() && loading) {
                            Text(
                                "Reading the article and translating it…",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else if (text.isBlank()) {
                            Text(
                                "The translation will appear here.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            SelectionContainer {
                                Text(
                                    text = text,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    Text(
                        "Review AI output before relying on it.",
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
                        color = TextSecondary.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
