package com.dude555afk.xeno.ui.browser

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.dude555afk.xeno.R

private val CandyCanvasTop = Color(0xFFD4D0F7)
private val CandyCanvasMiddle = Color(0xFFC4BDF1)
private val CandyCanvasBottom = Color(0xFFE9E4FA)
private val XenoInk = Color(0xFF302D43)
private val XenoMutedInk = Color(0xFF6F6A82)
private val XenoLime = Color(0xFFC9F63E)

@Composable
fun HomeScreen(
    onNewSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onVoiceSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to CandyCanvasTop,
                        0.48f to CandyCanvasMiddle,
                        1.0f to CandyCanvasBottom
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val searchInteraction = remember { MutableInteractionSource() }
        val searchPressed by searchInteraction.collectIsPressedAsState()
        val searchScale by animateFloatAsState(
            targetValue = if (searchPressed) 0.985f else 1f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 560f),
            label = "xeno-search-press"
        )

        // Soft, low-contrast light pools provide depth without obscuring the Xeno mark.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 78.dp, y = (-86).dp)
                .size(250.dp)
                .background(Color.White.copy(alpha = 0.16f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-90).dp, y = 94.dp)
                .size(260.dp)
                .background(Color(0xFFEEE9FF).copy(alpha = 0.52f), CircleShape)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(34.dp),
                color = Color(0xF7FFFFFF),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.92f)),
                shadowElevation = 14.dp,
                modifier = Modifier.size(132.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Xeno logo",
                        modifier = Modifier.size(112.dp)
                    )
                }
            }

            Text(
                text = "xeno",
                color = XenoInk,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.1).sp
            )
            Text(
                text = "A little less noise.",
                color = XenoMutedInk,
                fontSize = 14.sp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRIVATE BY DEFAULT",
                    color = XenoMutedInk.copy(alpha = 0.92f),
                    fontSize = 10.sp,
                    letterSpacing = 1.45.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    onClick = onOpenSettings,
                    shape = CircleShape,
                    color = Color(0xCCFFFFFF),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = XenoInk,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Surface(
                onClick = onNewSearch,
                shape = RoundedCornerShape(30.dp),
                color = Color(0xF7FFFFFF),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.95f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .scale(searchScale)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFECE8FF),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = Color(0xFF6254B8),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Search or ask Xeno",
                            color = XenoInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "A web search, a direct answer, or a URL",
                            color = XenoMutedInk,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = onVoiceSearch,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Voice search",
                            tint = XenoInk,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
