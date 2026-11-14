package com.dude555afk.xeno.ui.answersheet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.dude555afk.xeno.ui.arc.PixelBottomDock
import com.dude555afk.xeno.ui.render.AnswerText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    tabCount: Int
) {
    val motion = LocalArcMotionEnabled.current
    val parsed = remember(result?.markdown) { AnswerText.parse(result?.markdown.orEmpty()) }
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
                        modifier = Modifier.fillMaxWidth().height(74.dp).padding(start = 18.dp, end = 18.dp, top = 16.dp),
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
                    Column(Modifier.fillMaxWidth().padding(start = 19.dp, top = 28.dp)) {
                        Text(result?.query.orEmpty(), color = Color(0xFF5C86FE), fontSize = 27.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (!markdownReady) {
                    Spacer(Modifier.height(180.dp))
                    Text(
                        "Reading 10 web pages",
                        color = AnswerBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 19.dp, bottom = 8.dp)
                    )
                    val domains = (sources.map { it.host.removePrefix("www.") } + listOf("... and more"))
                        .distinct().take(6)
                    domains.forEach { domain ->
                        Text(
                            domain,
                            color = Color(0xFF313E6A),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 19.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                    if (sources.isEmpty()) {
                        listOf("Searching the web...", "Collecting useful sources...", "... and more").forEach {
                            Text(it, color = Color(0xFF313E6A), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 19.dp, top = 4.dp, bottom = 4.dp))
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
                                        AnswerImageCard(source, onClick = { onOpenSource(source) })
                                    }
                                } else {
                                    items(3) { index ->
                                        Box(
                                            Modifier.width(136.dp).height(202.dp).clip(RoundedCornerShape(19.dp))
                                                .background(Brush.linearGradient(listOf(Color(0xFF46464E), Color(0xFF2D3037), Color(0xFF292B31))))
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(25.dp))
                        }

                        val hero = parsed.blocks.firstOrNull() as? AnswerText.Parsed.Block.Heading
                        item {
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

                        itemsIndexed(parsed.blocks.drop(if (hero != null) 1 else 0)) { _, block ->
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

            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 17.dp, bottom = 90.dp)
                    .width(91.dp).height(50.dp),
                shape = CircleShape,
                color = Color(0xFF3C404E)
            ) {
                Row(Modifier.fillMaxSize().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).background(Color(0xFF873EA7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = "Browse for me mode", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Box(
                        Modifier.weight(1f).fillMaxSize().clickable(onClick = onOpenWeb),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = "Switch to web search", tint = Color(0xFF7E8294), modifier = Modifier.size(20.dp))
                    }
                }
            }

            PixelBottomDock(
                tabCount = tabCount,
                homeMode = false,
                onOpenTabs = onOpenTabs,
                onNewTab = onNewTab,
                onOpenControls = onOpenControls,
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
private fun AnswerImageCard(source: AnswerSource, onClick: () -> Unit) {
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
        Modifier.width(136.dp).height(202.dp).clip(RoundedCornerShape(19.dp))
            .background(Color(0xFF2D3037)).clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(bitmap!!.asImageBitmap(), contentDescription = source.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF46464E), Color(0xFF2D3037), Color(0xFF292B31)))))
        }
        Text(
            source.host.removePrefix("www."),
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
        )
    }
}
