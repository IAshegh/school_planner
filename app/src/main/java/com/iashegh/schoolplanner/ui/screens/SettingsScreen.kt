package com.iashegh.schoolplanner.ui.screens

import androidx.compose.foundation.background
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
