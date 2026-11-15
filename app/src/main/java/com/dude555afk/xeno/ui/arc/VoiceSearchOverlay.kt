package com.dude555afk.xeno.ui.arc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val VOICE_CANVAS_WIDTH = 462f
private const val VOICE_CANVAS_HEIGHT = 1000f

/**
 * Arc-style voice surface: a two-circle dome grows out of the bottom '+' action,
 * while the live speech level drives the exposed outer halo.
 */
@Composable
fun VoiceSearchOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
    languageMode: String = "infer"
) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("Hi! How can I help?") }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var isListening by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnResult by rememberUpdatedState(onResult)
    val domeScale = remember { Animatable(0.1f) }
    val eyeBlink = remember { Animatable(1f) }

    val startRecognition: () -> Unit = remember(context) {
        {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                transcript = "No speech recognition service is available"
                isListening = false
            } else {
                runCatching {
                    recognizer?.cancel()
                    recognizer?.destroy()

                    val speech = SpeechRecognizer.createSpeechRecognizer(context)
                    recognizer = speech
                    isListening = true

                    speech.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            // Keep the designed prompt until actual speech arrives.
                        }

                        override fun onBeginningOfSpeech() {
                            isListening = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            amplitude = ((rmsdB + 2f) / 14f).coerceIn(0f, 1f)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) = Unit

                        override fun onEndOfSpeech() {
                            amplitude = 0f
                        }

                        override fun onError(error: Int) {
                            amplitude = 0f
                            isListening = false
                            if (error != SpeechRecognizer.ERROR_CLIENT) {
                                transcript = "Didn't catch that. Try again."
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            amplitude = 0f
                            isListening = false
                            val spoken = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                            if (spoken.isNotBlank()) {
                                transcript = spoken
                                latestOnResult(spoken)
                                latestOnDismiss()
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                            if (!partial.isNullOrBlank()) transcript = partial
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    })

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        val languageTag = when (languageMode) {
                            "english" -> java.util.Locale.ENGLISH.toLanguageTag()
                            "system" -> java.util.Locale.getDefault().toLanguageTag()
                            else -> null
                        }
                        if (languageTag != null) putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                    }
                    speech.startListening(intent)
                }.onFailure {
                    isListening = false
                    amplitude = 0f
                    transcript = "Voice search isn't available"
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecognition() else latestOnDismiss()
    }

    LaunchedEffect(visible) {
        if (visible) {
            transcript = "Hi! How can I help?"
            amplitude = 0f
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) startRecognition()
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

            domeScale.snapTo(0.1f)
            domeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f)
            )
        } else {
            isListening = false
            amplitude = 0f
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
            eyeBlink.snapTo(1f)
            domeScale.animateTo(0.1f, animationSpec = tween(150))
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            while (true) {
                delay(3200)
                eyeBlink.animateTo(0.10f, animationSpec = tween(80))
                eyeBlink.animateTo(1f, animationSpec = tween(80))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(170))
    ) {
        val motion = rememberInfiniteTransition(label = "voice-dome-motion")
        val idlePhase by motion.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "voice-dome-idle-wobble"
        )
        val faceTilt by motion.animateFloat(
            initialValue = -4.5f,
            targetValue = 4.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "voice-smiley-rock"
        )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val unit = maxWidth / VOICE_CANVAS_WIDTH

            // Only the live page region is dimmed. The status-bar area remains clear.
            Box(
                Modifier
                    .offset(y = unit * 43f)
                    .fillMaxWidth()
                    .height(unit * 737f)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0x8C161326),
                                Color(0x99532025)
                            )
                        )
                    )
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = size.width / VOICE_CANVAS_WIDTH
                val pivot = Offset(231f * s, 955.5f * s)
                val phase = idlePhase

                scale(
                    scale = domeScale.value,
                    pivot = pivot
                ) {
                    drawCircle(
                        color = Color(0xFF6B74DD),
                        radius = (279.1f + amplitude * 35.9f) * s,
                        center = Offset(
                            (231.1f + 8f * sin(phase)) * s,
                            (930.1f + 6f * cos(phase * 2f) - 14f * amplitude) * s
                        )
                    )
                    drawCircle(
                        color = Color(0xFF52377E),
                        radius = 295.4f * s,
                        center = Offset(233.6f * s, 965.8f * s)
                    )
                }

                val faceColor = Color(0xFFA99BBE)
                rotate(
                    degrees = faceTilt,
                    pivot = Offset(239.5f * s, 726f * s)
                ) {
                    val blink = eyeBlink.value.coerceIn(0.10f, 1f)
                    val leftEyeHeight = 38f * blink * s
                    val rightEyeHeight = 37f * blink * s

                    drawRoundRect(
                        color = faceColor,
                        topLeft = Offset(
                            214f * s,
                            (692f + (38f - 38f * blink) / 2f) * s
                        ),
                        size = Size(14f * s, leftEyeHeight),
                        cornerRadius = CornerRadius(5.5f * s)
                    )
                    drawRoundRect(
                        color = faceColor,
                        topLeft = Offset(
                            245f * s,
                            (690f + (37f - 37f * blink) / 2f) * s
                        ),
                        size = Size(14f * s, rightEyeHeight),
                        cornerRadius = CornerRadius(5.5f * s)
                    )

                    val smile = Path().apply {
                        moveTo(210f * s, 749f * s)
                        quadraticTo(241f * s, 764f * s, 269f * s, 744f * s)
                    }
                    drawPath(
                        path = smile,
                        color = faceColor,
                        style = Stroke(width = 11f * s, cap = StrokeCap.Round)
                    )
                }
            }

            // The transcript occupies the audited 167px-wide line below the face.
            Box(
                modifier = Modifier
                    .offset(x = unit * 148f, y = unit * 850f)
                    .size(width = unit * 167f, height = unit * 33f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transcript,
                    color = Color(0xFFD2C1E3),
                    fontSize = (19.5f * unit.value).sp,
                    lineHeight = (22f * unit.value).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // This replaces the '+' capsule in-place, completing the dome-to-pill morph.
            Box(
                modifier = Modifier
                    .offset(x = unit * 183f, y = unit * 929f)
                    .size(width = unit * 97f, height = unit * 54f)
                    .clip(RoundedCornerShape(unit * 27f))
                    .background(Color(0xFF6F5793))
                    .clickable {
                        if (isListening) {
                            recognizer?.cancel()
                            isListening = false
                            amplitude = 0f
                            transcript = "Microphone paused"
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) startRecognition()
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = if (isListening) "Pause microphone" else "Resume microphone",
                    tint = Color.White,
                    modifier = Modifier.size(width = unit * 17f, height = unit * 24f)
                )
            }

            Box(
                modifier = Modifier
                    .offset(x = unit * 199f, y = unit * 989f)
                    .size(width = unit * 64f, height = unit * 4f)
                    .clip(CircleShape)
                    .background(Color(0xFF8975A8))
            )
        }
    }
}
