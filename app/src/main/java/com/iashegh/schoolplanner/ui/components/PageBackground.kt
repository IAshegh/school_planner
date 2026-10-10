package com.iashegh.schoolplanner.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.iashegh.schoolplanner.ui.theme.BgSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Full-screen page background: gradient, optional starfield, or the child's own photo with a dark veil. */
@Composable
fun PageBackground(spec: BgSpec, photoPath: String?, streak: Float) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(spec.top, spec.bottom)))) {
        if (spec.photo && photoPath != null) {
            val bmp by produceState<Bitmap?>(null, photoPath) {
                value = withContext(Dispatchers.IO) { loadScaled(photoPath, 1400) }
            }
            bmp?.let {
                Image(
                    it.asImageBitmap(), contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                )
            }
            // veil keeps white text readable on any photo
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
        }
        if (spec.stars) StarField(streak = streak)
    }
}

internal fun loadScaled(path: String, maxSide: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()
