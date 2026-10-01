package com.iashegh.schoolplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iashegh.schoolplanner.MainViewModel
import com.iashegh.schoolplanner.data.Settings
import com.iashegh.schoolplanner.data.SyncRole
import com.iashegh.schoolplanner.ui.components.ScreenHeader
import com.iashegh.schoolplanner.ui.components.SectionTitle
import com.iashegh.schoolplanner.ui.components.SkinCard
import com.iashegh.schoolplanner.ui.components.mutedColor
import com.iashegh.schoolplanner.ui.theme.LocalSkinColors
import kotlinx.coroutines.launch

@Composable
fun SyncScreen(vm: MainViewModel, settings: Settings, onBack: () -> Unit) {
    val status by vm.syncStatus.collectAsStateCompat()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var joinCode by remember { mutableStateOf("") }
    var confirmUnlink by remember { mutableStateOf(false) }
    val muted = LocalSkinColors.current.muted

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Sync phones", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (settings.syncRole) {
                SyncRole.NONE -> {
                    Text(
                        "Keep the timetable, homework and exams the same on the parent's and the child's phone. " +
                            "Both phones need internet now and then.",
                        color = muted,
                    )
                    SectionTitle("This is the parent's phone")
                    Text("It creates a family code. The timetable is edited here and sent to the child's phone.", color = muted)
                    Button(
                        enabled = !busy,
                        onClick = {
                            busy = true; error = null
                            scope.launch {
                                try { vm.createFamily() } catch (e: Exception) { error = e.message ?: "Something went wrong" }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (busy) "Please wait…" else "Create family code") }

                    SectionTitle("This is the child's phone")
                    Text("Type the family code that is shown on the parent's phone.", color = muted)
                    OutlinedTextField(
                        joinCode, { joinCode = it.uppercase().filter(Char::isLetterOrDigit).take(10) },
                        label = { Text("Family code") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        enabled = !busy && joinCode.length == 10,
                        onClick = {
                            busy = true; error = null
                            scope.launch {
                                try { vm.joinFamily(joinCode) } catch (e: Exception) { error = e.message ?: "Something went wrong" }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Link this phone") }
                    Text(
                        "Linking a child's phone replaces its subjects and timetable with the parent's. " +
                            "Homework on subjects that don't exist on the parent's phone is removed.",
                        style = MaterialTheme.typography.bodySmall, color = muted,
                    )
                }

                SyncRole.PARENT -> {
                    Text("This is the parent's phone. Enter this code on the child's phone:", color = muted)
                    SkinCard(Modifier.fillMaxWidth()) {
                        Text(
                            settings.familyCode.orEmpty().chunked(5).joinToString("  "),
                            style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                        )
                    }
                    Text("Keep the code private: anyone who has it can see and change the schedule.", color = muted, style = MaterialTheme.typography.bodySmall)
                }

                SyncRole.CHILD -> {
                    Text("This phone follows the parent's timetable. Homework and exams you add show up on the parent's phone too.", color = muted)
                }
            }

            if (settings.syncRole != SyncRole.NONE) {
                Text(status, style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = { confirmUnlink = true }, modifier = Modifier.fillMaxWidth()) { Text("Stop syncing on this phone") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("", Modifier.padding(bottom = 32.dp))
        }
    }

    if (confirmUnlink) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmUnlink = false },
            title = { Text("Stop syncing?") },
            text = { Text("Everything stays on this phone, but changes will no longer be shared.") },
            confirmButton = { Button(onClick = { vm.unlinkSync(); confirmUnlink = false }) { Text("Stop syncing") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmUnlink = false }) { Text("Keep") } },
        )
    }
}
