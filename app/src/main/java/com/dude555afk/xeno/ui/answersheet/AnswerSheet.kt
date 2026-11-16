package com.dude555afk.xeno.ui.answersheet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dude555afk.xeno.core.model.AnswerResult
import com.dude555afk.xeno.core.model.AnswerSource
import com.dude555afk.xeno.ui.arc.LocalArcMotionEnabled
import com.dude555afk.xeno.ui.arc.ArcAiWebTogglePill
import com.dude555afk.xeno.ui.arc.PixelBottomDock
import com.dude555afk.xeno.ui.render.AnswerText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sin
import java.net.HttpURLConnection
import java.net.URL

private val AnswerBg = Color(0xFF202126)
private val AnswerBlue = Color(0xFF5988FC)
private val AnswerWhite = Color.White
private val AnswerMuted = Color(0xFF8E8F94)

/** Browse for Me's loading and long-form answer surfaces. */
@Composable
fun AnswerSheet(
    result: AnswerResult?,
    streaming: Boolean,
    accent: Color,
    onOpenSource: (AnswerSource) -> Unit,
    onDismiss: () -> Unit,
    onOpenWeb: () -> Unit,
    onOpenTabs: () -> Unit,
    onNewTab: () -> Unit,
    onOpenControls: () -> Unit,
    tabCount: Int,
    onVoiceStart: () -> Unit = {},
    onVoiceEnd: () -> Unit = {},
    onSwipeTab: (Int) -> Unit = {}
) {
    val motion = LocalArcMotionEnabled.current
    val parsed = remember(result?.markdown) { AnswerText.parse(result?.markdown.orEmpty()) }
    val loadingMotion = rememberInfiniteTransition(label = "browse-loading")
    val shimmerPhase by loadingMotion.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(tween(1250), RepeatMode.Restart),
        label = "browse-domain-shimmer"
    )
    val readingPulse by loadingMotion.animateFloat(
        initialValue = 0.68f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "browse-reading-pulse"
    )
    val sources = result?.sources.orEmpty()
    val markdownReady = result?.markdown?.isNotBlank() == true
    val listState = rememberLazyListState()
    val imageSources = remember(sources) { sources.filter { !it.extracted?.imageUrl.isNullOrBlank() } }

    AnimatedVisibility(
        visible = result != null,
        enter = if (motion) fadeIn() else EnterTransition.None,
        exit = if (motion) fadeOut() else ExitTransition.None
    ) {
        Box(Modifier.fillMaxSize().background(AnswerBg)) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                if (markdownReady) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(74.dp).padding(start = 18.dp, end = 18.dp, top = 38.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = CircleShape,
                            color = Color(0xFF44454A)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFFA4A5A8), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    result?.query.orEmpty(),
                                    color = Color(0xFFF2F2F4),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(start = 22.dp, top = 54.dp)) {
                        Text(result?.query.orEmpty(), color = Color(0xFF5C86FE), fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (!markdownReady) {
                    Spacer(Modifier.height(180.dp))
                    var readingVisible by remember(result?.query) { mutableStateOf(false) }
                    LaunchedEffect(result?.query, markdownReady) {
                        delay(250)
                        readingVisible = true
                    }
                    AnimatedVisibility(
                        visible = readingVisible,
                        enter = fadeIn(tween(220)) + slideInVertically(initialOffsetY = { 12 })
                    ) {
                        Text(
                            "Reading 10 web pages",
                            color = AnswerBlue.copy(alpha = readingPulse),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 22.dp, bottom = 8.dp)
                        )
                    }
                    val domains = (
                        if (sources.isEmpty()) {
                            listOf("Searching the web...", "Collecting useful sources...", "Checking trusted pages...")
                        } else {
                            sources.map { it.host.removePrefix("www.") }
                        } + listOf("... and more")
                    ).distinct().take(6)
                    domains.forEachIndexed { index, domain ->
                        var rowVisible by remember(domain, result?.query) { mutableStateOf(false) }
                        LaunchedEffect(domain, result?.query, markdownReady) {
                            delay(250L + index * 60L)
                            rowVisible = true
                        }
                        AnimatedVisibility(
                            visible = rowVisible,
                            enter = fadeIn(tween(220)) + slideInVertically(initialOffsetY = { 12 })
                        ) {
                            Text(
                                domain,
                                color = Color(0xFF313E6A).copy(
                                    alpha = 0.62f + 0.3f * abs(sin((shimmerPhase + index * 0.55f).toDouble())).toFloat()
                                ),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth().height(41.dp)
                                    .padding(start = 22.dp, top = 9.dp)
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(25.dp))
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(start = 18.dp, end = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(13.dp)
                            ) {
                                if (imageSources.isNotEmpty()) {
                                    items(imageSources, key = { "image-${it.index}" }) { source ->
                                        AnswerImageCard(source, shimmerPhase, onClick = { onOpenSource(source) })
                                    }
                                } else {
                                    items(3) { index ->
                                        Box(
                                            Modifier.width(160.dp).height(238.dp).clip(RoundedCornerShape(22.dp))
                                                .background(Brush.linearGradient(listOf(Color(0xFF46464E), Color(0xFF2D3037), Color(0xFF292B31))))
                                        ) {
                                            MediaCardShimmer(shimmerPhase)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(25.dp))
                        }

                        val hero = parsed.blocks.firstOrNull() as? AnswerText.Parsed.Block.Heading
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { 16 })
                            ) {
                                Text(
                                    hero?.text ?: result?.query.orEmpty(),
                                    color = AnswerBlue,
                                    fontSize = 27.sp,
                                    lineHeight = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.4).sp,
                                    modifier = Modifier.padding(start = 19.dp, end = 19.dp, bottom = 19.dp)
                                )
                            }
                        }

                        itemsIndexed(parsed.blocks.drop(if (hero != null) 1 else 0)) { index, block ->
                            var blockVisible by remember(block) { mutableStateOf(false) }
                            LaunchedEffect(block, result?.query) {
                                delay(240L + index * 70L)
                                blockVisible = true
                            }
                            AnimatedVisibility(
                                visible = blockVisible,
                                enter = fadeIn(tween(230)) + slideInVertically(initialOffsetY = { 16 })
                            ) {
                            when (block) {
                                is AnswerText.Parsed.Block.Heading -> Text(
                                    block.text,
                                    color = AnswerWhite,
                                    fontSize = 20.sp,
                                    lineHeight = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 19.dp, end = 19.dp, top = 27.dp, bottom = 12.dp)
                                )
                                is AnswerText.Parsed.Block.Paragraph -> CitationText(
                                    raw = block.text,
                                    sources = sources,
                                    onOpenSource = onOpenSource,
                                    modifier = Modifier.padding(horizontal = 19.dp, vertical = 8.dp)
                                )
                                is AnswerText.Parsed.Block.Bullet -> Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 21.dp, end = 19.dp, top = 9.dp, bottom = 9.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        block.text.take(2).trim(),
                                        color = AnswerWhite,
                                        fontSize = 16.sp,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    CitationText(
                                        raw = block.text.drop(2).trimStart(),
                                        sources = sources,
                                        onOpenSource = onOpenSource,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            }
                        }

                        if (sources.isNotEmpty()) {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 19.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(sources, key = { "card-${it.index}" }) { source ->
                                        SourceMiniCard(source, onClick = { onOpenSource(source) })
                                    }
                                }
                                Spacer(Modifier.height(28.dp))
                            }
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(Color(0xFF232323), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                        .padding(start = 19.dp, end = 19.dp, top = 25.dp, bottom = 24.dp)
                                ) {
                                    Text("Sources", color = AnswerWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp))
                                    sources.forEach { source ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(55.dp).clickable { onOpenSource(source) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(shape = RoundedCornerShape(7.dp), color = Color(0xFF323337), modifier = Modifier.size(31.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFFCACBD0), modifier = Modifier.size(17.dp))
                                                }
                                            }
                                            Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                Text(source.title.ifBlank { source.host }, color = AnswerWhite, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(source.host.removePrefix("www."), color = AnswerMuted, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ArcAiWebTogglePill(
                webMode = false,
                onToggle = onOpenWeb,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 17.dp, bottom = 90.dp)
            )

            PixelBottomDock(
                tabCount = tabCount,
                homeMode = false,
                onOpenTabs = onOpenTabs,
                onNewTab = onNewTab,
                onOpenControls = onOpenControls,
                onVoiceStart = onVoiceStart,
                onVoiceEnd = onVoiceEnd,
                onSwipeTab = onSwipeTab,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun CitationText(
    raw: String,
    sources: List<AnswerSource>,
    onOpenSource: (AnswerSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotated = remember(raw) {
        AnswerText.annotated(raw, AnswerBlue) { /* Tap handled by ClickableText below. */ }
    }
    ClickableText(
        text = annotated,
        modifier = modifier.fillMaxWidth(),
        style = androidx.compose.ui.text.TextStyle(
            color = AnswerWhite,
            fontSize = 15.3.sp,
            lineHeight = 22.sp
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(AnswerText.CITE_TAG, offset, offset).firstOrNull()
                ?.item?.toIntOrNull()?.let { index ->
                    sources.firstOrNull { it.index == index }?.let(onOpenSource)
                }
        }
    )
}

@Composable
private fun SourceMiniCard(source: AnswerSource, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(119.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1C1D21)
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(source.title.ifBlank { source.host }, color = AnswerWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(source.host.removePrefix("www."), color = AnswerMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AnswerImageCard(source: AnswerSource, shimmerPhase: Float, onClick: () -> Unit) {
    val imageUrl = source.extracted?.imageUrl.orEmpty()
    val bitmap by produceState<Bitmap?>(initialValue = null, imageUrl) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(imageUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 7000
                connection.instanceFollowRedirects = true
                connection.inputStream.use { input -> BitmapFactory.decodeStream(input) }.also { connection.disconnect() }
            }.getOrNull()
        }
    }
    Box(
        Modifier.width(160.dp).height(238.dp).clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF2D3037)).clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(bitmap!!.asImageBitmap(), contentDescription = source.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF46464E), Color(0xFF2D3037), Color(0xFF292B31)))))
        }
        MediaCardShimmer(shimmerPhase)
        Text(
            source.host.removePrefix("www."),
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
        )
    }
}

@Composable
private fun MediaCardShimmer(phase: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val progress = (phase / 6.28318f).coerceIn(0f, 1f)
        val travel = progress * (size.width + size.height) - size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                start = Offset(travel, 0f),
                end = Offset(travel + size.height * 0.42f, size.height)
            )
        )
    }
}
