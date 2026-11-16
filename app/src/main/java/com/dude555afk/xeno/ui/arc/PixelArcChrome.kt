package com.dude555afk.xeno.ui.arc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import android.os.SystemClock
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

private val ArcAmbientBottom = Color(0xFF38425D)

/** The sampled Arc Search ambient background, built from overlapping colour fields. */
@Composable
fun PixelArcAmbientBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(ArcAmbientBottom)
        val w = size.width
        val h = size.height
        fun field(x: Float, y: Float, radius: Float, color: Color) {
            drawRect(
                brush = Brush.radialGradient(
                    0.0f to color,
                    1.0f to Color.Transparent,
                    center = Offset(w * x, h * y),
                    radius = radius
                )
            )
        }
        field(0.03f, 0.04f, w * 0.96f, Color(0xFF1A1438))
        field(0.50f, 0.00f, w * 0.78f, Color(0xFF332034))
        field(1.00f, 0.08f, w * 0.86f, Color(0xFF572F38))
        field(0.00f, 0.49f, w * 0.82f, Color(0xFF1F1D42))
        field(1.02f, 0.49f, w * 0.82f, Color(0xFF4D313F))
        field(0.50f, 0.67f, w * 0.95f, Color(0xFF363B58))
    }
}

/** Arc's five-limbed sticker mark, sized to the audit's 183×151 base grid. */
@Composable
fun PixelArcMark(modifier: Modifier = Modifier, enabled: Boolean = true) {
    var rotation by androidx.compose.runtime.remember { mutableFloatStateOf(0f) }
    var angularVelocity by androidx.compose.runtime.remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var flingJob by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Job?>(null) }

    Canvas(
        modifier
            .size(width = 156.dp, height = 128.dp)
            .graphicsLayer {
                rotationZ = rotation
                transformOrigin = TransformOrigin(0.5f, 0.53f)
            }
            .then(if (enabled) Modifier.pointerInput(enabled) {
                var previousAngle = 0.0
                var previousTime = 0L
                detectDragGestures(
                    onDragStart = { point ->
                        flingJob?.cancel()
                        val center = Offset(size.width * 0.5f, size.height * 0.53f)
                        previousAngle = atan2(
                            (point.y - center.y).toDouble(),
                            (point.x - center.x).toDouble()
                        )
                        previousTime = SystemClock.uptimeMillis()
                        angularVelocity = 0f
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width * 0.5f, size.height * 0.53f)
                        val angle = atan2(
                            (change.position.y - center.y).toDouble(),
                            (change.position.x - center.x).toDouble()
                        )
                        var delta = angle - previousAngle
                        while (delta > PI) delta -= 2.0 * PI
                        while (delta < -PI) delta += 2.0 * PI
                        val now = SystemClock.uptimeMillis()
                        val elapsed = (now - previousTime).coerceAtLeast(1L)
                        val degrees = Math.toDegrees(delta).toFloat()
                        rotation += degrees
                        angularVelocity = degrees / elapsed.toFloat() * 1000f
                        previousAngle = angle
                        previousTime = now
                        change.consume()
                    },
                    onDragEnd = {
                        val startAngle = rotation
                        val initialVelocity = angularVelocity
                        flingJob = scope.launch {
                            var elapsedSeconds = 0f
                            while (elapsedSeconds < 5f &&
                                abs(initialVelocity * exp(-1.15f * elapsedSeconds)) > 0.35f
                            ) {
                                delay(16)
                                elapsedSeconds += 0.016f
                                rotation = startAngle +
                                    (initialVelocity / 1.15f) * (1f - exp(-1.15f * elapsedSeconds))
                            }
                            val uprightTarget = (rotation / 360f).roundToInt() * 360f
                            val settle = Animatable(rotation)
                            settle.animateTo(
                                targetValue = uprightTarget,
                                animationSpec = spring(stiffness = 220f, dampingRatio = 0.742f)
                            ) { rotation = value }
                            rotation = uprightTarget
                        }
                    },
                    onDragCancel = { angularVelocity = 0f }
                )
            } else Modifier)
    ) {
        val path = Path().apply {
            moveTo(89f, 5f)
            cubicTo(97f, 0f, 106f, 4f, 110f, 13f)
            lineTo(177f, 128f)
            cubicTo(184f, 141f, 175f, 151f, 162f, 151f)
            cubicTo(153f, 151f, 147f, 146f, 142f, 137f)
            lineTo(93f, 51f)
            lineTo(43f, 139f)
            cubicTo(37f, 150f, 27f, 153f, 17f, 147f)
            cubicTo(7f, 141f, 5f, 131f, 11f, 120f)
            lineTo(77f, 13f)
            cubicTo(80f, 9f, 84f, 6f, 89f, 5f)
            close()
        }
        scale(size.width / 183f, size.height / 151f, pivot = Offset.Zero) {
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFF3ABD9), Color(0xFFF5BBC9), Color(0xFFF8D4B2))
                )
            )
            val cutout = Path().apply {
                moveTo(82f, 90f)
                lineTo(101f, 90f)
                lineTo(91.5f, 108f)
                close()
            }
            drawPath(cutout, Color.White)
        }
    }
}
@Composable
fun PixelArcHomeScreen(
    onOpenSearch: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenSettings: () -> Unit,
    tabCount: Int,
    onVoiceStart: () -> Unit = {},
    onVoiceEnd: () -> Unit = {},
    onSwipeTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        PixelArcAmbientBackground()
        PixelArcMark(Modifier.align(Alignment.Center), enabled = tabCount == 0)
        PixelBottomDock(
            tabCount = tabCount,
            homeMode = true,
            onOpenTabs = onOpenTabs,
            onNewTab = onOpenSearch,
            onOpenControls = onOpenSettings,
            onVoiceStart = onVoiceStart,
            onVoiceEnd = onVoiceEnd,
            onSwipeTab = onSwipeTab,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Three fixed controls aligned to the audited bottom-dock coordinates. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PixelBottomDock(
    tabCount: Int,
    homeMode: Boolean,
    onOpenTabs: () -> Unit,
    onNewTab: () -> Unit,
    onOpenControls: () -> Unit,
    onVoiceStart: () -> Unit = {},
    onVoiceEnd: () -> Unit = {},
    onSwipeTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dockSurface = if (homeMode) Color.Transparent else Color(0xFF2D2E32)
    var scrubDirection by androidx.compose.runtime.remember { mutableIntStateOf(1) }
    val leftCardTilt by animateFloatAsState(
        targetValue = if (scrubDirection > 0) -15f else 12f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.78f),
        label = "tab-mini-card-left-tilt"
    )
    val rightCardTilt by animateFloatAsState(
        targetValue = if (scrubDirection > 0) 12f else -15f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.78f),
        label = "tab-mini-card-right-tilt"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(dockSurface)
            .pointerInput(onSwipeTab) {
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        accumulated += amount
                        change.consume()
                        if (abs(accumulated) >= 72f) {
                            val direction = if (accumulated < 0f) 1 else -1
                            scrubDirection = direction
                            onSwipeTab(direction)
                            accumulated = 0f
                        }
                    },
                    onDragEnd = { accumulated = 0f },
                    onDragCancel = { accumulated = 0f }
                )
            }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Surface(
                onClick = onOpenTabs,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 45.dp)
                    .width(40.dp)
                    .height(64.dp),
                color = Color.Transparent,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (homeMode) {
                        Icon(
                            Icons.Rounded.Archive,
                            contentDescription = "Archive",
                            tint = Color.White,
                            modifier = Modifier.size(25.dp)
                        )
                    } else {
                        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            if (scrubDirection > 0) {
                                Surface(
                                    modifier = Modifier.offset(x = (-5).dp, y = 1.dp)
                                        .size(17.dp, 23.dp)
                                        .graphicsLayer { rotationZ = leftCardTilt }.zIndex(0f),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF777B8D),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f))
                                ) {}
                                Surface(
                                    modifier = Modifier.offset(x = 5.dp, y = (-1).dp)
                                        .size(17.dp, 23.dp)
                                        .graphicsLayer { rotationZ = rightCardTilt }.zIndex(1f),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF51596E),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.86f))
                                ) {}
                            } else {
                                Surface(
                                    modifier = Modifier.offset(x = 5.dp, y = (-1).dp)
                                        .size(17.dp, 23.dp)
                                        .graphicsLayer { rotationZ = rightCardTilt }.zIndex(0f),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF51596E),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.86f))
                                ) {}
                                Surface(
                                    modifier = Modifier.offset(x = (-5).dp, y = 1.dp)
                                        .size(17.dp, 23.dp)
                                        .graphicsLayer { rotationZ = leftCardTilt }.zIndex(1f),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF777B8D),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f))
                                ) {}
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(82.dp)
                    .height(46.dp),
                color = if (homeMode) Color(0xFF566076) else Color(0xFF4A4E59),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().pointerInput(onNewTab, onVoiceStart) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val released = withTimeoutOrNull(180L) { waitForUpOrCancellation() }
                            if (released == null) {
                                onVoiceStart()
                                waitForUpOrCancellation()
                                onVoiceEnd()
                            } else {
                                onNewTab()
                            }
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "New search. Hold for voice search",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Surface(
                onClick = onOpenControls,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 45.dp)
                    .width(40.dp)
                    .height(64.dp),
                color = Color.Transparent,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (homeMode) Icons.Rounded.Settings else Icons.Rounded.ExpandMore,
                        contentDescription = if (homeMode) "App settings" else "Page controls",
                        tint = Color.White,
                        modifier = Modifier
                            .size(if (homeMode) 20.dp else 17.dp)
                            .then(if (homeMode) Modifier else Modifier.rotate(180f))
                    )
                }
            }
        }
    }
}

