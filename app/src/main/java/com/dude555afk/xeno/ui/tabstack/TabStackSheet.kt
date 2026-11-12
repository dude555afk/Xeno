package com.dude555afk.xeno.ui.tabstack

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.Tab

/**
 * Arc Search-style Android tab switcher: a horizontal strip of large page
 * cards. It deliberately uses LazyRow rather than a vertical sheet/list.
 */
@Composable
fun TabStackSheet(
    visible: Boolean,
    tabs: List<Tab>,
    activeTabId: Long?,
    accent: Color,
    onSelect: (Tab) -> Unit,
    onClose: (Tab) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = visible, onBack = onDismiss)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(170)) + scaleIn(initialScale = 0.985f, animationSpec = tween(200)),
        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.985f, animationSpec = tween(160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6121020))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 46.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Your tabs",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${tabs.size} open",
                            color = Color.White.copy(alpha = 0.62f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close tabs", tint = Color.White)
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        ArcTabCard(
                            tab = tab,
                            active = tab.id == activeTabId,
                            accent = accent,
                            onSelect = { onSelect(tab) },
                            onClose = { onClose(tab) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Swipe sideways to switch tabs",
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        onClick = onNewTab,
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF6F2FB)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color(0xFF2A2334),
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                "New tab",
                                color = Color(0xFF2A2334),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArcTabCard(
    tab: Tab,
    active: Boolean,
    accent: Color,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.965f,
        animationSpec = tween(180),
        label = "arc-tab-card-scale"
    )
    val palette = when ((tab.id % 4).toInt()) {
        0 -> listOf(Color(0xFF34216F), Color(0xFF9D74C2))
        1 -> listOf(Color(0xFF17465C), Color(0xFF8CC9C2))
        2 -> listOf(Color(0xFF713D59), Color(0xFFE5A27F))
        else -> listOf(Color(0xFF3B465F), Color(0xFF9AB5D1))
    }
    val host = remember(tab.url) {
        runCatching { java.net.URI(tab.url).host?.removePrefix("www.") ?: tab.url }
            .getOrDefault(tab.url)
    }

    Surface(
        onClick = onSelect,
        modifier = Modifier
            .width(286.dp)
            .fillMaxHeight(0.78f)
            .scale(scale),
        shape = RoundedCornerShape(27.dp),
        color = Color(0xFFFAF8FC),
        border = BorderStroke(
            if (active) 2.dp else 1.dp,
            if (active) accent else Color.White.copy(alpha = 0.35f)
        ),
        shadowElevation = if (active) 20.dp else 10.dp
    ) {
        Column(Modifier.fillMaxSize().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = Color(0xFF7C6A8D),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    host.ifBlank { "New tab" },
                    color = Color(0xFF706879),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 7.dp)
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close tab",
                        tint = Color(0xFF746B7D),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(174.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Brush.linearGradient(palette)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.28f),
                    modifier = Modifier.size(60.dp)
                )
            }

            Text(
                tab.displayTitle.ifBlank { "New tab" },
                color = Color(0xFF282231),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 17.dp)
            )
            Text(
                tab.url.ifBlank { "Search the web" },
                color = Color(0xFF8B8292),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp)
            )

            Spacer(Modifier.height(17.dp))
            repeat(3) { index ->
                Box(
                    Modifier
                        .fillMaxWidth(if (index == 2) 0.62f else 0.92f)
                        .padding(bottom = 8.dp)
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFEAE6EF))
                )
            }
            Spacer(Modifier.weight(1f))
            if (active) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Text(
                        "CURRENT TAB",
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
