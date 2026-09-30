package com.iashegh.schoolplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iashegh.schoolplanner.MainViewModel
import com.iashegh.schoolplanner.data.ScheduleResolver
import com.iashegh.schoolplanner.data.Settings
import com.iashegh.schoolplanner.data.fmtMinute
import com.iashegh.schoolplanner.ui.components.Pill
import com.iashegh.schoolplanner.ui.components.SubjectBadge
import com.iashegh.schoolplanner.ui.theme.LocalSkinColors
import com.iashegh.schoolplanner.ui.theme.readableOn
import java.time.LocalDate

@Composable
fun WeekScreen(vm: MainViewModel, settings: Settings) {
    val subjects by vm.subjects.collectAsStateCompat()
    val slots by vm.slots.collectAsStateCompat()
    val bells by vm.bells.collectAsStateCompat()
    val overrides by vm.overrides.collectAsStateCompat()
    val colors = LocalSkinColors.current

    var zoom by remember { mutableFloatStateOf(1f) }
    val columns = remember(settings) { ScheduleResolver.columns(settings) }
    val periods = remember(bells, slots) { (bells.map { it.period } + slots.map { it.period }).distinct().sorted() }
    val subjectById = remember(subjects) { subjects.associateBy { it.id } }
    val bellBy = remember(bells) { bells.associateBy { it.period } }
    val today = LocalDate.now()
    val holidays = remember(overrides) { overrides.filter { it.isHoliday }.map { it.date }.toSet() }
    val todayKey = remember(settings, holidays) { ScheduleResolver.dayKeyFor(today, settings, holidays) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Week", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            todayKey?.let { key -> ScheduleResolver.labelFor(key, settings)?.let { Pill("Today: $it", colors.accent) } }
            IconButton(onClick = { zoom = (zoom - 0.2f).coerceAtLeast(0.6f) }) { Icon(Icons.Filled.Remove, "Zoom out") }
            IconButton(onClick = { zoom = (zoom + 0.2f).coerceAtMost(2f) }) { Icon(Icons.Filled.Add, "Zoom in") }
        }

        if (periods.isEmpty() || columns.isEmpty()) {
            Text(
                "The timetable is empty. A parent can set it up with the lock icon.",
                color = colors.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
            return@Column
        }

        val cellW = (92 * zoom).dp
        val cellH = (68 * zoom).dp
        val headW = (62 * zoom).dp

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                Column {
                    // header row
                    Row {
                        Box(Modifier.width(headW).height(40.dp))
                        columns.forEach { col ->
                            val isToday = col.dayKey == todayKey
                            Box(
                                Modifier.width(cellW).height(40.dp).padding(2.dp)
                                    .background(if (isToday) colors.accent.copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(col.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }
                    periods.forEach { p ->
                        Row {
                            Box(Modifier.width(headW).height(cellH), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("P$p", style = MaterialTheme.typography.labelLarge)
                                    bellBy[p]?.let { Text(fmtMinute(it.startMinute), style = MaterialTheme.typography.labelSmall, color = colors.muted) }
                                }
                            }
                            columns.forEach { col ->
                                val slot = slots.firstOrNull { it.dayKey == col.dayKey && it.period == p }
                                val subject = slot?.let { subjectById[it.subjectId] }
                                val room = slot?.room.orEmpty()
                                Box(Modifier.width(cellW).height(cellH).padding(2.dp)) {
                                    if (subject != null) {
                                        val c = Color(subject.color)
                                        Column(
                                            Modifier.fillMaxSize()
                                                .background(c, MaterialTheme.shapes.small)
                                                .then(if (col.dayKey == todayKey) Modifier.border(2.dp, colors.onSurface, MaterialTheme.shapes.small) else Modifier)
                                                .padding(4.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                subject.name, color = readableOn(c), fontSize = (13 * zoom).sp,
                                                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                                            )
                                            if (room.isNotBlank()) {
                                                Text(room, color = readableOn(c).copy(alpha = 0.8f), fontSize = (11 * zoom).sp)
                                            }
                                        }
                                    } else {
                                        Box(Modifier.fillMaxSize().border(1.dp, colors.muted.copy(alpha = 0.25f), MaterialTheme.shapes.small))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
