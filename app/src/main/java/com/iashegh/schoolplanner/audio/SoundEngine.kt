package com.iashegh.schoolplanner.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class Sfx { CLICK, SNAP, BLEEP, CHIME, CHEER, WHOOSH }

/**
 * Low-latency effects. The clips are synthesised once into small WAV files in the cache folder
 * and pre-loaded into a [SoundPool], so no audio assets need to ship with the app.
 */
class SoundEngine(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = HashMap<Sfx, Int>()
    private val loaded = HashSet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded.add(sampleId) }
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        Sfx.values().forEach { sfx ->
            val f = File(dir, "${sfx.name.lowercase()}.wav")
            runCatching {
                f.writeBytes(wav(synth(sfx)))
                ids[sfx] = pool.load(f.absolutePath, 1)
            }
        }
    }

    fun play(sfx: Sfx, volume: Float = 0.8f) {
        val id = ids[sfx] ?: return
        if (id in loaded) pool.play(id, volume, volume, 1, 0, 1f)
    }

    fun release() = pool.release()

    // ---- synthesis ----
    private val rate = 22050

    private fun tone(freq: Double, ms: Int, decay: Double = 8.0, harmonic: Double = 0.0, slideTo: Double = freq): DoubleArray {
        val n = rate * ms / 1000
        return DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            val f = freq + (slideTo - freq) * (i.toDouble() / n)
            val env = exp(-decay * t) * minOf(1.0, i / 80.0)
            (sin(2 * PI * f * t) + harmonic * sin(4 * PI * f * t)) * env
        }
    }

    private fun concat(vararg parts: DoubleArray): DoubleArray = parts.fold(DoubleArray(0)) { a, b -> a + b }

    private fun synth(sfx: Sfx): DoubleArray = when (sfx) {
        Sfx.CLICK -> tone(1800.0, 40, decay = 90.0)
        Sfx.SNAP -> concat(tone(220.0, 25, decay = 120.0, harmonic = 0.8), tone(880.0, 45, decay = 70.0))
        Sfx.BLEEP -> concat(tone(520.0, 70, 12.0, 0.3, 780.0), tone(1040.0, 90, 10.0, 0.3, 700.0))
        Sfx.CHIME -> concat(tone(784.0, 120, 9.0, 0.2), tone(1175.0, 260, 5.0, 0.2))
        Sfx.CHEER -> concat(tone(523.0, 90, 6.0), tone(659.0, 90, 6.0), tone(784.0, 90, 6.0), tone(1047.0, 350, 4.0, 0.3))
        Sfx.WHOOSH -> tone(200.0, 320, 5.0, 0.0, 1400.0)
    }

    private fun wav(samples: DoubleArray): ByteArray {
        val pcm = ByteArray(samples.size * 2)
        samples.forEachIndexed { i, s ->
            val v = (s.coerceIn(-1.0, 1.0) * 0.7 * Short.MAX_VALUE).toInt()
            pcm[i * 2] = (v and 0xFF).toByte()
            pcm[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        val out = ByteArrayOutputStream()
        fun int(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
        fun short(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
        out.write("RIFF".toByteArray()); int(36 + pcm.size)
        out.write("WAVE".toByteArray()); out.write("fmt ".toByteArray())
        int(16); short(1); short(1); int(rate); int(rate * 2); short(2); short(16)
        out.write("data".toByteArray()); int(pcm.size)
        out.write(pcm)
        return out.toByteArray()
    }
}
