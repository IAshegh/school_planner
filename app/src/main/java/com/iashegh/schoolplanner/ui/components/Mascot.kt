package com.iashegh.schoolplanner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iashegh.schoolplanner.ui.theme.LocalSkin
import com.iashegh.schoolplanner.ui.theme.Skin
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * The companion in the top bar. Droid / brick-guy / ink-ring depending on the skin.
 * Wiggles on tap and whenever [cheer] changes; looks wide-eyed and blinks its antenna when [alert].
 */
@Composable
fun Mascot(cheer: Int, alert: Boolean, size: Dp = 44.dp, onTap: () -> Unit = {}) {
    val skin = LocalSkin.current
    val wiggle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    suspend fun doWiggle() {
        wiggle.snapTo(0f)
        wiggle.animateTo(1f, tween(700, easing = LinearEasing))
    }
    LaunchedEffect(cheer) { if (cheer > 0) doWiggle() }

    val pulse = rememberInfiniteTransition(label = "mascot").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse",
    )

    Canvas(
        Modifier
            .size(size)
            .graphicsLayer {
                val w = wiggle.value
                rotationZ = sin(w * 6f * Math.PI.toFloat()) * 14f * (1f - w)
                scaleX = 1f + 0.12f * sin(w * Math.PI.toFloat())
                scaleY = scaleX
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onTap()
                scope.launch { doWiggle() }
            },
    ) {
        val w = this.size.width
        val h = this.size.height
        val eyeScale = if (alert) 1.25f + 0.1f * pulse.value else 1f
        when (skin) {
            Skin.GALACTIC -> {
                val body = Color(0xFFDDE3F0)
                // antenna
                drawLine(body, Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.06f), w * 0.05f, StrokeCap.Round)
                drawCircle(if (alert) Color(0xFFFF5252).copy(alpha = 0.4f + 0.6f * pulse.value) else Color(0xFF4FC3F7), w * 0.06f, Offset(w * 0.5f, h * 0.06f))
                // dome head
                drawRoundRect(body, Offset(w * 0.12f, h * 0.22f), Size(w * 0.76f, h * 0.68f), CornerRadius(w * 0.32f))
                // visor + eye
                drawRoundRect(Color(0xFF10163A), Offset(w * 0.22f, h * 0.36f), Size(w * 0.56f, h * 0.34f), CornerRadius(w * 0.17f))
                drawCircle(Color(0xFF4FC3F7), w * 0.11f * eyeScale, Offset(w * 0.5f, h * 0.53f))
                drawCircle(Color.White, w * 0.04f * eyeScale, Offset(w * 0.53f, h * 0.5f))
            }
            Skin.BUILDER -> {
                val body = Color(0xFFFFC107)
                drawRoundRect(body, Offset(w * 0.28f, h * 0.05f), Size(w * 0.16f, h * 0.14f), CornerRadius(w * 0.03f))
                drawRoundRect(body, Offset(w * 0.56f, h * 0.05f), Size(w * 0.16f, h * 0.14f), CornerRadius(w * 0.03f))
                drawRoundRect(body, Offset(w * 0.12f, h * 0.17f), Size(w * 0.76f, h * 0.74f), CornerRadius(w * 0.1f))
                drawCircle(Color(0xFF212121), w * 0.06f * eyeScale, Offset(w * 0.35f, h * 0.45f))
                drawCircle(Color(0xFF212121), w * 0.06f * eyeScale, Offset(w * 0.65f, h * 0.45f))
                drawArc(
                    Color(0xFF212121), 20f, 140f, false, Offset(w * 0.32f, h * 0.5f), Size(w * 0.36f, h * 0.26f),
                    style = Stroke(w * 0.055f, cap = StrokeCap.Round),
                )
            }
            Skin.SCRIBE -> {
                val ink = Color(0xFF26231F)
                drawArc(
                    ink, -40f, 320f, false, Offset(w * 0.1f, h * 0.1f), Size(w * 0.8f, h * 0.8f),
                    style = Stroke(w * 0.11f, cap = StrokeCap.Round),
                )
                drawCircle(Color(0xFF8C2F39), w * 0.1f * eyeScale, Offset(w * 0.5f, h * 0.5f))
                drawCircle(ink, w * 0.035f, Offset(w * 0.5f, h * 0.5f))
            }
        }
    }
}
