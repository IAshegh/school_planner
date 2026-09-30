package com.iashegh.schoolplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.iashegh.schoolplanner.MainViewModel
import com.iashegh.schoolplanner.data.BellPeriod
import com.iashegh.schoolplanner.data.CycleMode
import com.iashegh.schoolplanner.data.DayOverride
import com.iashegh.schoolplanner.data.ScheduleResolver
import com.iashegh.schoolplanner.data.Settings
import com.iashegh.schoolplanner.data.Subject
import com.iashegh.schoolplanner.data.SubjectIcons
import com.iashegh.schoolplanner.data.TimetableSlot
import com.iashegh.schoolplanner.data.fmtMinute
import com.iashegh.schoolplanner.data.short
import com.iashegh.schoolplanner.ui.components.ChipScroller
import com.iashegh.schoolplanner.ui.components.DateStepper
import com.iashegh.schoolplanner.ui.components.LocalFeedback
import com.iashegh.schoolplanner.ui.components.Pill
import com.iashegh.schoolplanner.ui.components.ScreenHeader
import com.iashegh.schoolplanner.ui.components.SectionTitle
import com.iashegh.schoolplanner.ui.components.ShortDate
import com.iashegh.schoolplanner.ui.components.SkinCard
import com.iashegh.schoolplanner.ui.components.SubjectBadge
import com.iashegh.schoolplanner.ui.components.TimeStepper
import com.iashegh.schoolplanner.ui.components.mutedColor
import com.iashegh.schoolplanner.ui.theme.LocalSkinColors
import com.iashegh.schoolplanner.ui.theme.readableOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

// ------------------------------------------------------------------ hub

@Composable
fun ParentHubScreen(
    vm: MainViewModel, settings: Settings, onNavigate: (String) -> Unit, onBack: () -> Unit, onLock: () -> Unit,
) {
    val subjects by vm.subjects.collectAsStateCompat()
    var changePin by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Parent area", onBack) {
            TextButton(onClick = onLock) { Icon(Icons.Filled.Lock, null); Text(" Lock") }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HubRow(Icons.Filled.School, "Subjects", "Names, colours and icons", Color(0xFF42A5F5)) { onNavigate("parent/subjects") }
            HubRow(Icons.Filled.Schedule, "Bell schedule", "When each period starts and ends", Color(0xFFFFA726)) { onNavigate("parent/bells") }
            HubRow(Icons.Filled.ViewWeek, "Timetable", "Who, what and where for each period", Color(0xFF66BB6A)) { onNavigate("parent/timetable") }
            HubRow(Icons.Filled.CalendarMonth, "Calendar & cycle", "School days, A/B weeks, rotating days", Color(0xFFAB47BC)) { onNavigate("parent/calendar") }
            HubRow(Icons.Filled.Event, "Days off & changes", "Holidays, half days, swapped lessons", Color(0xFFEF5350)) { onNavigate("parent/overrides") }
            HubRow(Icons.Filled.Save, "Backup & reset", "Save or restore everything as a file", Color(0xFF26C6DA)) { onNavigate("parent/backup") }
            HubRow(Icons.Filled.Password, "Change PIN", null, Color(0xFF8D6E63)) { changePin = true }

            if (subjects.isEmpty()) {
                OutlinedButton(onClick = { vm.loadSampleData() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Load an example timetable to try things out")
                }
            }
            Box(Modifier.padding(bottom = 32.dp))
        }
    }
    if (changePin) {
        CreatePinDialog("Change parent PIN", onDone = { vm.createPin(it); changePin = false }, onDismiss = { changePin = false })
    }
}

@Composable
private fun HubRow(icon: ImageVector, title: String, subtitle: String?, color: Color, onClick: () -> Unit) {
    val fb = LocalFeedback.current
    SkinCard(Modifier.fillMaxWidth(), accent = color, onClick = { fb.tap(); onClick() }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = readableOn(color))
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = mutedColor())
            }
        }
    }
}

// ------------------------------------------------------------------ subjects

@Composable
fun SubjectsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val subjects by vm.subjects.collectAsStateCompat()
    var editing by remember { mutableStateOf<Subject?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Subjects", onBack) {
            FilledTonalButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Filled.Add, null); Text(" Add") }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (subjects.isEmpty()) Text("No subjects yet. Tap Add.", color = LocalSkinColors.current.muted)
            subjects.forEach { s ->
                SkinCard(Modifier.fillMaxWidth(), accent = Color(s.color), onClick = { editing = s; showDialog = true }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        SubjectBadge(s, 44.dp)
                        Text(s.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 14.dp))
                    }
                }
            }
            Box(Modifier.padding(bottom = 32.dp))
        }
    }
    if (showDialog) {
        SubjectDialog(editing, onSave = { vm.saveSubject(it); showDialog = false },
            onDelete = { vm.deleteSubject(it); showDialog = false }, onDismiss = { showDialog = false })
    }
}

