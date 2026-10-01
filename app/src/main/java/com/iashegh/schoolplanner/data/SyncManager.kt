package com.iashegh.schoolplanner.data

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.iashegh.schoolplanner.alerts.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Keeps the parent's and the child's phones in step through Firestore.
 *
 *  - The PARENT phone owns the timetable (subjects, bells, slots, days off, calendar). Every change is pushed
 *    as one small JSON document; CHILD phones listen and apply it.
 *  - Homework and exams are edited on either phone: one Firestore document each, keyed by [Homework.syncId].
 *
 * Phones find each other through a secret family code (families/{code}).
 */
class SyncManager(
    private val app: Application,
    private val db: AppDatabase,
    private val settingsRepo: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private class Remote(val type: DocumentChange.Type, val id: String, val data: Map<String, Any>)

    private val dao = db.dao()
    private val mutex = Mutex()
    private val lock = Any()
    private var generation = 0
    private var role = SyncRole.NONE
    private var code: String? = null
    private var configReg: ListenerRegistration? = null
    private val plannerRegs = mutableListOf<ListenerRegistration>()
    private var pushJob: Job? = null
    private var lastPushed: String? = null

    private val _status = MutableStateFlow("Not linked")
    val status: StateFlow<String> = _status

    private fun fs() = FirebaseFirestore.getInstance()
    private fun family(code: String) = fs().collection("families").document(code)

    private suspend fun ensureAuth() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    // ---------------------------------------------------------------- linking

    suspend fun createFamily(): String {
        ensureAuth()
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = SecureRandom()
        val newCode = (1..10).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
        family(newCode).set(mapOf("created" to System.currentTimeMillis())).await()
        settingsRepo.setSync(SyncRole.PARENT, newCode)
        return newCode
    }

    suspend fun joinFamily(rawCode: String) {
        val c = rawCode.uppercase().filter { it.isLetterOrDigit() }
        require(c.length == 10) { "The code has 10 letters and numbers." }
        ensureAuth()
        val snap = family(c).get().await()
        require(snap.exists()) { "No family found with that code." }
        settingsRepo.setSync(SyncRole.CHILD, c)
    }

    suspend fun unlink() = settingsRepo.setSync(SyncRole.NONE, null)

    // ---------------------------------------------------------------- lifecycle

    private fun stop() {
        synchronized(lock) {
            generation++
            configReg?.remove(); configReg = null
            plannerRegs.forEach { it.remove() }; plannerRegs.clear()
        }
        pushJob?.cancel(); pushJob = null
        lastPushed = null
    }

    fun restart(newRole: SyncRole, newCode: String?) {
        stop()
        role = newRole
        code = newCode
        if (newRole == SyncRole.NONE || newCode == null) {
            _status.value = "Not linked"
            return
        }
        val gen = synchronized(lock) { generation }
        _status.value = "Connecting…"
        scope.launch(Dispatchers.IO) {
            try {
                ensureAuth()
            } catch (e: Exception) {
                _status.value = "Could not sign in: ${e.message}"
                return@launch
            }
            if (gen != synchronized(lock) { generation }) return@launch
            if (newRole == SyncRole.PARENT) {
                startPush(newCode)
                startPlannerListeners(newCode, gen)
                pushAllPlanner()
            } else {
                startConfigListener(newCode, gen)
            }
        }
    }

    private fun ok() {
        val t = LocalTime.now()
        val who = if (role == SyncRole.PARENT) "parent" else "child"
        _status.value = "Linked as $who phone · synced %02d:%02d".format(t.hour, t.minute)
    }

    private fun fail(e: Exception) { _status.value = "Sync problem: ${e.message ?: "unknown"}" }

    // ---------------------------------------------------------------- parent: push timetable

    @OptIn(FlowPreview::class)
    private fun startPush(code: String) {
        val calendarKey = settingsRepo.flow
            .map { listOf<Any>(it.firstDay, it.schoolDaysMask, it.cycleMode, it.cycleLength, it.anchorEpochDay) }
            .distinctUntilChanged()
        pushJob = scope.launch(Dispatchers.IO) {
            combine(dao.subjects(), dao.slots(), dao.bells(), dao.overrides(), calendarKey) { _, _, _, _, _ -> 0 }
                .debounce(1000)
                .collect {
                    val json = buildConfigJson()
                    if (json != lastPushed) {
                        try {
                            family(code).collection("sync").document("config")
                                .set(mapOf("json" to json, "updated" to System.currentTimeMillis())).await()
                            lastPushed = json
                            ok()
                        } catch (e: Exception) {
                            fail(e)
                        }
                    }
                }
        }
    }

    private suspend fun buildConfigJson(): String {
        val s = settingsRepo.flow.first()
        val root = JSONObject()
        root.put("calendar", JSONObject()
            .put("firstDay", s.firstDay.value).put("schoolDaysMask", s.schoolDaysMask)
            .put("cycleMode", s.cycleMode.name).put("cycleLength", s.cycleLength).put("anchor", s.anchorEpochDay))
        root.put("subjects", JSONArray().also { a ->
            dao.subjectsOnce().forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color).put("icon", it.icon)) }
        })
        root.put("bells", JSONArray().also { a ->
            dao.bellsOnce().forEach { a.put(JSONObject().put("period", it.period).put("start", it.startMinute).put("end", it.endMinute)) }
        })
        root.put("slots", JSONArray().also { a ->
            dao.slotsOnce().forEach {
                a.put(JSONObject().put("subjectId", it.subjectId).put("dayKey", it.dayKey).put("period", it.period)
                    .put("room", it.room).put("teacher", it.teacher))
            }
        })
        root.put("overrides", JSONArray().also { a ->
            dao.overridesOnce().forEach {
                a.put(JSONObject().put("date", it.date.toEpochDay()).put("holiday", it.isHoliday).put("note", it.note)
                    .put("replacement", it.replacementJson ?: JSONObject.NULL))
            }
        })
        return root.toString()
    }

    // ---------------------------------------------------------------- child: apply timetable

    private fun startConfigListener(code: String, gen: Int) {
        val reg = family(code).collection("sync").document("config").addSnapshotListener { snap, err ->
            if (err != null) { fail(err); return@addSnapshotListener }
            val json = snap?.getString("json")
            if (json == null) { _status.value = "Linked. Waiting for the parent's phone to share the timetable…"; return@addSnapshotListener }
            scope.launch(Dispatchers.IO) {
                try {
                    applyConfig(json)
                    if (gen == synchronized(lock) { generation }) {
                        startPlannerListeners(code, gen)
                        pushAllPlanner()
                    }
                    ok()
                } catch (e: Exception) {
                    fail(e)
                }
            }
        }
        synchronized(lock) { if (gen == generation) configReg = reg else reg.remove() }
    }

    private suspend fun applyConfig(json: String) = mutex.withLock {
        val root = JSONObject(json)
        db.tx {
            val subjects = root.getJSONArray("subjects").objects().map {
                Subject(it.getLong("id"), it.getString("name"), it.getInt("color"), it.optString("icon", "star"))
            }
            if (subjects.isEmpty()) dao.clearSubjects() else dao.deleteSubjectsNotIn(subjects.map { it.id })
            subjects.forEach { dao.upsertSubject(it) }

            dao.clearBells()
            root.getJSONArray("bells").objects().forEach { dao.upsertBell(BellPeriod(it.getInt("period"), it.getInt("start"), it.getInt("end"))) }

            dao.clearSlots()
            root.getJSONArray("slots").objects().forEach {
                dao.upsertSlot(TimetableSlot(0, it.getLong("subjectId"), it.getInt("dayKey"), it.getInt("period"), it.optString("room"), it.optString("teacher")))
            }

            dao.clearOverrides()
            root.getJSONArray("overrides").objects().forEach {
                dao.upsertOverride(DayOverride(
                    LocalDate.ofEpochDay(it.getLong("date")), it.optBoolean("holiday", true), it.optString("note"),
                    if (it.isNull("replacement")) null else it.getString("replacement"),
                ))
            }
        }
        root.optJSONObject("calendar")?.let { c ->
            settingsRepo.setCalendar(
                DayOfWeek.of(c.getInt("firstDay")), c.getInt("schoolDaysMask"),
                CycleMode.valueOf(c.getString("cycleMode")), c.getInt("cycleLength"), LocalDate.ofEpochDay(c.getLong("anchor")),
            )
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    // ---------------------------------------------------------------- both: homework + exams

    private fun startPlannerListeners(code: String, gen: Int) {
        val hw = family(code).collection("homework").addSnapshotListener { snap, err ->
            if (err != null) { fail(err); return@addSnapshotListener }
            val changes = snap?.documentChanges.orEmpty()
                .filter { !it.document.metadata.hasPendingWrites() }
                .map { Remote(it.type, it.document.id, it.document.data) }
            if (changes.isNotEmpty()) scope.launch(Dispatchers.IO) { applyHomework(changes); ok() }
        }
        val ex = family(code).collection("exams").addSnapshotListener { snap, err ->
            if (err != null) { fail(err); return@addSnapshotListener }
            val changes = snap?.documentChanges.orEmpty()
                .filter { !it.document.metadata.hasPendingWrites() }
                .map { Remote(it.type, it.document.id, it.document.data) }
            if (changes.isNotEmpty()) scope.launch(Dispatchers.IO) { applyExams(changes); ok() }
        }
        synchronized(lock) {
            if (gen != generation) { hw.remove(); ex.remove(); return }
            // a fresh listener replays everything, so drop the old ones first
            plannerRegs.forEach { it.remove() }
            plannerRegs.clear()
            plannerRegs += hw
            plannerRegs += ex
        }
    }

    private suspend fun applyHomework(changes: List<Remote>) = mutex.withLock {
        val subjectIds = dao.subjectsOnce().map { it.id }.toSet()
        for (c in changes) {
            try {
                if (c.type == DocumentChange.Type.REMOVED) { dao.deleteHomeworkBySyncId(c.id); continue }
                val d = c.data
                val subjectId = (d["subjectId"] as? Number)?.toLong() ?: continue
                if (subjectId !in subjectIds) continue
                val existing = dao.homeworkBySyncId(c.id)
                dao.upsertHomework(Homework(
                    id = existing?.id ?: 0,
                    subjectId = subjectId,
                    title = d["title"] as? String ?: "",
                    notes = d["notes"] as? String ?: "",
                    dueDate = LocalDate.ofEpochDay((d["due"] as Number).toLong()),
                    priority = (d["priority"] as? Number)?.toInt() ?: 0,
                    done = d["done"] as? Boolean ?: false,
                    doneDate = (d["doneDate"] as? Number)?.let { LocalDate.ofEpochDay(it.toLong()) },
                    photoUri = existing?.photoUri,
                    syncId = c.id,
                ))
            } catch (_: Exception) {
                // a single bad document must not stop the rest
            }
        }
    }

    private suspend fun applyExams(changes: List<Remote>) = mutex.withLock {
        val subjects = dao.subjectsOnce().associate { it.id to it.name }
        for (c in changes) {
            try {
                if (c.type == DocumentChange.Type.REMOVED) {
                    dao.examBySyncId(c.id)?.let { ReminderScheduler.cancelExam(app, it.id) }
                    dao.deleteExamBySyncId(c.id)
                    continue
                }
                val d = c.data
                val subjectId = (d["subjectId"] as? Number)?.toLong() ?: continue
                if (subjectId !in subjects) continue
                val existing = dao.examBySyncId(c.id)
                val exam = Exam(
                    id = existing?.id ?: 0,
                    subjectId = subjectId,
                    title = d["title"] as? String ?: "",
                    date = LocalDate.ofEpochDay((d["date"] as Number).toLong()),
                    startMinute = (d["start"] as? Number)?.toInt() ?: -1,
                    room = d["room"] as? String ?: "",
                    topics = d["topics"] as? String ?: "",
                    notes = d["notes"] as? String ?: "",
                    syncId = c.id,
                )
                val id = dao.upsertExam(exam)
                val saved = exam.copy(id = if (exam.id == 0L) id else exam.id)
                ReminderScheduler.cancelExam(app, saved.id)
                ReminderScheduler.scheduleExam(app, saved, subjects[subjectId] ?: "Exam")
            } catch (_: Exception) {
            }
        }
    }

    // ---------------------------------------------------------------- local edits -> cloud

    /** Uploads everything already on this phone (idempotent: documents are keyed by syncId). */
    private suspend fun pushAllPlanner() {
        dao.homeworkOnce().forEach { pushHomework(it) }
        dao.examsOnce().forEach { pushExam(it) }
    }

    private fun send(collection: String, id: String, data: Map<String, Any?>?) {
        val c = code ?: return
        if (role == SyncRole.NONE) return
        val ref = family(c).collection(collection).document(id)
        val task = if (data == null) ref.delete() else ref.set(data)
        task.addOnFailureListener { fail(it) }
    }

    fun pushHomework(h: Homework) = send("homework", h.syncId, mapOf(
        "subjectId" to h.subjectId, "title" to h.title, "notes" to h.notes, "due" to h.dueDate.toEpochDay(),
        "priority" to h.priority, "done" to h.done, "doneDate" to h.doneDate?.toEpochDay(),
    ))

    fun deleteHomework(h: Homework) = send("homework", h.syncId, null)

    fun pushExam(e: Exam) = send("exams", e.syncId, mapOf(
        "subjectId" to e.subjectId, "title" to e.title, "date" to e.date.toEpochDay(), "start" to e.startMinute,
        "room" to e.room, "topics" to e.topics, "notes" to e.notes,
    ))

    fun deleteExam(e: Exam) = send("exams", e.syncId, null)
}
