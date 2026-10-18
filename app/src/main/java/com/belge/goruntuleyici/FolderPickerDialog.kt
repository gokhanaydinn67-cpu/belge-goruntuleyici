package com.belge.goruntuleyici

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun FolderPickerDialog(onPick: (File) -> Unit, onDismiss: () -> Unit) {
    val root = remember { Environment.getExternalStorageDirectory() }
    var dir by remember { mutableStateOf(root) }
    val subs = remember(dir) {
        (dir.listFiles() ?: emptyArray<File>())
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }
    }
    val atRoot = dir.absolutePath == root.absolutePath
    val shown = dir.absolutePath.removePrefix(root.absolutePath).ifEmpty { "/" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Klasör seç") },
        text = {
            Column {
                Text(
                    text = shown,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    if (!atRoot) {
                        item {
                            Text(
                                text = "⬆️  Üst klasör",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dir = dir.parentFile ?: root }
                                    .padding(vertical = 12.dp)
                            )
                            HorizontalDivider()
                        }
                    }
                    items(subs.size) { i ->
                        Text(
                            text = "📁  " + subs[i].name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dir = subs[i] }
                                .padding(vertical = 12.dp)
                        )
                        HorizontalDivider()
                    }
                    if (subs.isEmpty()) {
                        item {
                            Text(
                                text = "Alt klasör yok",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(dir) }) { Text("Buraya taşı") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
