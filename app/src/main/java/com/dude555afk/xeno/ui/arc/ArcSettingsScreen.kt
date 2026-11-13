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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Info
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
        enter = if (motion) fadeIn() else EnterTransition.None,
        exit = if (motion) fadeOut() else ExitTransition.None
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(
                        "Settings",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    SettingsSectionTitle("Appearance", Icons.Rounded.Palette)
                    Text("Theme", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { option ->
                            FilterChip(
                                selected = themeMode == option.first,
                                onClick = { onThemeModeChange(option.first) },
                                label = { Text(option.second) }
                            )
                        }
                    }

                    Text("Accent colour", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp, bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        AccentOptions.forEach { (name, argb) ->
                            val selected = accentOverrideArgb == argb
                            val color = if (argb == 0) MaterialTheme.colorScheme.primary else Color(argb)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Surface(
                                    onClick = { onAccentChange(argb) },
                                    shape = CircleShape,
                                    color = color,
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (selected) {
                                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = if (name == "Lime" || name == "Amber") Color.Black else Color.White, modifier = Modifier.size(17.dp))
                                        }
                                    }
                                }
                                Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    SettingsDivider()
                    SettingsSectionTitle("Search", Icons.Rounded.Search)
                    PlainToggleRow(
                        title = "Open search on launch",
                        description = "Show the keyboard when Xeno opens.",
                        checked = autoOpenKeyboard,
                        onCheckedChange = onAutoOpenKeyboardChange
                    )

                    SettingsDivider()
                    SettingsSectionTitle("Site Settings", Icons.Rounded.Security)
                    PlainToggleRow(
                        title = "Block ads and trackers",
                        description = "Apply Xeno's content filter across sites.",
                        checked = blockingEnabled,
                        onCheckedChange = onBlockingChange
                    )

                    SettingsDivider()
                    SettingsSectionTitle("Motion", Icons.Rounded.Settings)
                    PlainToggleRow(
                        title = "Animations",
                        description = "Reduce transitions and movement.",
                        checked = animationsEnabled,
                        onCheckedChange = onAnimationsChange
                    )

                    SettingsDivider()
                    SettingsSectionTitle("About", Icons.Rounded.Info)
                    PlainInfoRow("Version", "0.1.0")
                    PlainInfoRow("Engine", "Android System WebView · Chromium")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)))
}

@Composable
private fun PlainToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).clickable { onCheckedChange(!checked) }.padding(end = 12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PlainInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
