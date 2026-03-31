package app.vitriol.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
internal fun SettingsLockDialog(
    settingPin: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DialogTitle(settingPin)
                Spacer(modifier = Modifier.height(16.dp))

                PinInputField(
                    value = pin,
                    isError = error.isNotEmpty(),
                    onValueChange = {
                        // Logic: Only allow digits and max 6 chars
                        if (it.all { char -> char.isDigit() } && it.length <= 6) {
                            pin = it
                            error = ""
                        }
                    }
                )

                if (settingPin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PinInputField(
                        label = "Confirm PIN",
                        value = confirmPin,
                        isError = error.isNotEmpty(),
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 6) {
                                confirmPin = it
                                error = ""
                            }
                        },
                    )
                }

                if (error.isNotEmpty()) {
                    ErrorText(error)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                DialogButtons(
                    onDismiss = onDismiss,
                    onConfirmClick = {
                        val validationError = when {
                            pin.length < 4 -> "PIN must be at least 4 digits"
                            settingPin && pin != confirmPin -> "PINs do not match"
                            else -> null
                        }

                        if (validationError != null) {
                            error = validationError
                        } else {
                            onConfirm(pin)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DialogTitle(settingPin: Boolean) {
    Text(
        text = if (settingPin) "Set PIN" else "Enter PIN",
        style = MaterialTheme.typography.headlineSmall,
    )
}


@Composable
private fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    label: String = "PIN",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError, // Highlighting the field in red
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorText(error: String) {
    Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun DialogButtons(
    onDismiss: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
        Button(
            onClick = onConfirmClick,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text("Confirm")
        }
    }
}
