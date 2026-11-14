package com.dude555afk.xeno.ui.arc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SettingsBg = Color(0xFF1A1B1F)
private val SettingsWhite = Color.White
private val SettingsMuted = Color(0xFFA4A5A8)
private val SettingsSubtitle = Color(0xFFB0B1B5)
private val SettingsToggleOn = Color(0xFF6858FF)
private val SettingsToggleOff = Color(0xFF78727C)

@Composable
fun ArcSettingsScreen(
    visible: Boolean,
    themeMode: String,
    accentOverrideArgb: Int,
    animationsEnabled: Boolean,
    blockingEnabled: Boolean,
    autoOpenKeyboard: Boolean,
    onThemeModeChange: (String) -> Unit,
    onAccentChange: (Int) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onBlockingChange: (Boolean) -> Unit,
    onAutoOpenKeyboardChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onShare: () -> Unit = {},
    onSetDefaultBrowser: () -> Unit = {},
    onClearBrowsingData: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onExportData: () -> Unit = {}
) {
    val motion = LocalArcMotionEnabled.current
    AnimatedVisibility(
        visible = visible,
        enter = if (motion) fadeIn() else EnterTransition.None,
        exit = if (motion) fadeOut() else ExitTransition.None
    ) {
        Surface(Modifier.fillMaxSize(), color = SettingsBg) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(62.dp).padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color(0xFFE2E2E6), modifier = Modifier.size(23.dp))
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(42.dp))
                    Text(
                        "App Settings",
                        color = SettingsWhite,
                        fontSize = 26.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.25).sp
                    )
                    Spacer(Modifier.height(38.dp))

                    SettingsRow(
                        icon = Icons.Rounded.Redeem,
                        title = "Gift Arc Search to a Friend",
                        onClick = onShare
                    )

                    SectionLabel("Sync")
                    SettingsRow(
                        icon = Icons.Rounded.Sync,
                        title = "Sync with Arc Desktop",
                        subtitle = "Sign In"
                    )

                    SectionLabel("Global")
                    SettingsRow(
                        icon = Icons.Rounded.Language,
                        title = "Global Site Settings",
                        subtitle = "Theme: ${themeMode.replaceFirstChar { it.uppercase() }}",
                        onClick = {
                            val next = when (themeMode) {
                                "system" -> "dark"
                                "dark" -> "light"
                                else -> "system"
                            }
                            onThemeModeChange(next)
                        }
                    )

                    SectionLabel("Defaults")
                    SettingsRow(
                        icon = Icons.Rounded.OpenInBrowser,
                        title = "Set Arc as Default Browser",
                        onClick = onSetDefaultBrowser
                    )
                    SettingsRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Change App Icon",
                        subtitle = "Tap to cycle the accent colour",
                        onClick = {
                            val values = listOf(0, 0xFF7C5CFF.toInt(), 0xFF00D8D8.toInt(), 0xFFFF6B5C.toInt(), 0xFFB8FF3D.toInt(), 0xFFFFB03D.toInt(), 0xFFFF5CA8.toInt())
                            val current = values.indexOf(accentOverrideArgb).coerceAtLeast(0)
                            onAccentChange(values[(current + 1) % values.size])
                        }
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Search,
                        title = "Search Engines",
                        subtitle = "DuckDuckGo"
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Translate,
                        title = "Languages"
                    )

                    SectionLabel("Storage")
                    SettingsRow(
                        icon = Icons.Rounded.Archive,
                        title = "Archive"
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Archive,
                        title = "Archive Inactive Tabs",
                        subtitle = "After 1 day"
                    )
                    SettingsRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Clear Browsing Data",
                        onClick = onClearBrowsingData
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Folder,
                        title = "Downloads",
                        onClick = onOpenDownloads
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Share,
                        title = "Export Data",
                        subtitle = "Share your open tab URLs",
                        onClick = onExportData
                    )

                    SectionLabel("Options")
                    SettingsToggleRow(
                        icon = Icons.Rounded.Keyboard,
                        title = "Auto-Open Keyboard",
                        checked = autoOpenKeyboard,
                        onCheckedChange = onAutoOpenKeyboardChange
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Block,
                        title = "Block Ads and Trackers",
                        checked = blockingEnabled,
                        onCheckedChange = onBlockingChange
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Settings,
                        title = "Animations",
                        checked = animationsEnabled,
                        onCheckedChange = onAnimationsChange
                    )

                    SectionLabel("Support")
                    SupportRow("Contact the Team")
                    SupportRow("Privacy Policy")
                    SupportRow("Show Onboarding")
                    SupportRow("Open Source Licenses")
                    Spacer(Modifier.height(26.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Xeno Browser Version", color = SettingsWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("0.1.0", color = SettingsSubtitle, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = SettingsMuted,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(if (subtitle == null) 49.dp else 55.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFC4C5C9), modifier = Modifier.size(22.dp))
        Column(Modifier.padding(start = 19.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = SettingsWhite, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = SettingsSubtitle, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(53.dp).clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFC4C5C9), modifier = Modifier.size(22.dp))
        Text(title, color = SettingsWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 19.dp).weight(1f))
        Box(
            modifier = Modifier.width(56.dp).height(33.dp)
                .clip(CircleShape)
                .background(if (checked) SettingsToggleOn else SettingsBg)
                .then(if (!checked) Modifier.background(SettingsBg, CircleShape) else Modifier),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            if (!checked) {
                Box(Modifier.matchParentSize().clip(CircleShape).background(Color.Transparent))
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, SettingsToggleOff)
                ) {}
            }
            Box(
                Modifier.padding(horizontal = if (checked) 3.dp else 8.dp)
                    .size(if (checked) 25.dp else 16.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color(0xFF1E1966) else Color(0xFF8E8A94))
            )
        }
    }
}

@Composable
private fun SupportRow(title: String) {
    Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = SettingsWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
