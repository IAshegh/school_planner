package com.iashegh.schoolplanner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iashegh.schoolplanner.data.Subject
import com.iashegh.schoolplanner.data.SubjectIcons
import com.iashegh.schoolplanner.ui.theme.LocalSkin
import com.iashegh.schoolplanner.ui.theme.LocalSkinColors
import com.iashegh.schoolplanner.ui.theme.Skin
import com.iashegh.schoolplanner.ui.theme.cardShape
import com.iashegh.schoolplanner.ui.theme.readableOn
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun mutedColor(): Color = LocalContentColor.current.copy(alpha = 0.72f)

/**
 * The basic card of the app, dressed per skin:
 *  - Galactic: dark panel with a glowing neon outline
 *  - Builder: solid toy brick in [accent] with studs on top
 *  - Scribe: matte paper with a thin ink outline
 */
@Composable
fun SkinCard(
    modifier: Modifier = Modifier,
    accent: Color = LocalSkinColors.current.primary,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val skin = LocalSkin.current
    val colors = LocalSkinColors.current
    val shape = skin.cardShape()
    val fill = if (skin == Skin.BUILDER) accent else colors.surface
    val textColor = if (skin == Skin.BUILDER) readableOn(accent) else colors.onSurface

    var outer = modifier
    if (skin == Skin.BUILDER) {
        outer = outer
            .drawBehind {
                // studs sit in the padding zone above the brick
                val stud = 7.dp.toPx()
                val gap = 22.dp.toPx()
                var x = 18.dp.toPx()
                while (x < size.width - 14.dp.toPx()) {
                    drawRoundRect(accent.copy(alpha = 0.9f), Offset(x, 0f), Size(stud * 1.8f, stud * 1.3f), CornerRadius(stud * 0.5f))
                    x += gap * 1.8f
                }
            }
            .padding(top = 8.dp)
    } else if (skin == Skin.GALACTIC) {
        outer = outer.drawBehind {
            drawRoundRect(accent.copy(alpha = 0.22f), style = Stroke(width = 8.dp.toPx()), cornerRadius = CornerRadius(18.dp.toPx()))
        }
    }

    val border = when (skin) {
        Skin.GALACTIC -> BorderStroke(1.5.dp, accent)
        Skin.BUILDER -> BorderStroke(2.dp, Color.Black.copy(alpha = 0.35f))
        Skin.SCRIBE -> BorderStroke(1.2.dp, colors.border)
    }
    Surface(
        modifier = outer,
        shape = shape,
        color = fill,
        contentColor = textColor,
        border = border,
    ) {
        Box(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) { content() }
    }
}

/** Round subject badge. In Decoder Mode it becomes a circular alien glyph. */
@Composable
fun SubjectBadge(subject: Subject, size: Dp = 48.dp, decoder: Boolean = false) {
    val c = Color(subject.color)
    if (decoder) {
        GlyphBadge(subject.name, c, size)
        return
    }
    Box(
        Modifier.size(size).clip(CircleShape).background(c),
        contentAlignment = Alignment.Center,
    ) {
        Icon(SubjectIcons.get(subject.icon), contentDescription = null, tint = readableOn(c), modifier = Modifier.size(size * 0.58f))
    }
}

/** Deterministic circular glyph derived from [seed] – the "alien logogram" of a word. */
@Composable
fun GlyphBadge(seed: String, color: Color, size: Dp = 48.dp) {
    Canvas(Modifier.size(size)) { drawGlyph(seed.hashCode() + 17, color, this.size.minDimension) }
}

/** A word rendered as a row of tiny glyphs, one per letter. */
@Composable
fun GlyphText(text: String, color: Color, glyphSize: Dp = 20.dp) {
    Row {
        text.filter { it.isLetterOrDigit() }.take(12).forEach { ch ->
            Canvas(Modifier.padding(end = 2.dp).size(glyphSize)) { drawGlyph(ch.lowercaseChar().code * 31, color, this.size.minDimension) }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlyph(seed: Int, color: Color, d: Float) {
    val r = d / 2f
    val center = Offset(r, r)
    val stroke = d * 0.09f
    var h = seed
    fun next(): Int { h = h * 1103515245 + 12345; return (h ushr 8) and 0xFFFF }
    // outer broken ring
    val segments = 3 + next() % 3
    val gapDeg = 18f
    val segSweep = 360f / segments
    val start0 = (next() % 360).toFloat()
    for (i in 0 until segments) {
        drawArc(
            color, startAngle = start0 + i * segSweep + gapDeg / 2, sweepAngle = segSweep - gapDeg, useCenter = false,
            topLeft = Offset(stroke, stroke), size = Size(d - 2 * stroke, d - 2 * stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
    // inner dots / bars
    val dots = 1 + next() % 3
    for (i in 0 until dots) {
        val a = (next() % 360) * PI / 180.0
        val dist = r * (0.18f + (next() % 40) / 100f)
        val p = Offset(center.x + (cos(a) * dist).toFloat(), center.y + (sin(a) * dist).toFloat())
        if (next() % 2 == 0) drawCircle(color, stroke * 0.9f, p)
        else drawLine(color, p, Offset(center.x, center.y), stroke * 0.8f, StrokeCap.Round)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleMedium, color = LocalSkinColors.current.muted,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = readableOn(color),
        modifier = modifier.clip(CircleShape).background(color).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
