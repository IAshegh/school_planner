package com.iashegh.schoolplanner.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/** Human-readable JSON backup of everything except the PIN. Photos are referenced by URI only. */
object Backup {
    private const val VERSION = 1

    suspend fun export(db: AppDatabase, settings: Settings): String {
        val dao = db.dao()
        val root = JSONObject()
        root.put("app", "school_planner")
        root.put("version", VERSION)

        root.put("calendar", JSONObject()
            .put("firstDay", settings.firstDay.value)
            .put("schoolDaysMask", settings.schoolDaysMask)
            .put("cycleMode", settings.cycleMode.name)
            .put("cycleLength", settings.cycleLength)
            .put("anchor", settings.anchorEpochDay))

        root.put("subjects", JSONArray().also { a ->
            dao.subjectsOnce().forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color).put("icon", it.icon))
            }
        })
        root.put("bells", JSONArray().also { a ->
            dao.bellsOnce().forEach {
                a.put(JSONObject().put("period", it.period).put("start", it.startMinute).put("end", it.endMinute))
            }
        })
        root.put("slots", JSONArray().also { a ->
            dao.slotsOnce().forEach {
                a.put(JSONObject().put("subjectId", it.subjectId).put("dayKey", it.dayKey).put("period", it.period)
                    .put("room", it.room).put("teacher", it.teacher))
            }
        })
        root.put("homework", JSONArray().also { a ->
            dao.homeworkOnce().forEach {
                a.put(JSONObject().put("subjectId", it.subjectId).put("title", it.title).put("notes", it.notes)
                    .put("due", it.dueDate.toEpochDay()).put("priority", it.priority).put("done", it.done)
                    .put("doneDate", it.doneDate?.toEpochDay() ?: JSONObject.NULL)
                    .put("photoUri", it.photoUri ?: JSONObject.NULL))
            }
        })
        root.put("exams", JSONArray().also { a ->
            dao.examsOnce().forEach {
                a.put(JSONObject().put("subjectId", it.subjectId).put("title", it.title).put("date", it.date.toEpochDay())
                    .put("start", it.startMinute).put("room", it.room).put("topics", it.topics).put("notes", it.notes))
            }
        })
        root.put("overrides", JSONArray().also { a ->
            dao.overridesOnce().forEach {
                a.put(JSONObject().put("date", it.date.toEpochDay()).put("holiday", it.isHoliday).put("note", it.note)
                    .put("replacement", it.replacementJson ?: JSONObject.NULL))
            }
        })
        return root.toString(2)
    }

    /** Replaces all data. Throws on malformed input, leaving the database untouched (single transaction). */
    suspend fun import(db: AppDatabase, settingsRepo: SettingsRepository, json: String) {
        val root = JSONObject(json)
        require(root.optString("app") == "school_planner") { "This is not a School Planner backup file." }
        val dao = db.dao()

        db.tx {
            dao.clearHomework(); dao.clearExams(); dao.clearOverrides(); dao.clearSlots(); dao.clearBells(); dao.clearSubjects()

            root.getJSONArray("subjects").objects().forEach {
                dao.upsertSubject(Subject(it.getLong("id"), it.getString("name"), it.getInt("color"), it.optString("icon", "star")))
            }
            root.getJSONArray("bells").objects().forEach {
                dao.upsertBell(BellPeriod(it.getInt("period"), it.getInt("start"), it.getInt("end")))
            }
            root.getJSONArray("slots").objects().forEach {
                dao.upsertSlot(TimetableSlot(0, it.getLong("subjectId"), it.getInt("dayKey"), it.getInt("period"),
                    it.optString("room"), it.optString("teacher")))
            }
            root.getJSONArray("homework").objects().forEach {
                dao.upsertHomework(Homework(
                    subjectId = it.getLong("subjectId"), title = it.getString("title"), notes = it.optString("notes"),
                    dueDate = LocalDate.ofEpochDay(it.getLong("due")), priority = it.optInt("priority"),
                    done = it.optBoolean("done"),
                    doneDate = if (it.isNull("doneDate")) null else LocalDate.ofEpochDay(it.getLong("doneDate")),
                    photoUri = if (it.isNull("photoUri")) null else it.getString("photoUri"),
                ))
            }
            root.getJSONArray("exams").objects().forEach {
                dao.upsertExam(Exam(
                    subjectId = it.getLong("subjectId"), title = it.optString("title"),
                    date = LocalDate.ofEpochDay(it.getLong("date")), startMinute = it.optInt("start", -1),
                    room = it.optString("room"), topics = it.optString("topics"), notes = it.optString("notes"),
                ))
            }
            root.getJSONArray("overrides").objects().forEach {
                dao.upsertOverride(DayOverride(
                    date = LocalDate.ofEpochDay(it.getLong("date")), isHoliday = it.optBoolean("holiday", true),
                    note = it.optString("note"),
                    replacementJson = if (it.isNull("replacement")) null else it.getString("replacement"),
                ))
            }
        }

        root.optJSONObject("calendar")?.let { c ->
            settingsRepo.setCalendar(
                firstDay = DayOfWeek.of(c.getInt("firstDay")),
                mask = c.getInt("schoolDaysMask"),
                mode = CycleMode.valueOf(c.getString("cycleMode")),
                length = c.getInt("cycleLength"),
                anchor = LocalDate.ofEpochDay(c.getLong("anchor")),
            )
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
