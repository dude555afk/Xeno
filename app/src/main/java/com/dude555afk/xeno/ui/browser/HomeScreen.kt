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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
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
import com.dude555afk.xeno.ui.theme.SurfaceCard
import com.dude555afk.xeno.ui.theme.SurfaceCardHigh
import com.dude555afk.xeno.ui.theme.StrokeSubtle
import com.dude555afk.xeno.ui.theme.TextPrimary
import com.dude555afk.xeno.ui.theme.TextSecondary

/**
 * Minimal new-tab surface: quiet hierarchy, one primary action, real Xeno mark.
 * No invented activity cards or decorative stats.
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
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(bottom = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Xeno",
                modifier = Modifier.size(76.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "xeno",
                color = TextPrimary,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1.8).sp
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text = "A little more room to think.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(30.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PillShape)
                    .clickable(onClick = onSearch),
                shape = PillShape,
                color = SurfaceCardHigh,
                border = BorderStroke(1.dp, StrokeSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Search the web",
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "Search",
                            tint = Color.Black,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(SurfaceCard)
                    .clickable(onClick = onSearch)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = "ASK XENO",
                    modifier = Modifier.padding(start = 8.dp),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp
                )
            }
        }

        Text(
            text = "LOCAL-FIRST  ·  NO TELEMETRY",
            modifier = Modifier.align(Alignment.BottomCenter),
            color = TextSecondary.copy(alpha = 0.45f),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.4.sp
        )
    }
}
