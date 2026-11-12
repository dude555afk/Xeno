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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.AnswerResult
import com.dude555afk.xeno.core.model.AnswerSource
import com.dude555afk.xeno.ui.render.AnswerText

private val BrowseInk = Color(0xFF282231)
private val BrowseMuted = Color(0xFF827A8A)
private val BrowsePaper = Color(0xFFFBF9FD)
private val BrowseAccent = Color(0xFF5944CE)

/**
 * Arc-style Browse for Me reading surface.
 *
 * The answer engine and inline citation routing are unchanged. This component
 * only changes presentation: a full reading canvas, query pill, source carousel,
 * and clear editorial hierarchy.
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
    val sources = result?.sources.orEmpty()
    val markdown = result?.markdown.orEmpty()

    LaunchedEffect(markdown.length) {
        val itemCount = 2 + sources.size + parsed.blocks.size
        if (itemCount > 0) runCatching { listState.animateScrollToItem(itemCount - 1) }
    }

    AnimatedVisibility(
        visible = result != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(140))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.46f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = BrowsePaper,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "BROWSE FOR ME",
                                color = BrowseAccent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "A web-wide answer",
                                color = BrowseMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = Color(0xFFF0EDF4)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close answer",
                                tint = BrowseInk,
                                modifier = Modifier.padding(10.dp).size(20.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEAE6EF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = BrowseMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                result?.query.orEmpty(),
                                color = BrowseInk,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 22.dp,
                            bottom = 38.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (sources.isNotEmpty()) {
                            item {
                                Text(
                                    "Sources from around the web",
                                    color = BrowseInk,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(end = 4.dp)
                                ) {
                                    items(sources, key = { it.index }) { source ->
                                        SourceCard(source, accent, onOpenSource)
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        itemsIndexed(parsed.blocks) { _, block ->
                            when (block) {
                                is AnswerText.Parsed.Block.Heading -> Text(
                                    text = block.text,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = BrowseAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 10.dp, top = 6.dp)
                                )

                                is AnswerText.Parsed.Block.Paragraph -> Text(
                                    text = AnswerText.annotated(block.text, accent) { index ->
                                        result?.sourceFor(index)?.let(onOpenSource)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = BrowseInk,
                                    modifier = Modifier.padding(bottom = 17.dp)
                                )

                                is AnswerText.Parsed.Block.Bullet -> Row(
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .padding(top = 8.dp, end = 10.dp)
                                            .size(7.dp)
                                            .background(BrowseAccent, CircleShape)
                                    )
                                    Text(
                                        text = AnswerText.annotated(block.text, accent) { index ->
                                            result?.sourceFor(index)?.let(onOpenSource)
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = BrowseInk
                                    )
                                }
                            }
                        }

                        if (streaming && markdown.isEmpty()) {
                            item { StreamingSkeleton() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceCard(
    source: AnswerSource,
    accent: Color,
    onOpen: (AnswerSource) -> Unit
) {
    Surface(
        onClick = { onOpen(source) },
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE8E3ED)),
        shadowElevation = 2.dp,
        modifier = Modifier.width(248.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(accent.copy(alpha = 0.13f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        source.index.toString(),
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    source.host,
                    color = BrowseMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Rounded.OpenInNew,
                    contentDescription = "Open source",
                    tint = BrowseMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                source.title,
                color = BrowseInk,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (source.snippet.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    source.snippet,
                    color = BrowseMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "OPEN SOURCE",
                color = BrowseAccent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Quiet skeleton blocks while the generated answer is streaming. */
@Composable
private fun StreamingSkeleton() {
    val transition = rememberInfiniteTransition(label = "browse-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "browse-skeleton-alpha"
    )
    Column {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 3) 0.58f else 0.94f)
                    .height(13.dp)
                    .background(Color(0xFFB8AFC4).copy(alpha = alpha), RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.height(11.dp))
        }
    }
}
