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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.dude555afk.xeno.core.model.AnswerResult
import com.dude555afk.xeno.core.model.AnswerSource
import com.dude555afk.xeno.ui.arc.LocalArcMotionEnabled
import com.dude555afk.xeno.ui.render.AnswerText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val ArcAnswerInk = Color(0xFF24212B)
private val ArcAnswerMuted = Color(0xFF7C7884)
private val ArcAnswerBlue = Color(0xFF5146D7)
private val ArcAnswerPaper = Color(0xFFFCFBFD)

/** Arc-style Browse for Me page with an image carousel and editorial answer. */
@Composable
fun AnswerSheet(
    result: AnswerResult?,
    streaming: Boolean,
    accent: Color,
    onOpenSource: (AnswerSource) -> Unit,
    onDismiss: () -> Unit
) {
    val motionEnabled = LocalArcMotionEnabled.current
    val parsed = remember(result?.markdown) { AnswerText.parse(result?.markdown.orEmpty()) }
    val listState = rememberLazyListState()
    val sources = result?.sources.orEmpty()
    val markdown = result?.markdown.orEmpty()
    val imageSources = remember(sources) { sources.filter { !it.extracted?.imageUrl.isNullOrBlank() } }

    AnimatedVisibility(
        visible = result != null,
        enter = if (motionEnabled) fadeIn() else EnterTransition.None,
        exit = if (motionEnabled) fadeOut() else ExitTransition.None
    ) {
        Surface(Modifier.fillMaxSize(), color = ArcAnswerPaper) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(58.dp).padding(start = 8.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = ArcAnswerInk)
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEAE8EF))
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = ArcAnswerMuted, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(9.dp))
                            Text(result?.query.orEmpty(), color = ArcAnswerInk, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    if (imageSources.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                items(imageSources, key = { it.index }) { source ->
                                    BrowseImageCard(source) { onOpenSource(source) }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    item {
                        Text(
                            result?.query.orEmpty(),
                            color = ArcAnswerBlue,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = if (imageSources.isEmpty()) 24.dp else 0.dp, bottom = 18.dp)
                        )
                    }

                    itemsIndexed(parsed.blocks) { _, block ->
                        when (block) {
                            is AnswerText.Parsed.Block.Heading -> Text(
                                block.text,
                                color = ArcAnswerInk,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp)
                            )
                            is AnswerText.Parsed.Block.Paragraph -> CitationText(
                                text = AnswerText.annotated(block.text, ArcAnswerBlue) { },
                                color = ArcAnswerInk,
                                onCitation = { index -> result?.sourceFor(index)?.let(onOpenSource) },
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp)
                            )
                            is AnswerText.Parsed.Block.Bullet -> Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(Modifier.padding(top = 9.dp, end = 11.dp).size(6.dp).background(ArcAnswerBlue, CircleShape))
                                CitationText(
                                    text = AnswerText.annotated(block.text, ArcAnswerBlue) { },
                                    color = ArcAnswerInk,
                                    onCitation = { index -> result?.sourceFor(index)?.let(onOpenSource) }
                                )
                            }
                        }
                    }

                    if (streaming && markdown.isEmpty()) item { StreamingSkeleton() }
                    if (streaming && markdown.isNotEmpty()) {
                        item {
                            Text("Writing…", color = ArcAnswerMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseImageCard(source: AnswerSource, onClick: () -> Unit) {
    val bitmap by produceState<Bitmap?>(null, source.extracted?.imageUrl) {
        value = loadBrowseImage(source.extracted?.imageUrl)
    }
    Box(
        modifier = Modifier.width(244.dp).height(166.dp).clip(RoundedCornerShape(17.dp))
            .background(Color(0xFFE6E1ED)).clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(bitmap!!.asImageBitmap(), contentDescription = source.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF4D4598), Color(0xFFB879AE)))))
        }
        Box(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC171421))))
                .padding(horizontal = 13.dp, vertical = 11.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(source.host, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(source.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CitationText(
    text: androidx.compose.ui.text.AnnotatedString,
    color: Color,
    onCitation: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ClickableText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(color = color),
        onClick = { offset ->
            text.getStringAnnotations(
                tag = AnswerText.CITE_TAG,
                start = offset,
                end = offset
            ).firstOrNull()?.item?.toIntOrNull()?.let(onCitation)
        }
    )
}

private suspend fun loadBrowseImage(url: String?): Bitmap? = withContext(Dispatchers.IO) {
    if (url.isNullOrBlank() || !(url.startsWith("https://") || url.startsWith("http://"))) return@withContext null
    var connection: HttpURLConnection? = null
    try {
        connection = URL(url).openConnection() as? HttpURLConnection ?: return@withContext null
        connection.connectTimeout = 4500
        connection.readTimeout = 5500
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36")
        connection.connect()
        if (connection.contentLengthLong > 4L * 1024L * 1024L) return@withContext null
        val bytes = connection.inputStream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > 4 * 1024 * 1024) return@withContext null
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sample = when {
            bounds.outWidth > 1600 || bounds.outHeight > 1200 -> 4
            bounds.outWidth > 1000 || bounds.outHeight > 800 -> 2
            else -> 1
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}

@Composable
private fun StreamingSkeleton() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        repeat(5) { index ->
            Box(Modifier.fillMaxWidth(if (index == 4) 0.56f else 0.94f).height(13.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE8E5EC)))
        }
    }
}