/** Animated two-mode switch shared by the AI answer and web-result surfaces. */
@Composable
fun ArcAiWebTogglePill(
    webMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWeb by androidx.compose.runtime.remember(webMode) {
        androidx.compose.runtime.mutableStateOf(webMode)
    }
    val coordinateScale = (LocalConfiguration.current.screenWidthDp / 462f).coerceIn(0.75f, 1.1f)
    val indicatorOffset by animateDpAsState(
        targetValue = if (selectedWeb) (48f * coordinateScale).dp else 0.dp,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.683f),
        label = "arc-ai-web-indicator"
    )
    val scope = rememberCoroutineScope()
    Surface(
        onClick = {
            selectedWeb = !selectedWeb
            scope.launch {
                delay(260)
                onToggle()
            }
        },
        modifier = modifier.width((107f * coordinateScale).dp).height((59f * coordinateScale).dp),
        shape = CircleShape,
        color = Color(0xFF3C404E)
    ) {
        Box(Modifier.fillMaxSize().padding((5f * coordinateScale).dp)) {
            Box(
                Modifier.offset(x = indicatorOffset).size((49f * coordinateScale).dp)
                    .background(Color(0xFF873EA7), CircleShape)
            )
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = "AI mode",
                tint = if (selectedWeb) Color(0xFF9DA1B2) else Color.White,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = (14f * coordinateScale).dp).size((20f * coordinateScale).dp)
            )
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Web mode",
                tint = if (selectedWeb) Color.White else Color(0xFF7E8294),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = (14f * coordinateScale).dp).size((20f * coordinateScale).dp)
            )
        }
    }
}
