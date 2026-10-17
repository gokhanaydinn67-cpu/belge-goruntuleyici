package com.belge.goruntuleyici

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatSize(b: Long): String {
    return when {
        b < 1024 -> "$b B"
        b < 1024 * 1024 -> "${b / 1024} KB"
        else -> String.format(Locale.US, "%.1f MB", b / 1048576.0)
    }
}

private fun formatDate(t: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(t))
}

private fun filterFiles(files: List<DocFile>, q: String): List<DocFile> {
    val s = q.trim()
    if (s.isEmpty()) return files
    if (s.startsWith(".")) {
        val ext = s.substring(1).lowercase()
        if (ext.isEmpty()) return files
        val exact = files.filter { it.ext == ext }
        return if (exact.isNotEmpty()) exact else files.filter { it.ext.startsWith(ext) }
    }
    return files.filter { it.name.contains(s, ignoreCase = true) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListScreen(
    files: List<DocFile>,
    listState: LazyListState,
    query: String,
    searching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchingChange: (Boolean) -> Unit,
    onOpen: (DocFile) -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    val shown = remember(files, query) { filterFiles(files, query) }

    fun toggle(path: String) {
        selected = if (path in selected) selected - path else selected + path
    }

    BackHandler(enabled = selected.isNotEmpty() || searching) {
        if (selected.isNotEmpty()) {
            selected = emptySet()
        } else {
            onSearchingChange(false)
            onQueryChange("")
        }
    }

    Scaffold(
        topBar = {
            FileListTopBar(
                searching = searching,
                query = query,
                selectedCount = selected.size,
                onQueryChange = onQueryChange,
                onStartSearch = { onSearchingChange(true) },
                onCloseSearch = {
                    onSearchingChange(false)
                    onQueryChange("")
                },
                onClearSelection = { selected = emptySet() },
                onShare = {
                    val picked = files.filter { it.path in selected }
                    ShareHelper.share(context, picked)
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (shown.isEmpty()) {
                Text(
                    text = "Dosya bulunamadı",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(count = shown.size, key = { shown[it].path }) { i ->
                        val f = shown[i]
                        val isSel = f.path in selected
                        val bg = if (isSel) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg)
                                .combinedClickable(
                                    onClick = {
                                        if (selected.isNotEmpty()) toggle(f.path) else onOpen(f)
                                    },
                                    onLongClick = { toggle(f.path) }
                                )
                                .padding(start = 16.dp, end = 28.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSel) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column {
                                Text(
                                    text = f.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = f.ext.uppercase() + "  •  " +
                                        formatSize(f.size) + "  •  " + formatDate(f.modified),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
                FastScroller(listState, Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}
