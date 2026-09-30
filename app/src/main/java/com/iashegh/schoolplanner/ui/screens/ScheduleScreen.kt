package com.iashegh.schoolplanner.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.iashegh.schoolplanner.MainViewModel
import com.iashegh.schoolplanner.data.DayPlan
import com.iashegh.schoolplanner.data.Exam
import com.iashegh.schoolplanner.data.Homework
import com.iashegh.schoolplanner.data.Lesson
import com.iashegh.schoolplanner.data.ScheduleResolver
import com.iashegh.schoolplanner.data.Settings
import com.iashegh.schoolplanner.data.Subject
import com.iashegh.schoolplanner.data.fmtMinute
import com.iashegh.schoolplanner.ui.components.CheckOrb
import com.iashegh.schoolplanner.ui.components.GlyphText
import com.iashegh.schoolplanner.ui.components.LocalFeedback
import com.iashegh.schoolplanner.ui.components.LongDate
import com.iashegh.schoolplanner.ui.components.Pill
import com.iashegh.schoolplanner.ui.components.ProgressRing
import com.iashegh.schoolplanner.ui.components.SectionTitle
import com.iashegh.schoolplanner.ui.components.SkinCard
import com.iashegh.schoolplanner.ui.components.SubjectBadge
import com.iashegh.schoolplanner.ui.components.enterAnim
import com.iashegh.schoolplanner.ui.components.mutedColor
import com.iashegh.schoolplanner.ui.theme.LocalSkin
import com.iashegh.schoolplanner.ui.theme.LocalSkinColors
import com.iashegh.schoolplanner.ui.theme.Skin
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(vm: MainViewModel, settings: Settings, now: LocalDateTime, onDaySwitch: () -> Unit) {
    val subjects by vm.subjects.collectAsStateCompat()
    val slots by vm.slots.collectAsStateCompat()
    val bells by vm.bells.collectAsStateCompat()
    val overrides by vm.overrides.collectAsStateCompat()
    val homework by vm.homework.collectAsStateCompat()
    val exams by vm.exams.collectAsStateCompat()
    val fb = LocalFeedback.current
    val colors = LocalSkinColors.current
    val scope = rememberCoroutineScope()

    val pager = rememberPagerState(pageCount = { 2 })
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.drop(1).collect {
            fb.whoosh()
            onDaySwitch()
        }
    }

    val today = now.toLocalDate()
    Column(Modifier.fillMaxSize()) {
        // Today / Tomorrow pill switch
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(Modifier.clip(CircleShape).background(colors.primary.copy(alpha = 0.15f)).padding(4.dp)) {
                listOf("Today", "Tomorrow").forEachIndexed { i, label ->
                    val selected = pager.currentPage == i
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (selected) colors.primary else Color.Transparent)
                            .clickable { fb.tap(); scope.launch { pager.animateScrollToPage(i) } }
                            .padding(horizontal = 26.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label, style = MaterialTheme.typography.titleSmall,
                            color = if (selected) (if (colors.dark) Color(0xFF00102A) else Color.White) else colors.onSurface,
                        )
                    }
                }
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val date = today.plusDays(page.toLong())
            val plan = remember(date, settings, subjects, slots, bells, overrides) {
                ScheduleResolver.resolve(date, settings, subjects, slots, bells, overrides)
            }
            DayPage(
                plan = plan, isToday = page == 0, now = now, settings = settings,
                subjects = subjects,
                homework = homework.filter { it.dueDate == date && !it.done },
                exams = exams.filter { it.date == date },
                onToggleHomework = { h ->
                    if (!h.done) fb.success() else fb.tap()
                    vm.toggleHomework(h)
                },
            )
        }
    }
}

