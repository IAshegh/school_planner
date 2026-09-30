package com.iashegh.schoolplanner.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val depth: Int, val twinkle: Int, val phase: Float, val size: Float)

/**
 * Twinkling parallax stars. [streak] (0..1) stretches them into hyperspace streaks radiating from the centre.
 */
@Composable
fun StarField(streak: Float, modifier: Modifier = Modifier) {
    val stars = remember {
        val r = Random(42)
        List(110) {
            Star(r.nextFloat(), r.nextFloat(), 1 + r.nextInt(3), 4 + r.nextInt(10), r.nextFloat(), 0.6f + r.nextFloat() * 1.8f)
        }
    }
    val transition = rememberInfiniteTransition(label = "stars")
    // 0..1 over 45 s; integer multiples below keep the loop seamless.
    val t = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(45_000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    Canvas(modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val time = t.value
        stars.forEach { s ->
            val py = ((s.y + time * s.depth) % 1f) * size.height
            val px = s.x * size.width
            val alpha = 0.35f + 0.65f * (0.5f + 0.5f * sin((2 * PI * (time * s.twinkle + s.phase)).toFloat()))
            val color = Color.White.copy(alpha = alpha)
            if (streak > 0.02f) {
                val dx = px - cx
                val dy = py - cy
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                val len = streak * s.depth * 0.22f * size.height
                val ex = px - dx / dist * len
                val ey = py - dy / dist * len
                drawLine(color, Offset(ex, ey), Offset(px, py), strokeWidth = s.size * (1f + streak), cap = StrokeCap.Round)
            } else {
                drawCircle(color, radius = s.size * (0.6f + 0.25f * s.depth), center = Offset(px, py))
            }
        }
    }
}
