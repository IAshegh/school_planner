package com.iashegh.schoolplanner.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class Lesson(
    val period: Int,
    val subject: Subject,
    val room: String,
    val teacher: String,
    val startMinute: Int?,
    val endMinute: Int?,
)

data class DayPlan(
    val date: LocalDate,
    val isSchoolDay: Boolean,
    /** e.g. "Day 3" or "Week B" */
    val label: String?,
    /** holiday note / reason for a day off */
    val note: String,
    val lessons: List<Lesson>,
)

data class TimetableColumn(val dayKey: Int, val label: String)

fun fmtMinute(m: Int): String = String.format(Locale.US, "%02d:%02d", m / 60, m % 60)

fun DayOfWeek.short(): String = name.take(3).lowercase().replaceFirstChar { it.uppercase() }

object ScheduleResolver {

    /** Columns shown in the timetable editor / week grid for the current cycle setup. */
    fun columns(s: Settings): List<TimetableColumn> {
        val days = (0 until 7).map { s.firstDay.plus(it.toLong()) }.filter { s.isSchoolDay(it) }
        return when (s.cycleMode) {
            CycleMode.WEEKLY -> days.map { TimetableColumn(it.value, it.short()) }
            CycleMode.AB -> (0..1).flatMap { w ->
                days.map { TimetableColumn(w * 10 + it.value, "${'A' + w} ${it.short()}") }
            }
            CycleMode.ROTATING -> (1..s.cycleLength).map { TimetableColumn(it, "Day $it") }
        }
    }

    private fun weekStart(d: LocalDate, first: DayOfWeek): LocalDate =
        d.with(TemporalAdjusters.previousOrSame(first))

    /** Returns the timetable dayKey for [date], or null when it is not a normal school day. */
    fun dayKeyFor(date: LocalDate, s: Settings, holidays: Set<LocalDate>): Int? {
        if (!s.isSchoolDay(date.dayOfWeek) || date in holidays) return null
        return when (s.cycleMode) {
            CycleMode.WEEKLY -> date.dayOfWeek.value
            CycleMode.AB -> {
                val weeks = ChronoUnit.WEEKS.between(weekStart(s.anchor, s.firstDay), weekStart(date, s.firstDay))
                val w = Math.floorMod(weeks, 2L).toInt()
                w * 10 + date.dayOfWeek.value
            }
            CycleMode.ROTATING -> {
                val n = s.cycleLength.coerceAtLeast(2)
                val anchor = s.anchor
                fun isSchool(d: LocalDate) = s.isSchoolDay(d.dayOfWeek) && d !in holidays
                var count = 0
                if (date >= anchor) {
                    var d = anchor
                    while (d < date) { if (isSchool(d)) count++; d = d.plusDays(1) }
                    count % n + 1
                } else {
                    var d = date
                    while (d < anchor) { if (isSchool(d)) count++; d = d.plusDays(1) }
                    Math.floorMod(-count, n) + 1
                }
            }
        }
    }

    fun labelFor(dayKey: Int, s: Settings): String? = when (s.cycleMode) {
        CycleMode.WEEKLY -> null
        CycleMode.AB -> "Week ${'A' + dayKey / 10}"
        CycleMode.ROTATING -> "Day $dayKey"
    }

    fun resolve(
        date: LocalDate,
        s: Settings,
        subjects: List<Subject>,
        slots: List<TimetableSlot>,
        bells: List<BellPeriod>,
        overrides: List<DayOverride>,
    ): DayPlan {
        val byId = subjects.associateBy { it.id }
        val bellByPeriod = bells.associateBy { it.period }
        val override = overrides.firstOrNull { it.date == date }
        val holidays = overrides.filter { it.isHoliday }.map { it.date }.toSet()

        fun lesson(period: Int, subjectId: Long, room: String, teacher: String): Lesson? {
            val subject = byId[subjectId] ?: return null
            val b = bellByPeriod[period]
            return Lesson(period, subject, room, teacher, b?.startMinute, b?.endMinute)
        }

        if (override != null && override.isHoliday) {
            return DayPlan(date, false, null, override.note.ifBlank { "No school" }, emptyList())
        }
        if (!s.isSchoolDay(date.dayOfWeek)) {
            return DayPlan(date, false, null, "", emptyList())
        }
        val key = dayKeyFor(date, s, holidays)
        val label = key?.let { labelFor(it, s) }

        val replacement = override?.replacementJson?.let { parseReplacement(it) }
        val lessons = if (replacement != null) {
            replacement.mapNotNull { lesson(it.period, it.subjectId, it.room, "") }
        } else {
            slots.filter { it.dayKey == key }.mapNotNull { lesson(it.period, it.subjectId, it.room, it.teacher) }
        }.sortedBy { it.period }

        return DayPlan(date, true, label, override?.note.orEmpty(), lessons)
    }

    data class Replacement(val period: Int, val subjectId: Long, val room: String)

    fun parseReplacement(json: String): List<Replacement>? = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Replacement(o.getInt("p"), o.getLong("s"), o.optString("r"))
        }
    }.getOrNull()

    fun encodeReplacement(list: List<Replacement>): String {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("p", it.period).put("s", it.subjectId).put("r", it.room)) }
        return arr.toString()
    }
}