@Composable
private fun DayPage(
    plan: DayPlan,
    isToday: Boolean,
    now: LocalDateTime,
    settings: Settings,
    subjects: List<Subject>,
    homework: List<Homework>,
    exams: List<Exam>,
    onToggleHomework: (Homework) -> Unit,
) {
    val colors = LocalSkinColors.current
    val nowMinute = now.hour * 60 + now.minute
    val subjectById = remember(subjects) { subjects.associateBy { it.id } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.date.format(LongDate), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                plan.label?.let { Pill(it, colors.accent) }
            }
        }

        if (!plan.isSchoolDay) {
            item { DayOffCard(plan.note) }
        } else {
            if (plan.note.isNotBlank()) item { Text(plan.note, color = colors.muted) }

            exams.forEach { exam ->
                item(key = "exam${exam.id}") {
                    val subject = subjectById[exam.subjectId]
                    SkinCard(Modifier.fillMaxWidth(), accent = Color(0xFFEF5350)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("!", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(end = 16.dp))
                            Column {
                                Text("EXAM: ${subject?.name ?: ""}", style = MaterialTheme.typography.titleMedium)
                                val extra = listOfNotNull(
                                    exam.startMinute.takeIf { it >= 0 }?.let { fmtMinute(it) },
                                    exam.room.takeIf { it.isNotBlank() }?.let { "Room $it" },
                                ).joinToString(" · ")
                                if (extra.isNotEmpty()) Text(extra, color = mutedColor())
                            }
                        }
                    }
                }
            }

            if (isToday) {
                val current = plan.lessons.firstOrNull { l ->
                    l.startMinute != null && l.endMinute != null && nowMinute >= l.startMinute && nowMinute < l.endMinute
                }
                val next = plan.lessons.firstOrNull { l -> l.startMinute != null && l.startMinute > nowMinute }
                if (current != null) item { LiveBanner(current, nowMinute, settings.decoderMode) }
                else if (next != null) item {
                    Text(
                        "Next: ${next.subject.name} in ${next.startMinute!! - nowMinute} min",
                        style = MaterialTheme.typography.titleSmall, color = colors.muted,
                    )
                }
            }

            if (plan.lessons.isEmpty()) {
                item {
                    SkinCard(Modifier.fillMaxWidth()) {
                        Text(
                            "No lessons in the timetable for this day yet.\nAsk a parent to fill it in.",
                            modifier = Modifier.padding(20.dp), textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            itemsIndexed(plan.lessons, key = { _, l -> "l${l.period}${l.subject.id}" }) { index, lesson ->
                val active = isToday && lesson.startMinute != null && lesson.endMinute != null &&
                    nowMinute >= lesson.startMinute && nowMinute < lesson.endMinute
                LessonCard(lesson, active, settings.decoderMode, Modifier.enterAnim(index))
            }

            if (homework.isNotEmpty()) {
                item { SectionTitle("Homework due") }
                items(homework, key = { "hw${it.id}" }) { h ->
                    val subject = subjectById[h.subjectId]
                    val c = Color(subject?.color ?: 0xFF888888.toInt())
                    SkinCard(Modifier.fillMaxWidth(), accent = c) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CheckOrb(h.done, if (LocalSkin.current == Skin.BUILDER) Color.White else c) { onToggleHomework(h) }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(h.title, style = MaterialTheme.typography.titleSmall)
                                Text(subject?.name ?: "", color = mutedColor(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, active: Boolean, decoder: Boolean, modifier: Modifier = Modifier) {
    val fb = LocalFeedback.current
    val colors = LocalSkinColors.current
    val accent = Color(lesson.subject.color)
    SkinCard(modifier.fillMaxWidth(), accent = accent, onClick = { fb.tap() }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(lesson.subject, 52.dp, decoder)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(lesson.subject.name, style = MaterialTheme.typography.titleMedium)
                if (decoder) GlyphText(lesson.subject.name, LocalContentColorOr(colors.onSurface), 18.dp)
                val details = listOfNotNull(
                    lesson.room.takeIf { it.isNotBlank() }?.let { "Room $it" },
                    lesson.teacher.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodyMedium, color = mutedColor())
            }
            Column(horizontalAlignment = Alignment.End) {
                if (active) Pill("NOW", colors.accent)
                if (lesson.startMinute != null && lesson.endMinute != null) {
                    Text(fmtMinute(lesson.startMinute), style = MaterialTheme.typography.titleSmall)
                    Text(fmtMinute(lesson.endMinute), style = MaterialTheme.typography.bodySmall, color = mutedColor())
                } else {
                    Text("P${lesson.period}", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun LocalContentColorOr(fallback: Color): Color = androidx.compose.material3.LocalContentColor.current.takeIf { it != Color.Unspecified } ?: fallback

@Composable
private fun LiveBanner(lesson: Lesson, nowMinute: Int, decoder: Boolean) {
    val colors = LocalSkinColors.current
    val start = lesson.startMinute!!
    val end = lesson.endMinute!!
    val progress = ((nowMinute - start).toFloat() / (end - start).coerceAtLeast(1)).coerceIn(0f, 1f)
    val remaining = end - nowMinute
    SkinCard(Modifier.fillMaxWidth(), accent = colors.accent) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress, Color(lesson.subject.color), 72.dp, track = LocalContentColorOr(colors.onSurface).copy(alpha = 0.15f)) {
                Text("$remaining", style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text("Happening now", style = MaterialTheme.typography.labelMedium, color = mutedColor())
                Text(lesson.subject.name, style = MaterialTheme.typography.headlineSmall)
                if (decoder) GlyphText(lesson.subject.name, LocalContentColorOr(colors.onSurface), 22.dp)
                Text("$remaining min left" + if (lesson.room.isNotBlank()) " · Room ${lesson.room}" else "", color = mutedColor())
            }
        }
    }
}

@Composable
private fun DayOffCard(note: String) {
    val skin = LocalSkin.current
    val bob = rememberInfiniteTransition(label = "bob").animateFloat(
        0.9f, 1.15f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "bob",
    )
    val emoji = when (skin) { Skin.GALACTIC -> "🚀"; Skin.BUILDER -> "🧱"; Skin.SCRIBE -> "🌀" }
    SkinCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 72.sp, modifier = Modifier.scale(bob.value))
            Text(
                "No school today — time to explore!",
                style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (note.isNotBlank()) Text(note, color = mutedColor(), modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
        }
    }
}
