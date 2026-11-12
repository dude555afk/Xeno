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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
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
    onOpenSettings: () -> Unit,
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
            onOpenControls = onOpenSettings,
            homeMode = true,
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
    homeMode: Boolean = false,
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
                    if (homeMode) Icons.Rounded.Settings else Icons.Rounded.ExpandMore,
                    contentDescription = if (homeMode) "Settings" else "Browser controls",
                    tint = ArcInk,
                    modifier = Modifier.size(23.dp)
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

/** Arc-style settings entry point from the start screen. */
@Composable
fun ArcHomeSettingsSheet(
    visible: Boolean,
    blockingEnabled: Boolean,
    onBlockingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
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
                        .padding(horizontal = 22.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "App Settings",
                                color = ArcInk,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Xeno Browser",
                                color = ArcMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close settings", tint = ArcInk)
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    Text(
                        "PRIVACY",
                        color = ArcViolet,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBlockingChange(!blockingEnabled) }
                            .padding(vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Block Ads & Trackers",
                                color = ArcInk,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                if (blockingEnabled) "Enabled for pages you visit" else "Protection is paused",
                                color = ArcMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (blockingEnabled) Color(0xFF5B48C8) else Color(0xFFD9D4DE)
                        ) {
                            Box(
                                Modifier.width(44.dp).height(26.dp).padding(3.dp),
                                contentAlignment = if (blockingEnabled) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    Modifier.size(20.dp).clip(CircleShape).background(Color.White)
                                )
                            }
                        }
                    }
                    Box(
                        Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEAE5EF))
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "ABOUT",
                        color = ArcViolet,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Chromium-based browsing with Xeno's own interface.",
                        color = ArcInk,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Version 0.1.0",
                        color = ArcMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Arc Search Android's page-control surface: history controls, four primary
 * actions, and expandable display/site settings.
 */
@Composable
fun ArcSiteActionsSheet(
    visible: Boolean,
    title: String,
    url: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isPinned: Boolean,
    blockingEnabled: Boolean,
    pageTextZoom: Int,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onCopyUrl: () -> Unit,
    onFindInPage: () -> Unit,
    onSummarize: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
    onBlockingChange: (Boolean) -> Unit,
    onZoomChange: (Int) -> Unit
) {
    var displayOptionsOpen by remember { mutableStateOf(false) }
    var siteSettingsOpen by remember { mutableStateOf(false) }

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
                        .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 17.dp)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD5D0DA))
                    )
                    Spacer(Modifier.height(12.dp))

                    // Arc's compact navigation/address row.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RoundControl(Icons.Rounded.ArrowBack, "Back", canGoBack, onBack)
                        RoundControl(Icons.Rounded.ArrowForward, "Forward", canGoForward, onForward)
                        Surface(
                            onClick = onCopyUrl,
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEAE5EF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    runCatching { java.net.URI(url).host?.removePrefix("www.") }
                                        .getOrNull().orEmpty().ifBlank { title.ifBlank { "Search" } },
                                    color = ArcInk,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = "Copy current address",
                                    tint = ArcMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        RoundControl(Icons.Rounded.Refresh, "Reload", url.isNotBlank(), onReload)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionTile(
                            "Find on Page",
                            Icons.Rounded.Search,
                            onFindInPage,
                            Modifier.weight(1f)
                        )
                        ActionTile(
                            "Summarize",
                            Icons.Rounded.AutoAwesome,
                            onSummarize,
                            Modifier.weight(1f)
                        )
                        ActionTile(
                            if (isPinned) "Unpin" else "Pin",
                            Icons.Rounded.PushPin,
                            onTogglePin,
                            Modifier.weight(1f)
                        )
                        ActionTile(
                            "Share",
                            Icons.Rounded.Share,
                            onShare,
                            Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    MenuSectionRow(
                        title = "Display Options",
                        subtitle = "Text size and page display",
                        expanded = displayOptionsOpen,
                        onClick = { displayOptionsOpen = !displayOptionsOpen }
                    )
                    if (displayOptionsOpen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Text size",
                                    color = ArcInk,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "$pageTextZoom%",
                                    color = ArcMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            SmallTextControl("A−", enabled = pageTextZoom > 50) {
                                onZoomChange(-10)
                            }
                            Spacer(Modifier.width(8.dp))
                            SmallTextControl("A+", enabled = pageTextZoom < 200) {
                                onZoomChange(10)
                            }
                        }
                    }

                    MenuSectionRow(
                        title = "Site Settings",
                        subtitle = "Privacy and content controls",
                        expanded = siteSettingsOpen,
                        onClick = { siteSettingsOpen = !siteSettingsOpen }
                    )
                    if (siteSettingsOpen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBlockingChange(!blockingEnabled) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Block ads and trackers",
                                    color = ArcInk,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    if (blockingEnabled) "Protection is on" else "Protection is paused",
                                    color = ArcMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (blockingEnabled) Color(0xFF5B48C8) else Color(0xFFD9D4DE)
                            ) {
                                Box(
                                    Modifier
                                        .width(42.dp)
                                        .height(25.dp)
                                        .padding(3.dp),
                                    contentAlignment = if (blockingEnabled) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Box(
                                        Modifier
                                            .size(19.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (blockingEnabled) "Xeno protection is active on this page"
                        else "Content protection is paused",
                        color = ArcMuted,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEAE5EF))
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) ArcInk else ArcMuted.copy(alpha = 0.35f),
            modifier = Modifier.padding(10.dp).size(18.dp)
        )
    }
}

@Composable
private fun MenuSectionRow(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = Color(0xFFF0EDF4),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 7.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = ArcInk, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, color = ArcMuted, style = MaterialTheme.typography.labelSmall)
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = ArcMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SmallTextControl(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) Color.White else Color(0xFFF0EDF4),
        border = BorderStroke(1.dp, Color(0xFFEAE5EF))
    ) {
        Text(
            label,
            color = if (enabled) ArcInk else ArcMuted.copy(alpha = 0.5f),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
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
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEAE5EF))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
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


/** Compact page-find overlay shown from the Arc-style page controls. */
@Composable
fun ArcFindInPageBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { requester.requestFocus() }
        keyboard?.show()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF9F7FB),
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
    ) {
        Row(
            modifier = Modifier.padding(start = 15.dp, end = 5.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = ArcMuted,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(requester),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = ArcInk),
                cursorBrush = SolidColor(ArcViolet),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
                decorationBox = { inner ->
                    Box {
                        if (query.isBlank()) {
                            Text(
                                "Find on page",
                                color = ArcMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        inner()
                    }
                }
            )
            IconButton(onClick = onPrevious, enabled = query.isNotBlank()) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = "Previous match",
                    tint = if (query.isNotBlank()) ArcInk else ArcMuted.copy(alpha = 0.35f)
                )
            }
            IconButton(onClick = onNext, enabled = query.isNotBlank()) {
                Icon(
                    Icons.Rounded.ArrowDownward,
                    contentDescription = "Next match",
                    tint = if (query.isNotBlank()) ArcInk else ArcMuted.copy(alpha = 0.35f)
                )
            }
            IconButton(onClick = {
                keyboard?.hide()
                onClose()
            }) {
                Icon(Icons.Rounded.Close, contentDescription = "Close find", tint = ArcInk)
            }
        }
    }
}