@Composable
private fun SubjectDialog(initial: Subject?, onSave: (Subject) -> Unit, onDelete: (Subject) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: SubjectIcons.palette.first()) }
    var icon by remember { mutableStateOf(initial?.icon ?: "star") }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${initial.name}?") },
            text = { Text("Its timetable slots, homework and exams will be deleted too.") },
            confirmButton = { Button(onClick = { onDelete(initial) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New subject" else "Edit subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ChipScroller {
                    SubjectIcons.palette.forEach { c ->
                        Box(
                            Modifier.size(38.dp).background(Color(c), CircleShape)
                                .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = c },
                        )
                    }
                }
                ChipScroller {
                    SubjectIcons.all.forEach { (key, vec) ->
                        FilterChip(selected = key == icon, onClick = { icon = key }, label = { Icon(vec, key, Modifier.size(22.dp)) })
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                onSave((initial ?: Subject(name = "", color = color)).copy(name = name.trim(), color = color, icon = icon))
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

// ------------------------------------------------------------------ bell schedule

@Composable
fun BellsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val bells by vm.bells.collectAsStateCompat()
    var editing by remember { mutableStateOf<BellPeriod?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Bell schedule", onBack) {
            FilledTonalButton(onClick = {
                val last = bells.maxByOrNull { it.period }
                val start = if (last == null) 8 * 60 + 30 else (last.endMinute + 5).coerceAtMost(23 * 60)
                vm.saveBell(BellPeriod((last?.period ?: 0) + 1, start, (start + 45).coerceAtMost(23 * 60 + 59)))
            }) { Icon(Icons.Filled.Add, null); Text(" Period") }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (bells.isEmpty()) Text("Add the periods of the school day. Tap a period to change its times.", color = LocalSkinColors.current.muted)
            bells.forEach { b ->
                SkinCard(Modifier.fillMaxWidth(), onClick = { editing = b }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Period ${b.period}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("${fmtMinute(b.startMinute)} – ${fmtMinute(b.endMinute)}", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            Box(Modifier.padding(bottom = 32.dp))
        }
    }
    editing?.let { b ->
        var start by remember(b) { mutableStateOf(b.startMinute) }
        var end by remember(b) { mutableStateOf(b.endMinute) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Period ${b.period}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Starts")
                    TimeStepper(start, { start = it })
                    Text("Ends")
                    TimeStepper(end, { end = it })
                    if (end <= start) Text("The end must be after the start.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(enabled = end > start, onClick = { vm.saveBell(b.copy(startMinute = start, endMinute = end)); editing = null }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.deleteBell(b.period); editing = null }) { Text("Delete") }
                    TextButton(onClick = { editing = null }) { Text("Cancel") }
                }
            },
        )
    }
}

// ------------------------------------------------------------------ timetable

@Composable
fun TimetableScreen(vm: MainViewModel, settings: Settings, onBack: () -> Unit) {
    val subjects by vm.subjects.collectAsStateCompat()
    val slots by vm.slots.collectAsStateCompat()
    val bells by vm.bells.collectAsStateCompat()
    val columns = remember(settings) { ScheduleResolver.columns(settings) }
    var selected by rememberSaveable { mutableStateOf(0) }
    var editPeriod by remember { mutableStateOf<Int?>(null) }
    val col = columns.getOrNull(selected.coerceAtMost((columns.size - 1).coerceAtLeast(0)))
    val periods = remember(bells, slots) { (bells.map { it.period } + slots.map { it.period }).distinct().sorted() }
    val subjectById = remember(subjects) { subjects.associateBy { it.id } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Timetable", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (subjects.isEmpty() || bells.isEmpty()) {
                Text("Add subjects and the bell schedule first, then fill in each day here.", color = LocalSkinColors.current.muted)
            }
            if (col != null) {
                ChipScroller {
                    columns.forEachIndexed { i, c -> FilterChip(selected = i == selected, onClick = { selected = i }, label = { Text(c.label) }) }
                }
                periods.forEach { p ->
                    val slot = slots.firstOrNull { it.dayKey == col.dayKey && it.period == p }
                    val subject = slot?.let { subjectById[it.subjectId] }
                    SkinCard(Modifier.fillMaxWidth(), accent = subject?.let { Color(it.color) } ?: LocalSkinColors.current.muted, onClick = { editPeriod = p }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("P$p", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 14.dp))
                            if (subject != null) {
                                Column(Modifier.weight(1f)) {
                                    Text(subject.name, style = MaterialTheme.typography.titleSmall)
                                    val d = listOf(slot?.room.orEmpty(), slot?.teacher.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
                                    if (d.isNotEmpty()) Text(d, style = MaterialTheme.typography.bodySmall, color = mutedColor())
                                }
                            } else {
                                Text("free", color = mutedColor(), modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Box(Modifier.padding(bottom = 32.dp))
        }
    }

    val p = editPeriod
    if (p != null && col != null) {
        val existing = slots.firstOrNull { it.dayKey == col.dayKey && it.period == p }
        var subjectId by remember(p, col.dayKey) { mutableStateOf(existing?.subjectId) }
        var room by remember(p, col.dayKey) { mutableStateOf(existing?.room ?: "") }
        var teacher by remember(p, col.dayKey) { mutableStateOf(existing?.teacher ?: "") }
        AlertDialog(
            onDismissRequest = { editPeriod = null },
            title = { Text("${col.label} · period $p") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChipScroller {
                        FilterChip(selected = subjectId == null, onClick = { subjectId = null }, label = { Text("Free") })
                        subjects.forEach { s ->
                            FilterChip(selected = subjectId == s.id, onClick = { subjectId = s.id }, label = { Text(s.name) }, leadingIcon = { SubjectBadge(s, 22.dp) })
                        }
                    }
                    OutlinedTextField(room, { room = it }, label = { Text("Room") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(teacher, { teacher = it }, label = { Text("Teacher") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = { vm.setSlot(col.dayKey, p, subjectId, room, teacher); editPeriod = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editPeriod = null }) { Text("Cancel") } },
        )
    }
}

// ------------------------------------------------------------------ calendar & cycle

@Composable
fun CalendarScreen(vm: MainViewModel, settings: Settings, onBack: () -> Unit) {
    var firstDay by remember { mutableStateOf(settings.firstDay) }
    var mask by remember { mutableStateOf(settings.schoolDaysMask) }
    var mode by remember { mutableStateOf(settings.cycleMode) }
    var length by remember { mutableStateOf(settings.cycleLength) }
    var anchor by remember { mutableStateOf(settings.anchor) }
    val fb = LocalFeedback.current

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Calendar & cycle", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle("First day of the week")
            ChipScroller {
                (0 until 7).map { DayOfWeek.MONDAY.plus(it.toLong()) }.forEach { d ->
                    FilterChip(selected = firstDay == d, onClick = { firstDay = d }, label = { Text(d.short()) })
                }
            }
            SectionTitle("School days")
            ChipScroller {
                (0 until 7).map { firstDay.plus(it.toLong()) }.forEach { d ->
                    val on = mask and (1 shl (d.value - 1)) != 0
                    FilterChip(selected = on, onClick = { mask = mask xor (1 shl (d.value - 1)) }, label = { Text(d.short()) })
                }
            }
            SectionTitle("Schedule type")
            ChipScroller {
                FilterChip(selected = mode == CycleMode.WEEKLY, onClick = { mode = CycleMode.WEEKLY }, label = { Text("Same every week") })
                FilterChip(selected = mode == CycleMode.AB, onClick = { mode = CycleMode.AB }, label = { Text("Week A / Week B") })
                FilterChip(selected = mode == CycleMode.ROTATING, onClick = { mode = CycleMode.ROTATING }, label = { Text("Rotating days") })
            }
            if (mode == CycleMode.ROTATING) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Days in the cycle", modifier = Modifier.weight(1f))
                    IconButton(onClick = { length = (length - 1).coerceAtLeast(2) }) { Icon(Icons.Filled.Remove, "Fewer") }
                    Text("$length", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { length = (length + 1).coerceAtMost(12) }) { Icon(Icons.Filled.Add, "More") }
                }
            }
            if (mode != CycleMode.WEEKLY) {
                Text(if (mode == CycleMode.AB) "The week that starts on this date is Week A" else "This date is Day 1 of the cycle")
                DateStepper(anchor) { anchor = it }
            }
            if (mode != settings.cycleMode || firstDay != settings.firstDay || mask != settings.schoolDaysMask || length != settings.cycleLength) {
                Text(
                    "Changing these changes the columns of the timetable. Check the timetable afterwards.",
                    color = LocalSkinColors.current.muted, style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                enabled = mask != 0,
                onClick = { vm.setCalendar(firstDay, mask, mode, length, anchor); fb.success(); vm.message("Calendar saved") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            Box(Modifier.padding(bottom = 32.dp))
        }
    }
}

// ------------------------------------------------------------------ overrides

@Composable
fun OverridesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val overrides by vm.overrides.collectAsStateCompat()
    var editing by remember { mutableStateOf<DayOverride?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Days off & changes", onBack) {
            FilledTonalButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Filled.Add, null); Text(" Add") }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (overrides.isEmpty()) Text("Nothing yet. Add holidays, teacher workdays or one-off timetable changes.", color = LocalSkinColors.current.muted)
            overrides.forEach { o ->
                SkinCard(Modifier.fillMaxWidth(), onClick = { editing = o; showDialog = true }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(o.date.format(ShortDate), style = MaterialTheme.typography.titleMedium)
                            if (o.note.isNotBlank()) Text(o.note, style = MaterialTheme.typography.bodySmall, color = mutedColor())
                        }
                        Pill(if (o.isHoliday) "No school" else "Changed", if (o.isHoliday) Color(0xFFEF5350) else Color(0xFFFFA726))
                    }
                }
            }
            Box(Modifier.padding(bottom = 32.dp))
        }
    }
    if (showDialog) {
        OverrideDialog(editing, vm, onDismiss = { showDialog = false })
    }
}

@Composable
private fun OverrideDialog(initial: DayOverride?, vm: MainViewModel, onDismiss: () -> Unit) {
    val subjects by vm.subjects.collectAsStateCompat()
    val bells by vm.bells.collectAsStateCompat()
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now().plusDays(1)) }
    var holiday by remember { mutableStateOf(initial?.isHoliday ?: true) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    val picks = remember {
        mutableStateMapOf<Int, Long>().also { m ->
            initial?.replacementJson?.let { ScheduleResolver.parseReplacement(it) }?.forEach { m[it.period] = it.subjectId }
        }
    }
    val periods = remember(bells) { bells.map { it.period }.ifEmpty { (1..6).toList() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add a day off or change" else "Edit day") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DateStepper(date) { date = it }
                ChipScroller {
                    FilterChip(selected = holiday, onClick = { holiday = true }, label = { Text("No school") })
                    FilterChip(selected = !holiday, onClick = { holiday = false }, label = { Text("Different lessons") })
                }
                OutlinedTextField(note, { note = it }, label = { Text("Note (e.g. Field trip)") }, modifier = Modifier.fillMaxWidth())
                if (!holiday) {
                    Text("Lessons on this day (replaces the normal timetable)", style = MaterialTheme.typography.labelLarge)
                    periods.forEach { p ->
                        Text("Period $p", style = MaterialTheme.typography.labelMedium)
                        ChipScroller {
                            FilterChip(selected = picks[p] == null, onClick = { picks.remove(p) }, label = { Text("Free") })
                            subjects.forEach { s ->
                                FilterChip(selected = picks[p] == s.id, onClick = { picks[p] = s.id }, label = { Text(s.name) })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (initial != null && initial.date != date) vm.deleteOverride(initial)
                val json = if (holiday) null else ScheduleResolver.encodeReplacement(
                    picks.entries.sortedBy { it.key }.map { ScheduleResolver.Replacement(it.key, it.value, "") },
                )
                vm.saveOverride(DayOverride(date, holiday, note.trim(), json))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = { vm.deleteOverride(initial); onDismiss() }) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

// ------------------------------------------------------------------ backup & reset

@Composable
fun BackupScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val json = vm.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                }
                vm.message("Backup saved")
            } catch (e: Exception) {
                vm.message("Could not save: ${e.message}")
            }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                pendingImport = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                }
            } catch (e: Exception) {
                vm.message("Could not read file: ${e.message}")
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Backup & reset", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "A backup is a plain JSON file with subjects, timetable, bell schedule, homework, exams and days off. " +
                    "It does not include the PIN or attached photos.",
                color = LocalSkinColors.current.muted,
            )
            Button(onClick = { export.launch("school-planner-backup.json") }, modifier = Modifier.fillMaxWidth()) { Text("Save backup file") }
            OutlinedButton(
                onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restore from backup file") }
            SectionTitle("Danger zone")
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Erase everything") }
        }
    }

    pendingImport?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Restore this backup?") },
            text = { Text("Everything currently in the app will be replaced.") },
            confirmButton = { Button(onClick = { vm.importJson(json); pendingImport = null }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
        )
    }
    if (confirmReset) {
        ConfirmResetDialog(
            "This deletes all subjects, timetable, homework, exams and the parent PIN.",
            onConfirm = { vm.resetEverything(); confirmReset = false }, onDismiss = { confirmReset = false },
        )
    }
}
