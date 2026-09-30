package com.iashegh.schoolplanner.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iashegh.schoolplanner.MainViewModel
import com.iashegh.schoolplanner.data.Exam
import com.iashegh.schoolplanner.data.Homework
import com.iashegh.schoolplanner.data.Settings
import com.iashegh.schoolplanner.data.Subject
import com.iashegh.schoolplanner.data.fmtMinute
import com.iashegh.schoolplanner.ui.components.CheckOrb
import com.iashegh.schoolplanner.ui.components.ChipScroller
import com.iashegh.schoolplanner.ui.components.DateStepper
import com.iashegh.schoolplanner.ui.components.LocalFeedback
import com.iashegh.schoolplanner.ui.components.Pill
import com.iashegh.schoolplanner.ui.components.ProgressRing
import com.iashegh.schoolplanner.ui.components.RewardTower
import com.iashegh.schoolplanner.ui.components.SectionTitle
import com.iashegh.schoolplanner.ui.components.ShortDate
import com.iashegh.schoolplanner.ui.components.SkinCard
import com.iashegh.schoolplanner.ui.components.SubjectBadge
import com.iashegh.schoolplanner.ui.components.TimeStepper
import com.iashegh.schoolplanner.ui.components.enterAnim
import com.iashegh.schoolplanner.ui.components.mutedColor
import com.iashegh.schoolplanner.ui.theme.LocalSkin
import com.iashegh.schoolplanner.ui.theme.LocalSkinColors
import com.iashegh.schoolplanner.ui.theme.Skin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

fun relativeDays(days: Long): String = when {
    days == 0L -> "Today"
    days == 1L -> "Tomorrow"
    days == -1L -> "Yesterday"
    days > 1 -> "In $days days"
    else -> "${-days} days ago"
}

