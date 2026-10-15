package com.dude555afk.xeno.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.summarize.Prompts
import com.dude555afk.xeno.ui.render.AnswerText
import com.dude555afk.xeno.ui.theme.Dimens
import com.dude555afk.xeno.ui.theme.PillShape
import com.dude555afk.xeno.ui.theme.XenoMotion

/**
 * The summarizer sheet.
 *
 * Same skeleton/shape language as the answer sheet — they are the same idea
 * (stream text into a bottom sheet) and should feel like one feature, not two.
 */
@Composable
fun SummarySheet(
    visible: Boolean,
    pageTitle: String,
    streamText: String,
    tone: Prompts.Tone,
    offline: Boolean,
    error: String?,
    accent: Color,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    val parsed = remember(streamText) { AnswerText.parse(streamText) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(XenoMotion.backdropFade()),
        exit = fadeOut(tween(160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = Dimens.cardRadius, topEnd = Dimens.cardRadius),
                color = Color(0xFF0A0A0A),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* swallow */ }
            ) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = Dimens.spaceM)
                            .size(width = 36.dp, height = 4.dp)
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(percent = 50)
                            )
                    )

                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Dimens.spaceL)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(Dimens.spaceS))
                            Text(
                                text = "Summary",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.weight(1f))

                            // Only offered once there is something to copy.
                            // A dead copy button mid-stream is a lie.
                            if (streamText.isNotBlank()) {
                                Surface(
                                    shape = PillShape,
                                    color = Color.White.copy(alpha = 0.06f),
                                    onClick = onCopy
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = Dimens.spaceM,
                                            vertical = 6.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy summary",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(Dimens.spaceXs))
                                        Text(
                                            text = "Copy",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (offline) {
                                Spacer(Modifier.width(Dimens.spaceS))
                                Text(
                                    text = "offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(Dimens.spaceS))
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )

                        Spacer(Modifier.height(Dimens.spaceM))

                        // Tone chips — same text, four different prompts.
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement
                                .spacedBy(Dimens.spaceS)
                        ) {
                            Prompts.Tone.entries.forEach { option ->
                                val selected = option == tone
                                Surface(
                                    shape = PillShape,
                                    color = if (selected) accent else Color.White.copy(alpha = 0.06f),
                                    onClick = { onToneChange(option) }
                                ) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            horizontal = Dimens.spaceM,
                                            vertical = 6.dp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(Dimens.spaceL))

                        error?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(Dimens.spaceM))
                        }

                        if (streamText.isBlank()) {
                            SummarySkeleton()
                        } else {
                            SelectionContainer {
                                Column {
                                    parsed.blocks.forEach { block ->
                                        val text: AnnotatedString = when (block) {
                                            is AnswerText.Parsed.Block.Heading ->
                                                AnswerText.annotated(block.text, accent) { }
                                            is AnswerText.Parsed.Block.Paragraph ->
                                                AnswerText.annotated(block.text, accent) { }
                                            is AnswerText.Parsed.Block.Bullet ->
                                                AnswerText.annotated(block.text, accent) { }
                                        }

                                        Row(Modifier.padding(bottom = Dimens.spaceM)) {
                                            if (block is AnswerText.Parsed.Block.Bullet) {
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = accent,
                                                    modifier = Modifier.padding(end = Dimens.spaceS)
                                                )
                                            }
                                            Text(
                                                text = text,
                                                style = if (block is AnswerText.Parsed.Block.Heading) {
                                                    MaterialTheme.typography.titleLarge
                                                } else {
                                                    MaterialTheme.typography.bodyLarge
                                                },
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummarySkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    Column {
        repeat(6) { i ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (i == 5) 0.5f else 0.92f)
                    .height(14.dp)
                    .background(
                        Color.White.copy(alpha = alpha),
                        RoundedCornerShape(7.dp)
                    )
            )
            Spacer(Modifier.height(Dimens.spaceM))
        }
    }
}
