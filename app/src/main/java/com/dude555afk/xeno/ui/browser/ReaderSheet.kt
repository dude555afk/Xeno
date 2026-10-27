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
import androidx.compose.ui.unit.sp
import com.dude555afk.xeno.core.model.ExtractedContent
import com.dude555afk.xeno.ui.theme.StrokeSubtle
import com.dude555afk.xeno.ui.theme.TextPrimary
import com.dude555afk.xeno.ui.theme.TextSecondary

@Composable
fun ReaderSheet(
    visible: Boolean,
    loading: Boolean,
    content: ExtractedContent?,
    error: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFFFCFAFE)).clickable(
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
                border = StrokeSubtle.let { androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)) }
            ) {
                Column(
                    Modifier.fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("READER MODE", color = Color(0xFF68675F), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.6.sp)
                            Text("Easy on the eyes", color = Color(0xFF23231F), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close reader mode", tint = Color(0xFF34342E))
                        }
                    }
                    if (loading) {
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            color = Color(0xFF557A29)
                        )
                    }
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        when {
                            loading -> {
                                Text("Preparing article…", color = Color(0xFF68675F), style = MaterialTheme.typography.bodyLarge)
                            }
                            error != null -> {
                                Text(error, color = Color(0xFF8C342D), style = MaterialTheme.typography.bodyLarge)
                            }
                            content != null -> {
                                Text(content.title.ifBlank { "Untitled article" }, color = Color(0xFF23231F), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
                                if (!content.byline.isNullOrBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(content.byline.orEmpty(), color = Color(0xFF68675F), style = MaterialTheme.typography.bodySmall)
                                }
                                if (content.excerpt.isNotBlank()) {
                                    Spacer(Modifier.height(18.dp))
                                    Text(content.excerpt, color = Color(0xFF4A4A43), style = MaterialTheme.typography.titleSmall, lineHeight = 25.sp, fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(22.dp))
                                SelectionContainer {
                                    Text(
                                        text = content.text,
                                        color = Color(0xFF292923),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 30.sp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = content?.url.orEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        color = Color(0xFF77776D),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
