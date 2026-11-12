package com.dude555afk.xeno.ui.arc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val AccentOptions = listOf(
    "Space" to 0,
    "Violet" to 0xFF7C5CFF.toInt(),
    "Cyan" to 0xFF00D8D8.toInt(),
    "Coral" to 0xFFFF6B5C.toInt(),
    "Lime" to 0xFFB8FF3D.toInt(),
    "Amber" to 0xFFFFB03D.toInt(),
    "Rose" to 0xFFFF5CA8.toInt()
)

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
    onDismiss: () -> Unit
) {
    val motion = LocalArcMotionEnabled.current
    AnimatedVisibility(
        visible = visible,
        enter = if (motion) {
            fadeIn() + slideInVertically(
                initialOffsetY = { it / 14 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else EnterTransition.None,
        exit = if (motion) fadeOut() + slideOutVertically() else ExitTransition.None
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Make Xeno feel like yours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsGroup(
                        title = "Appearance",
                        subtitle = "Theme and browser accent",
                        icon = { Icon(Icons.Rounded.Palette, contentDescription = null) }
                    ) {
                        Text(
                            "Theme",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Choose how Xeno should look.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("system" to "System", "dark" to "Dark", "light" to "Light").forEach { (value, label) ->
                                FilterChip(
                                    selected = themeMode == value,
                                    onClick = { onThemeModeChange(value) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Accent colour",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Use the active space colour or pin one across Xeno.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 13.dp),
                            horizontalArrangement = Arrangement.spacedBy(13.dp)
                        ) {
                            AccentOptions.forEach { (name, argb) ->
                                val selected = accentOverrideArgb == argb
                                val swatch = if (argb == 0) MaterialTheme.colorScheme.primary else Color(argb)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        onClick = { onAccentChange(argb) },
                                        shape = CircleShape,
                                        color = swatch,
                                        border = BorderStroke(
                                            if (selected) 3.dp else 1.dp,
                                            if (selected) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                        ),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (selected) {
                                                Icon(
                                                    Icons.Rounded.Check,
                                                    contentDescription = "Selected",
                                                    tint = if (name == "Lime" || name == "Amber") Color.Black else Color.White,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    SettingsGroup(
                        title = "Search",
                        subtitle = "Control how a new session starts",
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) }
                    ) {
                        SettingsToggle(
                            title = "Open search on launch",
                            description = "Focus the search field and show the keyboard when Xeno opens.",
                            checked = autoOpenKeyboard,
                            onCheckedChange = onAutoOpenKeyboardChange
                        )
                    }

                    SettingsGroup(
                        title = "Privacy",
                        subtitle = "Protection across the pages you visit",
                        icon = { Icon(Icons.Rounded.Security, contentDescription = null) }
                    ) {
                        SettingsToggle(
                            title = "Block ads and trackers",
                            description = if (blockingEnabled) {
                                "Network requests matching Xeno's filter lists are blocked."
                            } else {
                                "Content protection is paused for every site."
                            },
                            checked = blockingEnabled,
                            onCheckedChange = onBlockingChange
                        )
                    }

                    SettingsGroup(
                        title = "Motion",
                        subtitle = "Transitions and browser chrome",
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) }
                    ) {
                        SettingsToggle(
                            title = "Use animations",
                            description = "Animate panels, chrome and settings transitions. Turn this off to reduce motion.",
                            checked = animationsEnabled,
                            onCheckedChange = onAnimationsChange
                        )
                    }

                    SettingsGroup(
                        title = "About Xeno",
                        subtitle = "A Chromium-based browser with its own interface",
                        icon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                    ) {
                        SettingsInfoRow("Version", "0.1.0")
                        SettingsInfoRow("Rendering engine", "Android System WebView · Chromium")
                        SettingsInfoRow("Interface", "Xeno Arc-style browser chrome")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        icon()
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable { onCheckedChange(!checked) }.padding(vertical = 5.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
