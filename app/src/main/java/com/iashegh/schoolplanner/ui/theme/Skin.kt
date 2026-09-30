package com.iashegh.schoolplanner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Skin(val label: String, val blurb: String) {
    GALACTIC("Galactic Droid", "Starfield, neon glow and a beeping droid friend"),
    BUILDER("Master Builder", "Toy bricks that snap, bounce and stack up"),
    SCRIBE("Alien Scribe", "Matte ink, brush-stroke rings and secret glyphs"),
}

data class SkinColors(
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val muted: Color,
    val primary: Color,
    val accent: Color,
    val border: Color,
    val dark: Boolean,
)

private val GalacticColors = SkinColors(
    background = Color(0xFF070B24), surface = Color(0xFF121A45), onSurface = Color(0xFFEAF0FF),
    muted = Color(0xFF9AA6D6), primary = Color(0xFF4FC3F7), accent = Color(0xFFB388FF),
    border = Color(0xFF4FC3F7), dark = true,
)
private val BuilderColors = SkinColors(
    background = Color(0xFFFFF8E1), surface = Color(0xFFFFFFFF), onSurface = Color(0xFF212121),
    muted = Color(0xFF6D6D6D), primary = Color(0xFFE53935), accent = Color(0xFF1E88E5),
    border = Color(0xFF212121), dark = false,
)
private val ScribeColors = SkinColors(
    background = Color(0xFFF2EEE6), surface = Color(0xFFFBF9F4), onSurface = Color(0xFF1F1D1A),
    muted = Color(0xFF6B655B), primary = Color(0xFF26231F), accent = Color(0xFF8C2F39),
    border = Color(0xFF26231F), dark = false,
)

fun Skin.colors(): SkinColors = when (this) {
    Skin.GALACTIC -> GalacticColors
    Skin.BUILDER -> BuilderColors
    Skin.SCRIBE -> ScribeColors
}

val LocalSkin = staticCompositionLocalOf { Skin.GALACTIC }
val LocalSkinColors = staticCompositionLocalOf { GalacticColors }

/** Text on top of an arbitrary subject colour. */
fun readableOn(c: Color): Color = if (c.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

private fun typographyFor(skin: Skin): Typography {
    val family = when (skin) {
        Skin.GALACTIC -> FontFamily.SansSerif
        Skin.BUILDER -> FontFamily.SansSerif
        Skin.SCRIBE -> FontFamily.Serif
    }
    val heavy = if (skin == Skin.BUILDER) FontWeight.ExtraBold else FontWeight.Bold
    fun s(size: Int, weight: FontWeight, line: Int) =
        TextStyle(fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)
    // Deliberately large: this app is read at a glance by a kid on a small phone.
    return Typography(
        headlineMedium = s(30, heavy, 36),
        headlineSmall = s(26, heavy, 32),
        titleLarge = s(22, heavy, 28),
        titleMedium = s(19, FontWeight.SemiBold, 24),
        titleSmall = s(16, FontWeight.SemiBold, 22),
        bodyLarge = s(18, FontWeight.Normal, 24),
        bodyMedium = s(16, FontWeight.Normal, 22),
        bodySmall = s(14, FontWeight.Normal, 19),
        labelLarge = s(16, FontWeight.SemiBold, 20),
        labelMedium = s(14, FontWeight.Medium, 18),
        labelSmall = s(12, FontWeight.Medium, 16),
    )
}

fun Skin.cardShape(): RoundedCornerShape = when (this) {
    Skin.GALACTIC -> RoundedCornerShape(18.dp)
    Skin.BUILDER -> RoundedCornerShape(8.dp)
    Skin.SCRIBE -> RoundedCornerShape(28.dp)
}

@Composable
fun PlannerTheme(skin: Skin, content: @Composable () -> Unit) {
    val c = skin.colors()
    val scheme = if (c.dark) {
        darkColorScheme(
            primary = c.primary, onPrimary = Color(0xFF00102A), secondary = c.accent,
            background = c.background, onBackground = c.onSurface,
            surface = c.surface, onSurface = c.onSurface, surfaceVariant = c.surface, onSurfaceVariant = c.muted,
            outline = c.border, primaryContainer = c.surface, onPrimaryContainer = c.onSurface,
            secondaryContainer = c.surface, onSecondaryContainer = c.onSurface,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = Color.White, secondary = c.accent,
            background = c.background, onBackground = c.onSurface,
            surface = c.surface, onSurface = c.onSurface, surfaceVariant = c.surface, onSurfaceVariant = c.muted,
            outline = c.border, primaryContainer = c.background, onPrimaryContainer = c.onSurface,
            secondaryContainer = c.primary.copy(alpha = 0.15f), onSecondaryContainer = c.onSurface,
        )
    }
    val shape = skin.cardShape()
    CompositionLocalProvider(LocalSkin provides skin, LocalSkinColors provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typographyFor(skin),
            shapes = Shapes(small = shape, medium = shape, large = shape),
            content = content,
        )
    }
}
