package com.belge.goruntuleyici

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DeleteDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Silinsin mi?") },
        text = {
            Text(
                if (count == 1) {
                    "Seçili dosya kalıcı olarak silinecek. Bu işlem geri alınamaz."
                } else {
                    "$count dosya kalıcı olarak silinecek. Bu işlem geri alınamaz."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Sil") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

@Composable
fun RenameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeniden adlandır") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("Tamam") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

@Composable
fun DetailsDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayrıntılar") },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}
