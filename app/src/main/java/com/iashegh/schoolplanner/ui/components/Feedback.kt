package com.iashegh.schoolplanner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.iashegh.schoolplanner.audio.Sfx
import com.iashegh.schoolplanner.audio.SoundEngine

/** One place for "make it feel good": haptics + sound, both respecting the settings toggles. */
class Feedback(private val haptic: HapticFeedback, private val sound: SoundEngine) {
    var soundOn = true
    var hapticsOn = true

    private fun buzz(type: HapticFeedbackType) { if (hapticsOn) haptic.performHapticFeedback(type) }
    private fun sfx(s: Sfx) { if (soundOn) sound.play(s) }

    fun tap() { buzz(HapticFeedbackType.TextHandleMove); sfx(Sfx.CLICK) }
    fun snap() { buzz(HapticFeedbackType.LongPress); sfx(Sfx.SNAP) }
    fun bleep() { buzz(HapticFeedbackType.TextHandleMove); sfx(Sfx.BLEEP) }
    fun success() { buzz(HapticFeedbackType.LongPress); sfx(Sfx.CHEER) }
    fun chime() { sfx(Sfx.CHIME) }
    fun whoosh() { buzz(HapticFeedbackType.TextHandleMove); sfx(Sfx.WHOOSH) }
}

val LocalFeedback = staticCompositionLocalOf<Feedback> { error("Feedback not provided") }

@Composable
fun rememberFeedback(soundOn: Boolean, hapticsOn: Boolean): Feedback {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val engine = remember { SoundEngine(context.applicationContext) }
    DisposableEffect(engine) { onDispose { engine.release() } }
    val fb = remember(engine, haptic) { Feedback(haptic, engine) }
    fb.soundOn = soundOn
    fb.hapticsOn = hapticsOn
    return fb
}
