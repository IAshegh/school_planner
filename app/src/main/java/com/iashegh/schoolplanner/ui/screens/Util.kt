package com.iashegh.schoolplanner.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime

@Composable
fun <T> StateFlow<T>.collectAsStateCompat(): State<T> = collectAsState()

/** Current date-time, refreshed every 20 seconds so the live lesson banner stays accurate. */
@Composable
fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        delay(20_000)
        value = LocalDateTime.now()
    }
}
