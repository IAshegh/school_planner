package com.iashegh.schoolplanner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.iashegh.schoolplanner.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun CreatePinDialog(title: String = "Create a parent PIN", onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val valid = pin.length in 4..8 && pin == again
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("4 to 8 digits. This locks the timetable and other parent settings.")
                OutlinedTextField(
                    pin, { pin = it.filter(Char::isDigit).take(8) }, label = { Text("PIN") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    again, { again = it.filter(Char::isDigit).take(8) }, label = { Text("Repeat PIN") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = { Button(enabled = valid, onClick = { onDone(pin) }) { Text("Save PIN") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun EnterPinDialog(vm: MainViewModel, onSuccess: () -> Unit, onForgot: () -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun submit() {
        scope.launch {
            if (vm.verifyPin(pin)) onSuccess() else { wrong = true; pin = "" }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Parent PIN") },
        text = {
            Column {
                OutlinedTextField(
                    pin, { pin = it.filter(Char::isDigit).take(8); wrong = false }, label = { Text("Enter PIN") }, singleLine = true,
                    isError = wrong, supportingText = { if (wrong) Text("That PIN is not right") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onForgot) { Text("Forgot PIN?") }
            }
        },
        confirmButton = { Button(enabled = pin.length >= 4, onClick = { submit() }) { Text("Unlock") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Forgetting the PIN means starting over, so it needs a deliberate typed confirmation. */
@Composable
fun ConfirmResetDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Erase everything?") },
        text = {
            Column {
                Text(message)
                OutlinedTextField(
                    typed, { typed = it }, label = { Text("Type RESET to confirm") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
        confirmButton = { Button(enabled = typed == "RESET", onClick = onConfirm) { Text("Erase all") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
