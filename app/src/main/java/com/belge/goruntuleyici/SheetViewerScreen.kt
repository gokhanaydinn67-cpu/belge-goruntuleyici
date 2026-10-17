package com.belge.goruntuleyici

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private class Hit(val sheet: Int, val row: Int, val col: Int)

private fun sheetMime(ext: String): String {
    return when (ext) {
        "xlsx", "xlsm", "xltx" ->
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ods" -> "application/vnd.oasis.opendocument.spreadsheet"
        else -> "*/*"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetViewerScreen(file: DocFile, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val clipboard = LocalClipboardManager.current

    val loaded by produceState<List<SheetData>?>(null, file.path) {
        value = withContext(Dispatchers.IO) { SheetLoader.load(file.path, file.ext) }
    }
    val sheets = loaded ?: emptyList()

    var sheetIndex by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var activeQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Hit>>(emptyList()) }
    var current by remember { mutableIntStateOf(0) }

    val safeIndex = if (sheets.isEmpty()) 0 else sheetIndex.coerceIn(0, sheets.size - 1)
    val listState = remember(safeIndex) { LazyListState() }
    val hScroll = remember(safeIndex) { ScrollState(0) }

    LaunchedEffect(results, current, safeIndex) {
        val h = results.getOrNull(current)
        if (h != null && h.sheet == safeIndex && safeIndex < sheets.size) {
            val widths = sheetColWidths(sheets[safeIndex])
            var dp = 44f
            for (c in 0 until h.col) dp += widths.getOrElse(c) { 64 }
            listState.scrollToItem(h.row)
            hScroll.scrollTo((dp * zoom * density).toInt())
        }
    }

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
                val out = ArrayList<Hit>()
                loop@ for ((si, s) in sheets.withIndex()) {
                    for ((ri, row) in s.rows.withIndex()) {
                        for ((ci, t) in row.withIndex()) {
                            if (t.contains(q, ignoreCase = true)) {
                                out.add(Hit(si, ri, ci))
                                if (out.size >= 2000) break@loop
                            }
                        }
                    }
                }
                out
            }
            activeQuery = q
            results = r
            current = 0
            if (r.isEmpty()) {
                Toast.makeText(context, "Sonuç bulunamadı", Toast.LENGTH_SHORT).show()
            } else {
                sheetIndex = r[0].sheet
            }
        }
    }

    fun move(d: Int) {
        if (results.isEmpty()) return
        current = (current + d + results.size) % results.size
        sheetIndex = results[current].sheet
    }

    fun share() {
        try {
            val uri = FileProvider.getUriForFile(
                context, "com.belge.goruntuleyici.fileprovider", File(file.path)
            )
            val i = Intent(Intent.ACTION_SEND).apply {
                type = sheetMime(file.ext)
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
        },
        bottomBar = {
            if (sheets.size > 1) {
                ScrollableTabRow(selectedTabIndex = safeIndex, edgePadding = 0.dp) {
                    sheets.forEachIndexed { i, s ->
                        Tab(
                            selected = i == safeIndex,
                            onClick = { sheetIndex = i },
                            text = { Text(s.name, maxLines = 1) }
                        )
                    }
                }
            }
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
                                zoom = (zoom * z).coerceIn(0.6f, 2.5f)
                                event.changes.forEach {
                                    if (it.positionChanged()) it.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            if (loaded == null) {
                Text(
                    "Yükleniyor...",
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (sheets.isEmpty()) {
                Text(
                    "Dosya açılamadı veya boş",
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val h = results.getOrNull(current)
                val onThis = h != null && h.sheet == safeIndex
                SheetGrid(
                    sheet = sheets[safeIndex],
                    zoom = zoom,
                    query = activeQuery,
                    focusRow = if (onThis && h != null) h.row else -1,
                    focusCol = if (onThis && h != null) h.col else -1,
                    listState = listState,
                    hScroll = hScroll,
                    onCopy = {
                        clipboard.setText(AnnotatedString(it))
                        Toast.makeText(context, "Kopyalandı", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxSize()
                )
                FastScroller(listState, Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}
