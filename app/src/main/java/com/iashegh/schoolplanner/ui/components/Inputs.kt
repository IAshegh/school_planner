package com.iashegh.schoolplanner.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iashegh.schoolplanner.data.fmtMinute
import com.iashegh.schoolplanner.ui.theme.LocalSkinColors
import com.iashegh.schoolplanner.ui.theme.readableOn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val LongDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
val ShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        actions()
    }
}

/** Big round tick used to complete homework. Springs when toggled. */
@Composable
fun CheckOrb(done: Boolean, color: Color, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        if (done) 1.1f else 1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium), label = "orb",
    )
    Box(
        Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (done) color else Color.Transparent)
            .border(3.dp, color, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, contentDescription = "Done", tint = readableOn(color), modifier = Modifier.size(28.dp))
    }
}

/** ‹ [Tue 30 Sep] › — tap the date for the spinner picker, arrows move one day. */
@Composable
fun DateStepper(date: LocalDate, modifier: Modifier = Modifier, onChange: (LocalDate) -> Unit) {
    var show by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { onChange(date.minusDays(1)) }) { Icon(Icons.Filled.ChevronLeft, "Previous day") }
        FilledTonalButton(onClick = { show = true }) {
            Icon(Icons.Filled.CalendarMonth, null, Modifier.size(20.dp))
            Text("  " + date.format(ShortDate) + " " + date.year, style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = { onChange(date.plusDays(1)) }) { Icon(Icons.Filled.ChevronRight, "Next day") }
    }
    if (show) DatePickDialog(date, onConfirm = { onChange(it); show = false }, onDismiss = { show = false })
}

/** Shows the time; tap it for the clock-face picker. */
@Composable
fun TimeStepper(minute: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    FilledTonalButton(onClick = { show = true }, modifier = modifier) {
        Icon(Icons.Filled.Schedule, null, Modifier.size(20.dp))
        Text("  " + fmtMinute(minute), style = MaterialTheme.typography.titleMedium)
    }
    if (show) TimePickDialog(minute, onConfirm = { onChange(it); show = false }, onDismiss = { show = false })
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalSkinColors.current.muted)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Horizontally scrolling row for chips, so long lists never wrap or overflow. */
@Composable
fun ChipScroller(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}
