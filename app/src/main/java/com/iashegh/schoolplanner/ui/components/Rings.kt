package com.iashegh.schoolplanner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iashegh.schoolplanner.ui.theme.LocalSkin
import com.iashegh.schoolplanner.ui.theme.Skin

/** Progress ring that draws itself when it appears. In the Scribe skin it looks like an ink brush stroke. */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    size: Dp = 64.dp,
    track: Color = color.copy(alpha = 0.18f),
    content: @Composable () -> Unit = {},
) {
    val skin = LocalSkin.current
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) { anim.animateTo(progress.coerceIn(0f, 1f), tween(900)) }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.minDimension * if (skin == Skin.SCRIBE) 0.13f else 0.1f
            val inset = stroke / 2f + 1f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke * 0.5f))
            val sweep = 360f * anim.value
            drawArc(color, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (skin == Skin.SCRIBE && sweep > 10f) {
                // second, thinner, slightly offset pass gives the dry-brush look
                drawArc(
                    color.copy(alpha = 0.55f), -94f, sweep - 6f, false, Offset(inset + 3f, inset + 1f),
                    Size(arcSize.width - 6f, arcSize.height - 2f), style = Stroke(stroke * 0.35f, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}
