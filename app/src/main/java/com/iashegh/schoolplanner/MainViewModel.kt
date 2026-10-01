package com.iashegh.schoolplanner

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iashegh.schoolplanner.alerts.ReminderScheduler
import com.iashegh.schoolplanner.data.AppDatabase
import com.iashegh.schoolplanner.data.BellPeriod
import com.iashegh.schoolplanner.data.Backup
import com.iashegh.schoolplanner.data.CycleMode
import com.iashegh.schoolplanner.data.DayOverride
import com.iashegh.schoolplanner.data.Exam
import com.iashegh.schoolplanner.data.Homework
import com.iashegh.schoolplanner.data.PinManager
import com.iashegh.schoolplanner.data.ScheduleResolver
import com.iashegh.schoolplanner.data.Settings
import com.iashegh.schoolplanner.data.SettingsRepository
import com.iashegh.schoolplanner.data.SyncManager
import com.iashegh.schoolplanner.data.Subject
import com.iashegh.schoolplanner.data.TimetableSlot
import com.iashegh.schoolplanner.ui.theme.Skin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val dao = db.dao()
    private val settingsRepo = SettingsRepository(app)

    /** null until DataStore has delivered the first value (avoids flashing the wrong skin). */
    val settings: StateFlow<Settings?> = settingsRepo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val subjects = dao.subjects().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val slots = dao.slots().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val bells = dao.bells().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val homework = dao.homework().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val exams = dao.exams().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val overrides = dao.overrides().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sync = SyncManager(app, db, settingsRepo, viewModelScope)
    val syncStatus: StateFlow<String> = sync.status

    /** Only lives as long as the process; leaving the parent screens locks again. */
    var parentUnlocked by mutableStateOf(false)
    /** Bumped whenever something worth celebrating happens. */
    var cheer by mutableIntStateOf(0)
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        viewModelScope.launch {
            settings.filterNotNull().map { it.syncRole to it.familyCode }.distinctUntilChanged()
                .collect { (role, code) -> sync.restart(role, code) }
        }
        viewModelScope.launch {
            ReminderScheduler.rescheduleAll(app, settingsRepo.flow.first())
        }
    }

    // ---- settings ----
    fun setSkin(s: Skin) = viewModelScope.launch { settingsRepo.setSkin(s) }
    fun setSound(v: Boolean) = viewModelScope.launch { settingsRepo.setSound(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { settingsRepo.setHaptics(v) }
    fun setDecoder(v: Boolean) = viewModelScope.launch { settingsRepo.setDecoder(v) }

    fun setMorning(on: Boolean, minute: Int) = viewModelScope.launch {
        settingsRepo.setMorning(on, minute)
        ReminderScheduler.scheduleMorning(app, settingsRepo.flow.first())
    }

    fun setCalendar(firstDay: DayOfWeek, mask: Int, mode: CycleMode, length: Int, anchor: LocalDate) =
        viewModelScope.launch { settingsRepo.setCalendar(firstDay, mask, mode, length, anchor) }

    // ---- PIN ----
    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.Default) {
        val s = settingsRepo.flow.first()
        PinManager.verify(pin, s.pinSalt, s.pinHash)
    }

    fun createPin(pin: String) = viewModelScope.launch {
        val salt = PinManager.newSalt()
        val hash = withContext(Dispatchers.Default) { PinManager.hash(pin, salt) }
        settingsRepo.setPin(hash, salt)
        parentUnlocked = true
    }

    fun lockParent() { parentUnlocked = false }
    fun unlockParent() { parentUnlocked = true }

    // ---- subjects & timetable ----
    fun saveSubject(s: Subject) = viewModelScope.launch { dao.upsertSubject(s) }

    fun deleteSubject(s: Subject) = viewModelScope.launch {
        // exams of this subject disappear through the cascade, so drop their alarms first
        exams.value.filter { it.subjectId == s.id }.forEach { ReminderScheduler.cancelExam(app, it.id) }
        dao.deleteSubject(s)
    }

    fun setSlot(dayKey: Int, period: Int, subjectId: Long?, room: String, teacher: String) = viewModelScope.launch {
        dao.clearSlot(dayKey, period)
        if (subjectId != null) dao.upsertSlot(TimetableSlot(0, subjectId, dayKey, period, room.trim(), teacher.trim()))
    }

    fun saveBell(b: BellPeriod) = viewModelScope.launch { dao.upsertBell(b) }
    fun deleteBell(period: Int) = viewModelScope.launch { dao.deleteBell(period) }

    fun saveOverride(o: DayOverride) = viewModelScope.launch { dao.upsertOverride(o) }
    fun deleteOverride(o: DayOverride) = viewModelScope.launch { dao.deleteOverride(o) }

    // ---- homework ----
    fun saveHomework(h: Homework) = viewModelScope.launch {
        dao.upsertHomework(h)
        sync.pushHomework(h)
    }

    fun toggleHomework(h: Homework) = viewModelScope.launch {
        val nowDone = !h.done
        val updated = h.copy(done = nowDone, doneDate = if (nowDone) LocalDate.now() else null)
        dao.upsertHomework(updated)
        sync.pushHomework(updated)
        if (nowDone) cheer++
    }

    fun deleteHomework(h: Homework) = viewModelScope.launch {
        h.photoUri?.let { runCatching { File(it).delete() } }
        dao.deleteHomework(h)
        sync.deleteHomework(h)
    }

    // ---- exams ----
    fun saveExam(e: Exam) = viewModelScope.launch {
        val id = dao.upsertExam(e)
        val saved = if (e.id == 0L) e.copy(id = id) else e
        ReminderScheduler.cancelExam(app, saved.id)
        val name = subjects.value.firstOrNull { it.id == saved.subjectId }?.name ?: "Exam"
        ReminderScheduler.scheduleExam(app, saved, name)
        sync.pushExam(saved)
    }

    fun deleteExam(e: Exam) = viewModelScope.launch {
        ReminderScheduler.cancelExam(app, e.id)
        dao.deleteExam(e)
        sync.deleteExam(e)
    }

    // ---- photos ----
    private fun photoDir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    /** A fresh file + content Uri for the camera to write into. */
    fun newPhotoTarget(): Pair<File, Uri> {
        val f = File(photoDir(app), "${UUID.randomUUID()}.jpg")
        return f to FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", f)
    }

    /** Copies a picked image into app storage (picker Uris are only temporary). Returns the file path. */
    suspend fun importPhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val f = File(photoDir(app), "${UUID.randomUUID()}.jpg")
            app.contentResolver.openInputStream(uri)!!.use { input -> f.outputStream().use { input.copyTo(it) } }
            f.absolutePath
        }.getOrNull()
    }

    // ---- phone sync ----
    suspend fun createFamily(): String = sync.createFamily()
    suspend fun joinFamily(code: String) = sync.joinFamily(code)
    fun unlinkSync() = viewModelScope.launch { sync.unlink() }

    // ---- backup / reset ----
    suspend fun exportJson(): String = Backup.export(db, settingsRepo.flow.first())

    fun importJson(json: String) = viewModelScope.launch {
        try {
            exams.value.forEach { ReminderScheduler.cancelExam(app, it.id) }
            Backup.import(db, settingsRepo, json)
            ReminderScheduler.rescheduleAll(app, settingsRepo.flow.first())
            _messages.tryEmit("Backup restored")
        } catch (e: Exception) {
            _messages.tryEmit("Could not restore: ${e.message ?: "invalid file"}")
        }
    }

    fun resetEverything() = viewModelScope.launch {
        exams.value.forEach { ReminderScheduler.cancelExam(app, it.id) }
        homework.value.forEach { h -> h.photoUri?.let { runCatching { File(it).delete() } } }
        db.tx {
            dao.clearHomework(); dao.clearExams(); dao.clearOverrides(); dao.clearSlots(); dao.clearBells(); dao.clearSubjects()
        }
        settingsRepo.clearAll()
        parentUnlocked = false
        ReminderScheduler.scheduleMorning(app, settingsRepo.flow.first())
        _messages.tryEmit("Everything was reset")
    }

    /** Handy for trying the app out: a few subjects, a bell schedule and a full example timetable. */
    fun loadSampleData() = viewModelScope.launch {
        val s = settingsRepo.flow.first()
        val defs = listOf(
            Triple("Math", 0xFF42A5F5, "math"), Triple("Science", 0xFF66BB6A, "science"),
            Triple("English", 0xFFEF5350, "book"), Triple("Art", 0xFFAB47BC, "art"),
            Triple("PE", 0xFFFF7043, "sport"), Triple("Music", 0xFFFFCA28, "music"),
        )
        val ids = defs.map { (name, color, icon) -> dao.upsertSubject(Subject(name = name, color = color.toInt(), icon = icon)) }
        for (p in 1..6) dao.upsertBell(BellPeriod(p, 8 * 60 + 30 + (p - 1) * 50, 8 * 60 + 30 + (p - 1) * 50 + 45))
        ScheduleResolver.columns(s).forEachIndexed { ci, col ->
            for (p in 1..6) {
                dao.upsertSlot(TimetableSlot(0, ids[(ci * 2 + p - 1) % ids.size], col.dayKey, p, room = "${100 + (p * 3 + ci) % 20}"))
            }
        }
        _messages.tryEmit("Example timetable added")
    }

    fun message(text: String) { _messages.tryEmit(text) }
}