@Composable
fun PlannerScreen(vm: MainViewModel, settings: Settings) {
    val subjects by vm.subjects.collectAsStateCompat()
    val homework by vm.homework.collectAsStateCompat()
    val exams by vm.exams.collectAsStateCompat()
    val fb = LocalFeedback.current

    var tab by rememberSaveable { mutableStateOf(0) }
    var editingHomework by remember { mutableStateOf<Homework?>(null) }
    var editingExam by remember { mutableStateOf<Exam?>(null) }
    var showHomeworkDialog by remember { mutableStateOf(false) }
    var showExamDialog by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val subjectById = remember(subjects) { subjects.associateBy { it.id } }
    val weekStart = today.with(TemporalAdjusters.previousOrSame(settings.firstDay))
    val doneThisWeek = homework.count { it.done && it.doneDate != null && !it.doneDate.isBefore(weekStart) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                contentColor = LocalSkinColors.current.onSurface,
            ) {
                Tab(selected = tab == 0, onClick = { fb.tap(); tab = 0 }, text = { Text("Homework", style = MaterialTheme.typography.titleSmall) })
                Tab(selected = tab == 1, onClick = { fb.tap(); tab = 1 }, text = { Text("Exams", style = MaterialTheme.typography.titleSmall) })
            }

            if (tab == 0) {
                val open = homework.filter { !it.done }
                val dueNow = open.filter { !it.dueDate.isAfter(today) }
                val upcoming = open.filter { it.dueDate.isAfter(today) }
                val completed = homework.filter { it.done }.sortedByDescending { it.doneDate ?: LocalDate.MIN }.take(15)

                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { RewardTower(doneThisWeek) }
                    if (subjects.isEmpty()) item { EmptyHint("Ask a parent to add subjects first (lock icon), then you can add homework here.") }
                    if (open.isEmpty() && subjects.isNotEmpty()) item { EmptyHint("No homework. Enjoy!") }

                    if (dueNow.isNotEmpty()) item { SectionTitle("Due today") }
                    items(dueNow, key = { "d${it.id}" }) { h ->
                        HomeworkRow(h, subjectById[h.subjectId], today, vm) { editingHomework = h; showHomeworkDialog = true }
                    }
                    if (upcoming.isNotEmpty()) item { SectionTitle("Upcoming") }
                    items(upcoming, key = { "u${it.id}" }) { h ->
                        HomeworkRow(h, subjectById[h.subjectId], today, vm) { editingHomework = h; showHomeworkDialog = true }
                    }
                    if (completed.isNotEmpty()) item { SectionTitle("Completed") }
                    items(completed, key = { "c${it.id}" }) { h ->
                        HomeworkRow(h, subjectById[h.subjectId], today, vm) { editingHomework = h; showHomeworkDialog = true }
                    }
                }
            } else {
                val upcoming = exams.filter { !it.date.isBefore(today) }
                val past = exams.filter { it.date.isBefore(today) }.sortedByDescending { it.date }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (subjects.isEmpty()) item { EmptyHint("Ask a parent to add subjects first (lock icon).") }
                    else if (upcoming.isEmpty()) item { EmptyHint("No exams coming up. Tap + to add one.") }
                    items(upcoming.withIndex().toList(), key = { "e${it.value.id}" }) { (i, e) ->
                        ExamCard(e, subjectById[e.subjectId], today, Modifier.enterAnim(i)) { editingExam = e; showExamDialog = true }
                    }
                    if (past.isNotEmpty()) item { SectionTitle("Past") }
                    items(past, key = { "p${it.id}" }) { e ->
                        ExamCard(e, subjectById[e.subjectId], today, Modifier) { editingExam = e; showExamDialog = true }
                    }
                }
            }
        }

        if (subjects.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    fb.snap()
                    if (tab == 0) { editingHomework = null; showHomeworkDialog = true } else { editingExam = null; showExamDialog = true }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                containerColor = LocalSkinColors.current.primary,
                contentColor = if (LocalSkinColors.current.dark) Color(0xFF00102A) else Color.White,
            ) { Icon(Icons.Filled.Add, contentDescription = "Add") }
        }
    }

    if (showHomeworkDialog) {
        HomeworkDialog(
            initial = editingHomework, subjects = subjects, vm = vm,
            onDismiss = { showHomeworkDialog = false },
        )
    }
    if (showExamDialog) {
        ExamDialog(
            initial = editingExam, subjects = subjects,
            onSave = { vm.saveExam(it); showExamDialog = false },
            onDelete = { e -> vm.deleteExam(e); showExamDialog = false },
            onDismiss = { showExamDialog = false },
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, color = LocalSkinColors.current.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
}

@Composable
private fun HomeworkRow(h: Homework, subject: Subject?, today: LocalDate, vm: MainViewModel, onEdit: () -> Unit) {
    val fb = LocalFeedback.current
    val c = Color(subject?.color ?: 0xFF888888.toInt())
    val builder = LocalSkin.current == Skin.BUILDER
    SkinCard(Modifier.fillMaxWidth(), accent = c, onClick = onEdit) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CheckOrb(h.done, if (builder) Color.White else c) {
                if (!h.done) fb.success() else fb.tap()
                vm.toggleHomework(h)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(h.title, style = MaterialTheme.typography.titleSmall)
                val days = ChronoUnit.DAYS.between(today, h.dueDate)
                Text(
                    "${subject?.name ?: "?"} · ${if (h.done) "done" else relativeDays(days)}",
                    style = MaterialTheme.typography.bodySmall, color = mutedColor(),
                )
            }
            if (h.photoUri != null) Icon(Icons.Filled.PhotoCamera, contentDescription = "Has photo", modifier = Modifier.padding(end = 8.dp))
            when (h.priority) {
                2 -> Pill("Urgent", Color(0xFFE53935))
                1 -> Pill("Important", Color(0xFFFFB300))
            }
        }
    }
}

@Composable
private fun ExamCard(e: Exam, subject: Subject?, today: LocalDate, modifier: Modifier, onEdit: () -> Unit) {
    val colors = LocalSkinColors.current
    val days = ChronoUnit.DAYS.between(today, e.date)
    val c = Color(subject?.color ?: 0xFF888888.toInt())
    val ringColor = if (LocalSkin.current == Skin.BUILDER) Color.White else if (days in 0..2) Color(0xFFE53935) else c
    SkinCard(modifier.fillMaxWidth(), accent = c, onClick = onEdit) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(if (days < 0) 1f else 1f - (days.coerceAtMost(14) / 14f), ringColor, 72.dp) {
                Text(if (days < 0) "✓" else "$days", style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(subject?.name ?: "Exam", style = MaterialTheme.typography.titleMedium)
                if (e.title.isNotBlank()) Text(e.title, style = MaterialTheme.typography.bodyMedium)
                val whenText = relativeDays(days) + " · " + e.date.format(ShortDate) +
                    (if (e.startMinute >= 0) " " + fmtMinute(e.startMinute) else "")
                Text(whenText, style = MaterialTheme.typography.bodyMedium, color = mutedColor())
                if (e.room.isNotBlank()) Text("Room ${e.room}", style = MaterialTheme.typography.bodySmall, color = mutedColor())
                if (e.topics.isNotBlank()) Text(e.topics, style = MaterialTheme.typography.bodySmall, color = mutedColor(), maxLines = 3)
            }
        }
    }
}

// ---------------------------------------------------------------- dialogs

