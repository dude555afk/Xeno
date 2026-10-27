package com.dude555afk.xeno.ui.answersheet

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.dude555afk.xeno.core.model.AnswerResult
import com.dude555afk.xeno.core.model.AnswerSource
import com.dude555afk.xeno.ui.render.AnswerText

private val AnswerBackground = Color(0xFFFAF8FD)
private val AnswerInk = Color(0xFF282333)
private val AnswerMuted = Color(0xFF898295)
private val AnswerAccent = Color(0xFF5B49E8)

/**
 * A full-page Browse for Me result rather than a generic modal bottom sheet.
 * Sources remain tappable, the answer streams as readable sections, and the
 * follow-up field stays attached to the bottom of the page.
 */
@Composable
fun AnswerSheet(
    result: AnswerResult?,
    streaming: Boolean,
    accent: Color,
    onOpenSource: (AnswerSource) -> Unit,
    onDismiss: () -> Unit,
    onFollowUp: (String) -> Unit = {}
) {
    val parsed = remember(result?.markdown) { AnswerText.parse(result?.markdown.orEmpty()) }
    val listState = rememberLazyListState()
    var followUp by remember(result?.query) { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val sources = result?.sources.orEmpty()
    val markdown = result?.markdown.orEmpty()

    LaunchedEffect(result?.query, markdown.length) {
        if (streaming && markdown.isNotBlank()) {
            val target = (parsed.blocks.size - 1).coerceAtLeast(0)
            runCatching { listState.animateScrollToItem(target) }
        }
    }

    AnimatedVisibility(
        visible = result != null,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(androidx.compose.animation.core.tween(180)) +
            scaleIn(initialScale = 0.99f, animationSpec = androidx.compose.animation.core.tween(180)),
        exit = fadeOut(androidx.compose.animation.core.tween(130)) +
            scaleOut(targetScale = 0.99f, animationSpec = androidx.compose.animation.core.tween(130))
    ) {
        if (result != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AnswerBackground)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 7.dp, end = 18.dp, top = 5.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back to browser",
                            tint = AnswerInk
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = AnswerAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Browse for Me",
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                        color = AnswerInk,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (streaming) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(AnswerAccent, CircleShape)
                        )
                    }
                }

                if (streaming) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = AnswerAccent,
                        trackColor = Color(0xFFE8E2F2)
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    item(key = "query") {
                        Text(
                            text = result.query,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 25.sp,
                                lineHeight = 31.sp
                            ),
                            color = AnswerInk,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }

                    if (markdown.isBlank() && streaming) {
                        item(key = "skeleton") { StreamingSkeleton() }
                    }

                    itemsIndexed(parsed.blocks) { _, block ->
                        when (block) {
                            is AnswerText.Parsed.Block.Heading -> Text(
                                text = block.text,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    lineHeight = 27.sp
                                ),
                                color = AnswerInk,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 18.dp, bottom = 3.dp)
                            )
                            is AnswerText.Parsed.Block.Paragraph -> Text(
                                text = AnswerText.annotated(block.text, AnswerAccent) { index ->
                                    result.sourceFor(index)?.let(onOpenSource)
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = AnswerInk,
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp
                                ),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            is AnswerText.Parsed.Block.Bullet -> Row(
                                modifier = Modifier.padding(bottom = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = AnswerAccent,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(end = 10.dp, top = 1.dp)
                                )
                                Text(
                                    text = AnswerText.annotated(block.text, AnswerAccent) { index ->
                                        result.sourceFor(index)?.let(onOpenSource)
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = AnswerInk,
                                        fontSize = 15.sp,
                                        lineHeight = 23.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (sources.isNotEmpty()) {
                        item(key = "sources-title") {
                            Text(
                                text = "Sources",
                                color = AnswerInk,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 22.dp, bottom = 5.dp)
                            )
                        }
                        item(key = "sources-list") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 4.dp)
                            ) {
                                items(sources, key = { it.index }) { source ->
                                    SourceCard(source = source, onOpen = onOpenSource)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(25.dp),
                        color = Color(0xFFF0ECF7),
                        border = BorderStroke(1.dp, Color(0xFFE1DAEE)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 7.dp, top = 9.dp, bottom = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = followUp,
                                onValueChange = { followUp = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AnswerInk, fontSize = 14.sp),
                                cursorBrush = SolidColor(AnswerAccent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    val next = followUp.trim()
                                    if (next.isNotBlank()) {
                                        keyboard?.hide()
                                        followUp = ""
                                        onFollowUp(next)
                                    }
                                }),
                                decorationBox = { inner ->
                                    if (followUp.isBlank()) {
                                        Text("Ask a follow-up…", color = AnswerMuted, fontSize = 14.sp)
                                    }
                                    inner()
                                }
                            )
                            IconButton(
                                onClick = {
                                    val next = followUp.trim()
                                    if (next.isNotBlank()) {
                                        keyboard?.hide()
                                        followUp = ""
                                        onFollowUp(next)
                                    }
                                },
                                enabled = followUp.isNotBlank(),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowForward,
                                    contentDescription = "Send follow-up",
                                    tint = if (followUp.isNotBlank()) AnswerAccent else AnswerMuted
                                )
                            }
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
    onOpen: (AnswerSource) -> Unit
) {
    Surface(
        onClick = { onOpen(source) },
        shape = RoundedCornerShape(19.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE9E3F1)),
        shadowElevation = 1.dp,
        modifier = Modifier.width(232.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color(0xFFEAE5FA), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = source.index.toString(),
                        color = AnswerAccent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = source.host.removePrefix("www."),
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = AnswerMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(
                text = source.title,
                style = MaterialTheme.typography.bodyMedium,
                color = AnswerInk,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (source.snippet.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = source.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = AnswerMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = "Open source", tint = AnswerAccent, modifier = Modifier.size(15.dp))
                Text(
                    "Open source",
                    modifier = Modifier.padding(start = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = AnswerAccent
                )
            }
        }
    }
}

@Composable
private fun StreamingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 2) 0.62f else 0.98f)
                    .height(if (index == 0) 18.dp else 13.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFFE7E1F0))
            )
        }
        Spacer(Modifier.height(8.dp))
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 3) 0.72f else 1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEDE8F4))
            )
        }
    }
}
