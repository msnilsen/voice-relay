package com.openclaw.assistant.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Animated orb overlay for Voice Talk Mode.
 * Visualizes the assistant's state and audio levels.
 */
@Composable
fun TalkOrbOverlay(
    state: TalkOrbState,
    audioLevel: Float = 0f, // 0..1
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val baseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "base_scale"
    )

    val audioScale by animateFloatAsState(
        targetValue = 1f + (audioLevel * 0.4f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "audio_scale"
    )

    val finalScale = when (state) {
        TalkOrbState.LISTENING -> baseScale * audioScale
        TalkOrbState.SPEAKING -> baseScale * audioScale
        TalkOrbState.THINKING -> baseScale * 1.1f
        else -> 1f
    }

    val orbColor = when (state) {
        TalkOrbState.LISTENING -> Color(0xFF4CAF50)
        TalkOrbState.THINKING -> Color(0xFFFFC107)
        TalkOrbState.SPEAKING -> Color(0xFF2196F3)
        TalkOrbState.ERROR -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = finalScale * 1.5f
                    scaleY = finalScale * 1.5f
                    alpha = 0.3f
                }
                .blur(40.dp)
                .background(orbColor, CircleShape)
        )

        // Main orb
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = finalScale
                    scaleY = finalScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            orbColor.copy(alpha = 0.9f),
                            orbColor.copy(alpha = 0.6f)
                        ),
                        center = Offset.Zero,
                        radius = 300f
                    )
                )
        ) {
            // Internal animation for "Thinking" state
            if (state == TalkOrbState.THINKING) {
                val rotateTransition = rememberInfiniteTransition(label = "orb_rotate")
                val rotation by rotateTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        radius = size.minDimension / 4,
                        center = Offset(size.width / 2, size.height / 4)
                    )
                }
            }
        }
    }
}

enum class TalkOrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}
