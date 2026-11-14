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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.sin

/**
 * Long-press voice surface. Android's speech service supplies partial results;
 * Xeno does not record or persist microphone audio.
 */
@Composable
fun VoiceSearchOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("Hi! How can I help?") }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val startRecognition = remember(context) {
        {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                runCatching {
                    recognizer?.destroy()
                    val speech = SpeechRecognizer.createSpeechRecognizer(context)
                    speech.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            transcript = "Listening…"
                        }

                        override fun onBeginningOfSpeech() {
                            transcript = "I'm listening"
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
                            if (transcript == "Listening…" || transcript == "I'm listening") {
                                transcript = "Didn't catch that. Try again."
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            amplitude = 0f
                            val spoken = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                            if (spoken.isNotBlank()) {
                                transcript = spoken
                                onResult(spoken)
                                onDismiss()
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
                    recognizer = speech
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                    }
                    speech.startListening(intent)
                }.onFailure {
                    transcript = "Voice search isn't available"
                }
            } else {
                transcript = "No speech recognition service is available"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecognition() else onDismiss()
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            transcript = "Hi! How can I help?"
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) startRecognition()
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            recognizer?.stopListening()
            amplitude = 0f
        }
    }

    val motion = rememberInfiniteTransition(label = "voice-face")
    val facePulse by motion.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "voice-face-pulse"
    )
    val wave by motion.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "voice-wave"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.94f),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.96f)
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFF34215E), Color(0xFF8E3F8D), Color(0xFFE58BAA))
                )
            )
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 38.dp, end = 16.dp).size(48.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close voice search", tint = Color.White)
            }

            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp).navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(106.dp))
                Text(
                    "☺",
                    color = Color.White,
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.size(116.dp).graphicsLayer {
                        scaleX = facePulse
                        scaleY = facePulse
                    },
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(36.dp))
                Text(
                    transcript,
                    color = Color.White,
                    fontSize = 25.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp).height(72.dp),
                    shape = RoundedCornerShape(36.dp),
                    color = Color(0x55302A48),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(8) { index ->
                            val level = abs(sin(wave + index * 0.72f)).toFloat()
                            val height = (9f + level * (11f + amplitude * 28f) + amplitude * 8f).coerceIn(7f, 48f)
                            Box(
                                Modifier.padding(horizontal = 4.dp).width(4.dp).height(height.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Icon(Icons.Rounded.Mic, contentDescription = "Listening", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
