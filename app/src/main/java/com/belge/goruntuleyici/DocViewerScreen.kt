package com.belge.goruntuleyici

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun mimeFor(ext: String): String {
    return when (ext) {
        "docx", "docm", "dotx" ->
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "odt" -> "application/vnd.oasis.opendocument.text"
        "rtf" -> "application/rtf"
        else -> "*/*"
    }
}

@Composable
fun DocViewerScreen(file: DocFile, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val loaded by produceState<List<DocBlock>?>(null, file.path) {
        value = withContext(Dispatchers.IO) {
            try {
                DocLoader.load(file.path, file.ext)
            } catch (e: Throwable) {
                emptyList()
            }
        }
    }
    val blocks = loaded ?: emptyList()
    val texts = remember(blocks) { blocks.map { it.text() } }

    var zoom by remember { mutableFloatStateOf(1f) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var activeQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Int>>(emptyList()) }
    var current by remember { mutableIntStateOf(0) }

    fun closeSearch() {
        searching = false
        query = ""
        activeQuery = ""
        results = emptyList()
        current = 0
    }

    fun runSearch() {
        val q = query
        if (q.isBlank()) return
        scope.launch {
            val r = withContext(Dispatchers.Default) {
                texts.indices.filter { texts[it].contains(q, ignoreCase = true) }
            }
            activeQuery = q
            results = r
            current = 0
            if (r.isEmpty()) {
                Toast.makeText(context, "Sonuç bulunamadı", Toast.LENGTH_SHORT).show()
            } else {
                listState.scrollToItem(r[0])
            }
        }
    }

    fun move(d: Int) {
        if (results.isEmpty()) return
        current = (current + d + results.size) % results.size
        scope.launch { listState.scrollToItem(results[current]) }
    }

    fun share() {
        try {
            val uri = FileProvider.getUriForFile(
                context, "com.belge.goruntuleyici.fileprovider", File(file.path)
            )
            val i = Intent(Intent.ACTION_SEND).apply {
                type = mimeFor(file.ext)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(i, "Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(context, "Paylaşılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = searching) { closeSearch() }

    Scaffold(
        topBar = {
            PdfTopBar(
                title = file.name.substringBeforeLast('.'),
                searching = searching,
                query = query,
                resultText = if (results.isEmpty()) "" else "${current + 1}/${results.size}",
                onQueryChange = { query = it },
                onSubmit = { runSearch() },
                onBack = onBack,
                onStartSearch = { searching = true },
                onCloseSearch = { closeSearch() },
                onPrev = { move(-1) },
                onNext = { move(1) },
                onShare = { share() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.size >= 2) {
                                val z = event.calculateZoom()
                                zoom = (zoom * z).coerceIn(0.6f, 3f)
                                event.changes.forEach {
                                    if (it.positionChanged()) it.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            if (loaded == null) {
                Text("Yükleniyor...", color = Color.Black, modifier = Modifier.align(Alignment.Center))
            } else if (blocks.isEmpty()) {
                Text(
                    "Belge açılamadı veya boş",
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 24.dp)
                    ) {
                        items(blocks.size) { i ->
                            BlockView(
                                blocks[i],
                                zoom,
                                activeQuery,
                                results.getOrNull(current) == i
                            )
                        }
                    }
                }
                FastScroller(listState, Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}
