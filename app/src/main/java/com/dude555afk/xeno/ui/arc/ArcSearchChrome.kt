package com.dude555afk.xeno.ui.arc

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.Tab

private val ArcInk = Color(0xFF211A2B)
private val ArcMuted = Color(0xFF827A8A)
private val ArcPaper = Color(0xFFFAF8FC)
private val ArcViolet = Color(0xFF6D50D8)

/**
 * Xeno's Arc Search-inspired new-tab surface.
 *
 * The layout intentionally follows the Android adaptation: a soft gradient
 * canvas, a single rounded search surface, compact "Browse for me" suggestions,
 * and the three-control bottom dock.
 */
@Composable
fun ArcHomeScreen(
    onOpenSearch: () -> Unit,
    onBrowseForMe: (String) -> Unit,
    onOpenTabs: () -> Unit,
    onNewTab: () -> Unit,
    onOpenControls: () -> Unit,
    tabCount: Int,
    modifier: Modifier = Modifier
) {
    val ideas = remember {
        listOf(
            "Summarize today's news",
            "museum shows near me",
            "quick chicken recipes",
            "fun things to do in NYC"
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF23106A),
                            Color(0xFF5142C9),
                            Color(0xFFB46BBE),
                            Color(0xFFE99AA5)
                        )
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 100.dp, end = 12.dp)
                .size(230.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x66FFB4D7), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 0.dp, bottom = 110.dp)
                .size(260.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x665E89FF), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 92.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arc's home screen leaves the upper gradient deliberately quiet.
            Spacer(Modifier.weight(1.35f))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                color = ArcPaper,
                shadowElevation = 18.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenSearch)
                            .padding(horizontal = 18.dp, vertical = 17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = ArcMuted,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Search or ask anything",
                            color = ArcMuted,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = "Browse for me",
                            tint = ArcViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    ideas.forEachIndexed { index, idea ->
                        if (index > 0) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 18.dp, end = 18.dp)
                                    .height(1.dp)
                                    .background(Color(0xFFEDE9F1))
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBrowseForMe(idea) }
                                .padding(start = 18.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = ArcMuted,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = idea,
                                color = ArcInk,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                onClick = { onBrowseForMe(idea) },
                                shape = CircleShape,
                                color = Color(0xFFEDE8FF)
                            ) {
                                Text(
                                    "Browse for me",
                                    color = ArcViolet,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1.15f))
        }

        ArcBottomDock(
            tabCount = tabCount,
            onOpenTabs = onOpenTabs,
            onNewTab = onNewTab,
            onOpenControls = onOpenControls,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

/** The compact three-control dock used on both the start page and web pages. */
@Composable
fun ArcBottomDock(
    tabCount: Int,
    onOpenTabs: () -> Unit,
    onNewTab: () -> Unit,
    onOpenControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onOpenTabs,
            shape = CircleShape,
            color = Color(0xDDF7F4FA),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
            shadowElevation = 8.dp
        ) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Layers,
                    contentDescription = "Open tabs",
                    tint = ArcInk,
                    modifier = Modifier.size(22.dp)
                )
                Surface(
                    shape = CircleShape,
                    color = ArcViolet,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 5.dp, end = 5.dp)
                ) {
                    Text(
                        tabCount.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Surface(
            onClick = onNewTab,
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            shadowElevation = 10.dp,
            modifier = Modifier
                .width(66.dp)
                .height(52.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFF9F7FC), Color(0xFFE7E1F2))
                    ),
                    RoundedCornerShape(22.dp)
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "New search",
                    tint = ArcInk,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Surface(
            onClick = onOpenControls,
            shape = CircleShape,
            color = Color(0xDDF7F4FA),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
            shadowElevation = 8.dp
        ) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = "Browser controls",
                    tint = ArcInk,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

@Composable
fun ArcPageAddress(
    url: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val host = remember(url) {
        runCatching {
            java.net.URI(url).host?.removePrefix("www.") ?: url
        }.getOrDefault(url).ifBlank { title }
    }
    Surface(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 26.dp),
        shape = CircleShape,
        color = Color(0xEFFFFFFF),
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = ArcMuted,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                host.ifBlank { "Search or enter address" },
                color = ArcInk,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = "Open address controls",
                tint = ArcMuted,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/** A small native-feeling action sheet for the bottom chevron. */
@Composable
fun ArcSiteActionsSheet(
    visible: Boolean,
    title: String,
    url: String,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onSummarize: () -> Unit,
    onNewTab: () -> Unit,
    onShowTabs: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(220)
        ),
        exit = fadeOut(tween(120)) + slideOutVertically(tween(170))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                color = Color(0xFFF9F7FB),
                shadowElevation = 18.dp
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {}
                        .navigationBarsPadding()
                        .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 20.dp)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD5D0DA))
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                title.ifBlank { "Browser controls" },
                                color = ArcInk,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                url,
                                color = ArcMuted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close controls", tint = ArcInk)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile("Back", Icons.Rounded.ArrowBack, onBack, Modifier.weight(1f))
                        ActionTile("Summarize", Icons.Rounded.AutoAwesome, onSummarize, Modifier.weight(1f))
                        ActionTile("New tab", Icons.Rounded.Add, onNewTab, Modifier.weight(1f))
                        ActionTile("Tabs", Icons.Rounded.Layers, onShowTabs, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEAE5EF))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = ArcViolet, modifier = Modifier.size(21.dp))
            Text(
                label,
                color = ArcInk,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
