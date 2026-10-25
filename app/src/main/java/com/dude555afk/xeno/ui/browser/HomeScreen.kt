package com.dude555afk.xeno.ui.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dude555afk.xeno.R
import com.dude555afk.xeno.ui.theme.PillShape
import com.dude555afk.xeno.ui.theme.StrokeSubtle
import com.dude555afk.xeno.ui.theme.SurfaceCardHigh
import com.dude555afk.xeno.ui.theme.TextPrimary
import com.dude555afk.xeno.ui.theme.TextSecondary

/**
 * Arc Search-inspired new-tab surface, with Xeno's own identity.
 * One clear search action, quiet typography, no fake dashboard widgets.
 */
@Composable
fun HomeScreen(
    accent: Color,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Xeno",
                modifier = Modifier.size(58.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "What are you\ncurious about?",
                color = TextPrimary,
                fontSize = 31.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-1.25).sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Search the web, or ask Xeno.",
                color = TextSecondary.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PillShape)
                    .clickable(onClick = onSearch),
                shape = PillShape,
                color = Color(0xFF171717),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.11f))
            ) {
                Row(
                    modifier = Modifier.padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        text = "Ask anything or search",
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "Search",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.clip(PillShape).clickable(onClick = onSearch),
                    shape = PillShape,
                    color = Color(0xFF101010),
                    border = BorderStroke(1.dp, StrokeSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Ask Xeno",
                            modifier = Modifier.padding(start = 7.dp),
                            color = TextPrimary.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Text(
                    text = "PRIVATE BY DEFAULT",
                    color = TextSecondary.copy(alpha = 0.42f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.1.sp
                )
            }
        }
    }
}
