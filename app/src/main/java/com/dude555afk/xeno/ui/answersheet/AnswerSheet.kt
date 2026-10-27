package com.dude555afk.xeno.ui.answersheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.AnswerResult
import com.dude555afk.xeno.core.model.AnswerSource
import com.dude555afk.xeno.ui.render.AnswerText
import com.dude555afk.xeno.ui.theme.Dimens
import com.dude555afk.xeno.ui.theme.XenoMotion

/**
 * The answer sheet — flagship surface.
 *
 * A readable, scrollable answer page with a distinct source section. The
 * streaming skeleton fills the empty state while the answer is generated.
 */
@Composable
fun AnswerSheet(
    result: AnswerResult?,
    streaming: Boolean,
    accent: Color,
    onOpenSource: (AnswerSource) -> Unit,
    onDismiss: () -> Unit
) {
    val parsed = remember(result?.markdown) { AnswerText.parse(result?.markdown.orEmpty()) }
    val listState = rememberLazyListState()

    /**
     * Hoisted once so the LazyRow below never has to dereference a nullable
     * inside a lambda. The previous version did `items(result.sources)` guarded
     * only by `if (!result?.sources.isNullOrEmpty())` — Kotlin's smart-cast
     * engine does not reason that far through a null-safe call chain, so
     * `result` stayed nullable there and the build failed on a type mismatch.
     * A non-null local is the unambiguous form and reads better anyway.
     */
    val sources = result?.sources.orEmpty()
    val markdown = result?.markdown.orEmpty()

    // Keep the newest text in view as it streams. Keyed on length rather than
    // content so a token arriving mid-animation does not cancel the scroll.
    LaunchedEffect(markdown.length) {
        val count = sources.size + parsed.blocks.size + 1
        if (count > 0) runCatching { listState.animateScrollToItem(count - 1) }
    }

    AnimatedVisibility(
        visible = result != null,
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
                    .fillMaxHeight(0.85f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* keep sheet open */ }
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Handle
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

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = Dimens.spaceL,
                            end = Dimens.spaceL,
                            bottom = Dimens.spaceXl
                        )
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(Dimens.spaceS))
                                Text(
                                    text = result?.query.orEmpty(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(Dimens.spaceL))
                        }

                        // Answer blocks.
                        itemsIndexed(parsed.blocks) { _, block ->
                            when (block) {
                                is AnswerText.Parsed.Block.Heading -> Text(
                                    text = block.text,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = Dimens.spaceS)
                                )

                                is AnswerText.Parsed.Block.Paragraph -> Text(
                                    text = AnswerText.annotated(
                                        block.text, accent
                                    ) { index ->
                                        result?.sourceFor(index)?.let(onOpenSource)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = Dimens.spaceM)
                                )

                                is AnswerText.Parsed.Block.Bullet -> Row(
                                    modifier = Modifier.padding(bottom = Dimens.spaceS)
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = accent,
                                        modifier = Modifier.padding(end = Dimens.spaceS)
                                    )
                                    Text(
                                        text = AnswerText.annotated(
                                            block.text, accent
                                        ) { index ->
                                            result?.sourceFor(index)?.let(onOpenSource)
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (streaming && markdown.isEmpty()) {
                            item { StreamingSkeleton() }
                        }

                        // Arc's generated answer is a readable page first;
                        // sources remain available as a distinct section below it.
                        if (sources.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(Dimens.spaceL))
                                Text(
                                    text = "Sources",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(Dimens.spaceS))
                            }
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    items(sources) { source ->
                                        SourceCard(source, accent, onOpenSource)
                                    }
                                }
                                Spacer(Modifier.height(Dimens.spaceL))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A source card — favicon dot, title, domain. Tap opens the real page. */
@Composable
private fun SourceCard(
    source: AnswerSource,
    accent: Color,
    onOpen: (AnswerSource) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .width(240.dp)
            .clickable { onOpen(source) }
    ) {
        Column(Modifier.padding(Dimens.spaceM)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(accent.copy(alpha = 0.25f), RoundedCornerShape(percent = 50))
                )
                Spacer(Modifier.width(Dimens.spaceS))
                Text(
                    text = "[${source.index}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                Spacer(Modifier.width(Dimens.spaceS))
                Text(
                    text = source.host,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(Dimens.spaceS))
            Text(
                text = source.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (source.snippet.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = source.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(Dimens.spaceS))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Open",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Shimmer lines while the answer generates.
 *
 * Deliberately skeleton rather than a spinner — it reads as "text is on the
 * way" instead of "the app is thinking".
 */
@Composable
private fun StreamingSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmerAlpha"
    )

    Column {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (i == 2) 0.6f else 0.95f)
                    .height(14.dp)
                    .background(
                        Color.White.copy(alpha = alpha),
                        RoundedCornerShape(7.dp)
                    )
            )
            Spacer(Modifier.height(Dimens.spaceS))
        }
    }
}
