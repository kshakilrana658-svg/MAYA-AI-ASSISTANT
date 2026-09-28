package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.MayaState
import com.example.ui.theme.MayaCyan
import com.example.ui.theme.MayaError
import com.example.ui.theme.MayaPink
import com.example.ui.theme.MayaPurple
import com.example.ui.theme.MayaSuccess
import com.example.ui.theme.MayaWarning
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MayaAvatar(
    state: MayaState,
    audioRms: Float = 0.2f,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MayaAvatarPulse")

    // Breathing pulse for sleeping / calm states
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // Fast rotation for thinking / working states
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == MayaState.THINKING) 3500 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    // Waveform phase animation
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f * 0.85f

            val primaryColor = when (state) {
                MayaState.SLEEPING -> MayaCyan.copy(alpha = 0.7f)
                MayaState.LISTENING -> MayaCyan
                MayaState.UNDERSTANDING -> MayaPurple
                MayaState.VERIFYING -> MayaCyan
                MayaState.THINKING -> MayaPurple
                MayaState.WORKING -> MayaCyan
                MayaState.RESPONDING -> MayaPink
                MayaState.SUCCESS -> MayaSuccess
                MayaState.ERROR -> MayaError
                MayaState.PAUSED -> MayaWarning
                MayaState.STOPPED -> MayaWarning.copy(alpha = 0.6f)
            }

            val secondaryColor = when (state) {
                MayaState.SLEEPING -> MayaPurple.copy(alpha = 0.5f)
                MayaState.LISTENING -> MayaPink
                MayaState.UNDERSTANDING -> MayaCyan
                MayaState.VERIFYING -> MayaPurple
                MayaState.THINKING -> MayaPink
                MayaState.WORKING -> MayaPurple
                MayaState.RESPONDING -> MayaCyan
                MayaState.SUCCESS -> Color(0xFF69F0AE)
                MayaState.ERROR -> Color(0xFFFF8A80)
                MayaState.PAUSED -> MayaCyan
                MayaState.STOPPED -> Color(0xFF90A4AE)
            }

            // 1. Draw Outer Deep Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.25f * breathScale),
                        secondaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.35f
                ),
                radius = baseRadius * 1.35f,
                center = center
            )

            // 2. Draw Orbital Tech Rings
            rotate(rotationAngle, pivot = center) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f),
                    radius = baseRadius * 1.05f,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Arc accents
                drawArc(
                    color = primaryColor,
                    startAngle = 0f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 1.05f, center.y - baseRadius * 1.05f),
                    size = Size(baseRadius * 2.1f, baseRadius * 2.1f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                drawArc(
                    color = secondaryColor,
                    startAngle = 180f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 1.05f, center.y - baseRadius * 1.05f),
                    size = Size(baseRadius * 2.1f, baseRadius * 2.1f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Counter-rotating inner ring
            rotate(-rotationAngle * 0.7f, pivot = center) {
                drawArc(
                    color = secondaryColor.copy(alpha = 0.6f),
                    startAngle = 90f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.94f, center.y - baseRadius * 0.94f),
                    size = Size(baseRadius * 1.88f, baseRadius * 1.88f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Inner Holographic Core Orb
            val currentCoreRadius = baseRadius * 0.82f * (if (state == MayaState.SLEEPING) breathScale else 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF16234F),
                        Color(0xFF0F1738),
                        Color(0xFF090D24)
                    ),
                    center = center,
                    radius = currentCoreRadius
                ),
                radius = currentCoreRadius,
                center = center
            )

            // Inner rim glow
            drawCircle(
                color = primaryColor.copy(alpha = 0.5f),
                radius = currentCoreRadius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 4. Stylized Futuristic Maya Silhouette / Hologram Avatar
            drawMayaAvatarSilhouette(
                center = center,
                radius = currentCoreRadius,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor
            )

            // 5. State Specific Visuals
            when (state) {
                MayaState.LISTENING -> {
                    // Draw dynamic acoustic voice waveform around core
                    val barCount = 28
                    val effectiveAmp = (audioRms.coerceIn(0.1f, 1.0f))
                    for (i in 0 until barCount) {
                        val angle = (i.toFloat() / barCount) * 2f * PI.toFloat() + wavePhase
                        val waveOffset = (sin(angle * 3f) * 0.5f + 0.5f) * effectiveAmp
                        val barHeight = 6.dp.toPx() + waveOffset * 28.dp.toPx()
                        val startRad = currentCoreRadius + 4.dp.toPx()
                        val endRad = startRad + barHeight

                        val startX = center.x + cos(angle) * startRad
                        val startY = center.y + sin(angle) * startRad
                        val endX = center.x + cos(angle) * endRad
                        val endY = center.y + sin(angle) * endRad

                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(MayaCyan, MayaPink),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY)
                            ),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                MayaState.VERIFYING -> {
                    // Biometric scanning reticle & grid
                    val scanY = center.y + sin(wavePhase * 2f) * (currentCoreRadius * 0.6f)
                    drawLine(
                        color = MayaCyan,
                        start = Offset(center.x - currentCoreRadius * 0.65f, scanY),
                        end = Offset(center.x + currentCoreRadius * 0.65f, scanY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Reticle brackets
                    drawReticleBrackets(center, currentCoreRadius * 0.6f, MayaCyan)
                }

                MayaState.THINKING -> {
                    // Orbiting neural sparks
                    for (i in 0 until 6) {
                        val sparkAngle = (i * 60f + rotationAngle * 1.5f) * (PI / 180f).toFloat()
                        val sparkX = center.x + cos(sparkAngle) * (currentCoreRadius * 0.72f)
                        val sparkY = center.y + sin(sparkAngle) * (currentCoreRadius * 0.72f)
                        drawCircle(
                            color = if (i % 2 == 0) MayaCyan else MayaPink,
                            radius = 4.dp.toPx(),
                            center = Offset(sparkX, sparkY)
                        )
                    }
                }

                MayaState.WORKING -> {
                    // Pulsing working data ring
                    drawCircle(
                        color = MayaCyan.copy(alpha = 0.4f),
                        radius = currentCoreRadius * 0.5f,
                        center = center,
                        style = Stroke(
                            width = 3.dp.toPx()
                        )
                    )
                }

                else -> {}
            }
        }
    }
}

/**
 * Draws an elegant cybernetic female AI avatar silhouette inside the core orb.
 */
private fun DrawScope.drawMayaAvatarSilhouette(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val scale = radius / 100f

    // Soft celestial background hair aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                secondaryColor.copy(alpha = 0.35f),
                Color.Transparent
            ),
            center = Offset(center.x, center.y - 10f * scale),
            radius = 65f * scale
        ),
        radius = 65f * scale,
        center = Offset(center.x, center.y - 10f * scale)
    )

    // Flowing Hair Silhouette (Left & Right locks)
    val hairPath = Path().apply {
        moveTo(center.x, center.y - 48f * scale)
        cubicTo(
            center.x - 42f * scale, center.y - 42f * scale,
            center.x - 48f * scale, center.y + 10f * scale,
            center.x - 36f * scale, center.y + 45f * scale
        )
        cubicTo(
            center.x - 24f * scale, center.y + 35f * scale,
            center.x - 28f * scale, center.y + 15f * scale,
            center.x - 22f * scale, center.y - 10f * scale
        )
        lineTo(center.x, center.y - 48f * scale)
    }
    drawPath(
        path = hairPath,
        brush = Brush.verticalGradient(
            colors = listOf(secondaryColor.copy(alpha = 0.8f), primaryColor.copy(alpha = 0.4f)),
            startY = center.y - 48f * scale,
            endY = center.y + 45f * scale
        )
    )

    // Symmetric Right Hair
    val rightHairPath = Path().apply {
        moveTo(center.x, center.y - 48f * scale)
        cubicTo(
            center.x + 42f * scale, center.y - 42f * scale,
            center.x + 48f * scale, center.y + 10f * scale,
            center.x + 36f * scale, center.y + 45f * scale
        )
        cubicTo(
            center.x + 24f * scale, center.y + 35f * scale,
            center.x + 28f * scale, center.y + 15f * scale,
            center.x + 22f * scale, center.y - 10f * scale
        )
        lineTo(center.x, center.y - 48f * scale)
    }
    drawPath(
        path = rightHairPath,
        brush = Brush.verticalGradient(
            colors = listOf(secondaryColor.copy(alpha = 0.8f), primaryColor.copy(alpha = 0.4f)),
            startY = center.y - 48f * scale,
            endY = center.y + 45f * scale
        )
    )

    // Face Silhouette
    val facePath = Path().apply {
        moveTo(center.x - 18f * scale, center.y - 12f * scale)
        cubicTo(
            center.x - 18f * scale, center.y + 16f * scale,
            center.x - 10f * scale, center.y + 32f * scale,
            center.x, center.y + 36f * scale
        )
        cubicTo(
            center.x + 10f * scale, center.y + 32f * scale,
            center.x + 18f * scale, center.y + 16f * scale,
            center.x + 18f * scale, center.y - 12f * scale
        )
        close()
    }
    drawPath(
        path = facePath,
        color = Color(0xFF1E2856)
    )

    // Cybernetic Circlet / Headset Diadem
    drawLine(
        color = primaryColor,
        start = Offset(center.x - 22f * scale, center.y - 20f * scale),
        end = Offset(center.x + 22f * scale, center.y - 20f * scale),
        strokeWidth = 2.5f * scale,
        cap = StrokeCap.Round
    )
    // Center forehead diamond node
    drawCircle(
        color = primaryColor,
        radius = 3.5f * scale,
        center = Offset(center.x, center.y - 20f * scale)
    )

    // Glowing Eyes
    drawCircle(
        color = primaryColor,
        radius = 2.8f * scale,
        center = Offset(center.x - 7.5f * scale, center.y - 2f * scale)
    )
    drawCircle(
        color = primaryColor,
        radius = 2.8f * scale,
        center = Offset(center.x + 7.5f * scale, center.y - 2f * scale)
    )

    // Shoulders / Cyber collar
    val collarPath = Path().apply {
        moveTo(center.x - 28f * scale, center.y + 55f * scale)
        lineTo(center.x - 10f * scale, center.y + 42f * scale)
        lineTo(center.x + 10f * scale, center.y + 42f * scale)
        lineTo(center.x + 28f * scale, center.y + 55f * scale)
    }
    drawPath(
        path = collarPath,
        color = secondaryColor.copy(alpha = 0.7f),
        style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawReticleBrackets(center: Offset, halfSize: Float, color: Color) {
    val bracketLen = halfSize * 0.35f
    val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)

    // Top-Left
    val p1 = Path().apply {
        moveTo(center.x - halfSize, center.y - halfSize + bracketLen)
        lineTo(center.x - halfSize, center.y - halfSize)
        lineTo(center.x - halfSize + bracketLen, center.y - halfSize)
    }
    drawPath(p1, color, style = stroke)

    // Top-Right
    val p2 = Path().apply {
        moveTo(center.x + halfSize - bracketLen, center.y - halfSize)
        lineTo(center.x + halfSize, center.y - halfSize)
        lineTo(center.x + halfSize, center.y - halfSize + bracketLen)
    }
    drawPath(p2, color, style = stroke)

    // Bottom-Left
    val p3 = Path().apply {
        moveTo(center.x - halfSize, center.y + halfSize - bracketLen)
        lineTo(center.x - halfSize, center.y + halfSize)
        lineTo(center.x - halfSize + bracketLen, center.y + halfSize)
    }
    drawPath(p3, color, style = stroke)

    // Bottom-Right
    val p4 = Path().apply {
        moveTo(center.x + halfSize - bracketLen, center.y + halfSize)
        lineTo(center.x + halfSize, center.y + halfSize)
        lineTo(center.x + halfSize, center.y + halfSize - bracketLen)
    }
    drawPath(p4, color, style = stroke)
}
