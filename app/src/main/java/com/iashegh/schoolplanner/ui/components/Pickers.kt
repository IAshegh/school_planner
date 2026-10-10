package com.iashegh.schoolplanner.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.filter
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val RowHeight = 48.dp

/**
 * Spinner column: three rows visible, the middle one is selected, scrolling snaps row by row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(items: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, width: Dp) {
    val last = (items.size - 1).coerceAtLeast(0)
    val rowPx = with(LocalDensity.current) { RowHeight.roundToPx() }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, last))
    val fling = rememberSnapFlingBehavior(state)
    val callback by rememberUpdatedState(onSelected)
    val center by remember(rowPx) {
        derivedStateOf { state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > rowPx / 2) 1 else 0 }
    }

    LaunchedEffect(state, last) {
        snapshotFlow { state.isScrollInProgress }.filter { !it }.collect { callback(center.coerceIn(0, last)) }
    }
    LaunchedEffect(selectedIndex, last) {
        if (!state.isScrollInProgress && center != selectedIndex.coerceIn(0, last)) {
            state.scrollToItem(selectedIndex.coerceIn(0, last))
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.width(width).height(RowHeight * 3)) {
        LazyColumn(state = state, flingBehavior = fling) {
            item { Spacer(Modifier.height(RowHeight)) }
            itemsIndexed(items) { i, text ->
                val selected = i == center
                Box(Modifier.fillMaxWidth().height(RowHeight), contentAlignment = Alignment.Center) {
                    Text(
                        text,
                        style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 1f else 0.4f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item { Spacer(Modifier.height(RowHeight)) }
        }
        // the two accent lines around the selected row
        Box(Modifier.fillMaxWidth().height(2.dp).offset(y = RowHeight).background(accent))
        Box(Modifier.fillMaxWidth().height(2.dp).offset(y = RowHeight * 2).background(accent))
    }
}

@Composable
fun DatePickDialog(initial: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val now = LocalDate.now()
    val firstYear = minOf(initial.year, now.year) - 1
    val lastYear = maxOf(initial.year, now.year) + 5
    val years = remember { (firstYear..lastYear).toList() }
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.monthValue) }
    var day by remember { mutableIntStateOf(initial.dayOfMonth) }
    val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
    val safeDay = day.coerceAtMost(daysInMonth)
    val picked = LocalDate.of(year, month, safeDay)
    val accent = MaterialTheme.colorScheme.primary
    val months = remember { Month.values().map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) } }
    val days = remember(daysInMonth) { (1..daysInMonth).map { it.toString() } }
    val yearLabels = remember { years.map { it.toString() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(picked.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())), color = accent)
        },
        text = {
            Column {
                Box(Modifier.fillMaxWidth().height(2.dp).background(accent))
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelPicker(months, month - 1, { month = it + 1 }, 84.dp)
                    WheelPicker(days, safeDay - 1, { day = it + 1 }, 64.dp)
                    WheelPicker(yearLabels, year - firstYear, { year = firstYear + it }, 84.dp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(picked) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** 24-hour clock-face time picker (hour dial, then minute dial), coloured from the current theme. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(initialMinute: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinute / 60, initialMinute = initialMinute % 60, is24Hour = true)
    val cs = MaterialTheme.colorScheme
    val colors = TimePickerDefaults.colors(
        clockDialColor = cs.primary.copy(alpha = 0.15f),
        selectorColor = cs.primary,
        clockDialSelectedContentColor = cs.onPrimary,
        clockDialUnselectedContentColor = cs.onSurface,
        timeSelectorSelectedContainerColor = cs.primary.copy(alpha = 0.3f),
        timeSelectorUnselectedContainerColor = cs.onSurface.copy(alpha = 0.1f),
        timeSelectorSelectedContentColor = cs.onSurface,
        timeSelectorUnselectedContentColor = cs.onSurface,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp),
        text = {
            Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                TimePicker(state = state, colors = colors)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
