package com.iashegh.schoolplanner.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.iashegh.schoolplanner.ui.theme.LocalSkin
import com.iashegh.schoolplanner.ui.theme.LocalSkinColors
import com.iashegh.schoolplanner.ui.theme.Skin

private val BrickColors = listOf(Color(0xFFE53935), Color(0xFFFFC107), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00))

/** Weekly accomplishment tracker: each finished homework adds a piece to the stack. */
@Composable
fun RewardTower(count: Int, modifier: Modifier = Modifier) {
    val skin = LocalSkin.current
    val colors = LocalSkinColors.current
    val shown = count.coerceAtMost(14)
    val grow by animateFloatAsState(
        shown.toFloat(), spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow), label = "tower",
    )
    val title = when (skin) {
        Skin.BUILDER -> "My tower"
        Skin.GALACTIC -> "Energy cells"
        Skin.SCRIBE -> "Ink rings"
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(72.dp).height(96.dp)) {
            val pieceH = this.size.height / 14f
            val w = this.size.width
            val full = grow.toInt()
            val frac = grow - full
            for (i in 0..full) {
                val f = if (i == full) frac else 1f
                if (f <= 0.01f) continue
                val y = this.size.height - (i + 1) * pieceH
                val offsetX = if (i % 2 == 0) 0f else w * 0.06f
                val c = BrickColors[i % BrickColors.size]
                when (skin) {
                    Skin.BUILDER -> {
                        drawRoundRect(c.copy(alpha = f), Offset(offsetX, y), Size(w * 0.88f, pieceH * 0.92f), CornerRadius(3f))
                        drawRoundRect(c.copy(alpha = f), Offset(offsetX + w * 0.15f, y - pieceH * 0.16f), Size(w * 0.18f, pieceH * 0.2f), CornerRadius(2f))
                        drawRoundRect(c.copy(alpha = f), Offset(offsetX + w * 0.5f, y - pieceH * 0.16f), Size(w * 0.18f, pieceH * 0.2f), CornerRadius(2f))
                    }
                    Skin.GALACTIC -> {
                        val cc = if (i % 2 == 0) colors.primary else colors.accent
                        drawRoundRect(cc.copy(alpha = 0.25f * f), Offset(4f, y), Size(w - 8f, pieceH * 0.9f), CornerRadius(8f))
                        drawRoundRect(cc.copy(alpha = f), Offset(4f, y), Size(w - 8f, pieceH * 0.9f), CornerRadius(8f), style = Stroke(3f))
                    }
                    Skin.SCRIBE -> {
                        drawOval(colors.primary.copy(alpha = f), Offset(w * 0.1f, y), Size(w * 0.8f, pieceH * 0.9f), style = Stroke(4f))
                    }
                }
            }
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                if (count == 0) "Finish homework to build it up!" else "$count done this week",
                style = MaterialTheme.typography.bodyMedium, color = colors.muted,
            )
        }
    }
}
