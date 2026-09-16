package ir.amir.parsping.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun AddDnsDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, primary: String, secondary: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var primary by remember { mutableStateOf("") }
    var secondary by remember { mutableStateOf("") }

    val ipRegex = remember {
        Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
    }
    val isValid = name.isNotBlank() && ipRegex.matches(primary.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن DNS دلخواه") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام") },
                    singleLine = true
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                OutlinedTextField(
                    value = primary,
                    onValueChange = { primary = it },
                    label = { Text("آی‌پی اصلی") },
                    placeholder = { Text("مثال: 1.1.1.1") },
                    singleLine = true
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                OutlinedTextField(
                    value = secondary,
                    onValueChange = { secondary = it },
                    label = { Text("آی‌پی دوم (اختیاری)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onConfirm(name.trim(), primary.trim(), secondary.trim().ifBlank { null })
                }
            ) { Text("افزودن") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
