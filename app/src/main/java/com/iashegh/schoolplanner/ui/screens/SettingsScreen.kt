package com.iashegh.schoolplanner.ui.screens

import androidx.compose.foundation.background
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.iashegh.schoolplanner.ui.components.ChipScroller
import com.iashegh.schoolplanner.ui.components.loadScaled
import com.iashegh.schoolplanner.ui.theme.AppBackground
import com.iashegh.schoolplanner.ui.theme.backgroundSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.iashegh.schoolplanner.MainViewModel
import com.iashegh.schoolplanner.data.Settings
import com.iashegh.schoolplanner.ui.components.LocalFeedback
import com.iashegh.schoolplanner.ui.components.ScreenHeader
import com.iashegh.schoolplanner.ui.components.SectionTitle
import com.iashegh.schoolplanner.ui.components.SkinCard
import com.iashegh.schoolplanner.ui.components.SwitchRow
import com.iashegh.schoolplanner.ui.components.TimeStepper
import com.iashegh.schoolplanner.ui.components.mutedColor
import com.iashegh.schoolplanner.ui.theme.LocalSkinColors
import com.iashegh.schoolplanner.ui.theme.Skin
import com.iashegh.schoolplanner.ui.theme.colors

@Composable
fun SettingsScreen(vm: MainViewModel, settings: Settings, onBack: () -> Unit) {
    val fb = LocalFeedback.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Preferences", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle("Theme")
            Skin.values().forEach { skin ->
                val c = skin.colors()
                SkinCardPreview(
                    skin = skin, selected = settings.skin == skin,
                    onClick = { vm.setSkin(skin); fb.snap() },
                    swatch = listOf(c.background, c.primary, c.accent),
                )
            }
            SwitchRow(
                "Decoder mode", "Show subjects as circular alien glyphs",
                settings.decoderMode,
            ) { vm.setDecoder(it); fb.tap() }

            SectionTitle("Background")
            BackgroundChooser(vm, settings)

            SectionTitle("Sound & touch")
            SwitchRow("Sound effects", "Clicks, bleeps and cheers", settings.soundOn) { vm.setSound(it) }
            SwitchRow("Vibration", "Little buzzes when you tap and finish things", settings.hapticsOn) { vm.setHaptics(it) }

            SectionTitle("Morning reminder")
            SwitchRow("Show today's classes each morning", null, settings.morningAlertOn) { vm.setMorning(it, settings.morningMinute) }
            if (settings.morningAlertOn) TimeStepper(settings.morningMinute, { vm.setMorning(true, it) })
            Column(Modifier.padding(bottom = 32.dp)) {}
        }
    }
}

@Composable
private fun SkinCardPreview(skin: Skin, selected: Boolean, onClick: () -> Unit, swatch: List<androidx.compose.ui.graphics.Color>) {
    val colors = LocalSkinColors.current
    SkinCard(Modifier.fillMaxWidth(), accent = if (selected) colors.accent else colors.primary, onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(skin.label, style = MaterialTheme.typography.titleMedium)
                Text(skin.blurb, style = MaterialTheme.typography.bodySmall, color = mutedColor())
            }
            swatch.forEach {
                Row(
                    Modifier.padding(start = 4.dp).size(22.dp).clip(CircleShape).background(it),
                ) {}
            }
        }
    }
}


@Composable
private fun BackgroundChooser(vm: MainViewModel, settings: Settings) {
    val fb = LocalFeedback.current
    val colors = LocalSkinColors.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val path = vm.importPhoto(uri)
            if (path != null) vm.setBackground(AppBackground.PHOTO, path) else vm.message("Could not use that picture")
        }
    }
    ChipScroller {
        AppBackground.values().forEach { choice ->
            val spec = backgroundSpec(settings.skin, choice, settings.bgImagePath != null)
            val selected = settings.background == choice
            Column(
                Modifier
                    .width(76.dp)
                    .clickable {
                        fb.tap()
                        if (choice == AppBackground.PHOTO) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        else vm.setBackground(choice)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(spec.top, spec.bottom)))
                        .border(if (selected) 3.dp else 1.dp, if (selected) colors.accent else colors.muted.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (choice == AppBackground.PHOTO) {
                        val thumb by produceState<Bitmap?>(null, settings.bgImagePath) {
                            value = settings.bgImagePath?.let { p -> withContext(Dispatchers.IO) { loadScaled(p, 160) } }
                        }
                        thumb?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        Icon(Icons.Filled.PhotoCamera, null, tint = Color.White)
                    } else if (spec.stars) {
                        Text("✦ ✧", color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Text(
                    choice.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp), maxLines = 2,
                )
            }
        }
    }
}