@Composable
private fun SubjectPicker(subjects: List<Subject>, selectedId: Long?, onPick: (Long) -> Unit) {
    ChipScroller {
        subjects.forEach { s ->
            FilterChip(
                selected = s.id == selectedId, onClick = { onPick(s.id) },
                label = { Text(s.name) },
                leadingIcon = { SubjectBadge(s, 22.dp) },
            )
        }
    }
}

@Composable
private fun HomeworkDialog(initial: Homework?, subjects: List<Subject>, vm: MainViewModel, onDismiss: () -> Unit) {
    var subjectId by remember { mutableStateOf(initial?.subjectId ?: subjects.firstOrNull()?.id) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var due by remember { mutableStateOf(initial?.dueDate ?: LocalDate.now().plusDays(1)) }
    var priority by remember { mutableStateOf(initial?.priority ?: 0) }
    var photo by remember { mutableStateOf(initial?.photoUri) }
    val scope = rememberCoroutineScope()

    var pendingCamera by remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingCamera?.let { photo = it.absolutePath } else pendingCamera?.delete()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch { vm.importPhoto(uri)?.let { photo = it } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New homework" else "Edit homework") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SubjectPicker(subjects, subjectId) { subjectId = it }
                OutlinedTextField(title, { title = it }, label = { Text("What do you need to do?") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                Text("Due", style = MaterialTheme.typography.labelLarge)
                DateStepper(due) { due = it }
                Text("How important?", style = MaterialTheme.typography.labelLarge)
                ChipScroller {
                    listOf("Normal", "Important", "Urgent").forEachIndexed { i, label ->
                        FilterChip(selected = priority == i, onClick = { priority = i }, label = { Text(label) })
                    }
                }
                Text("Photo of the board (optional)", style = MaterialTheme.typography.labelLarge)
                photo?.let { PhotoThumb(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val (file, uri) = vm.newPhotoTarget()
                        pendingCamera = file
                        camera.launch(uri)
                    }) { Text("Camera") }
                    OutlinedButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Gallery") }
                    if (photo != null) TextButton(onClick = { photo = null }) { Text("Remove") }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = subjectId != null && title.isNotBlank(),
                onClick = {
                    vm.saveHomework(
                        (initial ?: Homework(subjectId = subjectId!!, title = "", dueDate = due)).copy(
                            subjectId = subjectId!!, title = title.trim(), notes = notes.trim(),
                            dueDate = due, priority = priority, photoUri = photo,
                        ),
                    )
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = { vm.deleteHomework(initial); onDismiss() }) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun PhotoThumb(path: String) {
    val bmp by produceState<Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(path, opts)
            }.getOrNull()
        }
    }
    bmp?.let {
        Image(
            it.asImageBitmap(), contentDescription = "Attached photo",
            modifier = Modifier.fillMaxWidth().height(140.dp).clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun ExamDialog(
    initial: Exam?, subjects: List<Subject>,
    onSave: (Exam) -> Unit, onDelete: (Exam) -> Unit, onDismiss: () -> Unit,
) {
    var subjectId by remember { mutableStateOf(initial?.subjectId ?: subjects.firstOrNull()?.id) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now().plusDays(7)) }
    var hasTime by remember { mutableStateOf((initial?.startMinute ?: -1) >= 0) }
    var minute by remember { mutableStateOf(initial?.startMinute?.takeIf { it >= 0 } ?: (9 * 60)) }
    var room by remember { mutableStateOf(initial?.room ?: "") }
    var topics by remember { mutableStateOf(initial?.topics ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New exam" else "Edit exam") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SubjectPicker(subjects, subjectId) { subjectId = it }
                OutlinedTextField(title, { title = it }, label = { Text("Name (e.g. Unit 3 test)") }, modifier = Modifier.fillMaxWidth())
                DateStepper(date) { date = it }
                ChipScroller {
                    FilterChip(selected = !hasTime, onClick = { hasTime = false }, label = { Text("No time") })
                    FilterChip(selected = hasTime, onClick = { hasTime = true }, label = { Text("Set time") })
                }
                if (hasTime) TimeStepper(minute, { minute = it })
                OutlinedTextField(room, { room = it }, label = { Text("Room") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(topics, { topics = it }, label = { Text("Topics covered") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Study notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                enabled = subjectId != null,
                onClick = {
                    onSave(
                        (initial ?: Exam(subjectId = subjectId!!, date = date)).copy(
                            subjectId = subjectId!!, title = title.trim(), date = date,
                            startMinute = if (hasTime) minute else -1, room = room.trim(),
                            topics = topics.trim(), notes = notes.trim(),
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = { onDelete(initial) }) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
