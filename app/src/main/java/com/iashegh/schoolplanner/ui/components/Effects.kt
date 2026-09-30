package com.iashegh.schoolplanner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.composed
import com.iashegh.schoolplanner.ui.theme.LocalSkin
import com.iashegh.schoolplanner.ui.theme.Skin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Drops a list item in. Builder skin: bouncy spring (dampingRatio 0.6). Others: gentle slide + fade.
 * Only animates the first time the item enters composition.
 */
fun Modifier.enterAnim(index: Int): Modifier = composed {
    val skin = LocalSkin.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val delayMs = (index.coerceAtMost(8) * 60L)
        kotlinx.coroutines.delay(delayMs)
        if (skin == Skin.BUILDER) progress.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        else progress.animateTo(1f, tween(320))
    }
    graphicsLayer {
        val p = progress.value
        if (skin == Skin.BUILDER) {
            translationY = -(1f - p) * 220f
            alpha = (p * 4f).coerceIn(0f, 1f)
        } else {
            translationY = (1f - p) * 40f
            alpha = p.coerceIn(0f, 1f)
        }
    }
}

/** Confetti / sparkle burst. Fires every time [trigger] changes to a new positive value. */
@Composable
fun CelebrationBurst(trigger: Int, modifier: Modifier = Modifier) {
    val skin = LocalSkin.current
    val progress = remember { Animatable(1f) }
    var seed by remember { mutableIntStateOf(0) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            seed = trigger
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1500, easing = LinearEasing))
        }
    }
    val p = progress.value
    if (p >= 1f) return
    val colors = when (skin) {
        Skin.GALACTIC -> listOf(Color(0xFF4FC3F7), Color(0xFFB388FF), Color(0xFFFFFFFF), Color(0xFFFFEB3B))
        Skin.BUILDER -> listOf(Color(0xFFE53935), Color(0xFFFFC107), Color(0xFF1E88E5), Color(0xFF43A047))
        Skin.SCRIBE -> listOf(Color(0xFF26231F), Color(0xFF8C2F39), Color(0xFF6B655B))
    }
    Canvas(modifier.fillMaxSize()) {
        val rnd = Random(seed)
        val ox = size.width / 2f
        val oy = size.height * 0.62f
        repeat(70) {
            val angle = rnd.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 0.25f + rnd.nextFloat() * 0.6f
            val color = colors[rnd.nextInt(colors.size)]
            val sz = 6f + rnd.nextFloat() * 12f
            val x = ox + cos(angle) * speed * p * size.width * 0.7f
            val y = oy + sin(angle) * speed * p * size.height * 0.5f + p * p * size.height * 0.35f
            val a = (1f - p * p).coerceIn(0f, 1f)
            when (skin) {
                Skin.BUILDER -> drawRect(color.copy(alpha = a), Offset(x, y), Size(sz * 1.4f, sz))
                Skin.SCRIBE -> drawCircle(color.copy(alpha = a), sz * 0.8f, Offset(x, y), style = Stroke(width = 3f))
                Skin.GALACTIC -> drawCircle(color.copy(alpha = a), sz * 0.6f, Offset(x, y))
            }
        }
    }
}
