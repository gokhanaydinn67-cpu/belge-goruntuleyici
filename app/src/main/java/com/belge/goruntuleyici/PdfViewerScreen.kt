package com.belge.goruntuleyici

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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

@Composable
fun PdfViewerScreen(file: DocFile, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val clipboard = LocalClipboardManager.current
    val doc = remember(file.path) { PdfDoc(file.path) }
    val index = remember(file.path) { PdfTextIndex(context, file.path) }
    DisposableEffect(doc) {
        onDispose {
            doc.close()
            index.close()
        }
    }
    val listState = rememberLazyListState()
    val zoom = remember { ZoomState() }

    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf<List<PdfMatch>>(emptyList()) }
    var matchTexts by remember { mutableStateOf<Map<Int, PdfPageText>>(emptyMap()) }
    var current by remember { mutableIntStateOf(0) }
    val matchesByPage = remember(matches) { matches.groupBy { it.page } }

    var sel by remember { mutableStateOf<PdfSel?>(null) }
    var selText by remember { mutableStateOf<PdfPageText?>(null) }
    val selState = rememberUpdatedState(sel)
    val textState = rememberUpdatedState(selText)

    fun clearSelection() {
        sel = null
        selText = null
    }

    val actions = remember {
        SelectionActions(
            onLongPress = { page, fx, fy ->
                scope.launch {
                    val t = withContext(Dispatchers.IO) { index.page(page) }
                    if (t.chars.isEmpty()) {
                        Toast.makeText(
                            context, "Bu sayfada seçilebilir yazı yok", Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val n = PdfSelect.nearest(t, fx, fy)
                        if (n.first >= 0 && n.second < 60f) {
                            val r = PdfSelect.wordRange(t, n.first)
                            selText = t
                            sel = PdfSel(page, r.first, r.second)
                        }
                    }
                }
            },
            onTapOutside = {
                sel = null
                selText = null
            },
            onChange = { sel = it }
        )
    }

    fun closeSearch() {
        searching = false
        query = ""
        matches = emptyList()
        matchTexts = emptyMap()
        current = 0
    }

    suspend fun goToMatch(m: PdfMatch) {
        listState.scrollToItem(m.page)
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == m.page }
        val t = matchTexts[m.page]
        if (info != null && t != null && t.chars.isNotEmpty()) {
            val c = t.chars[m.start.coerceIn(0, t.chars.size - 1)]
            listState.scrollBy(c.t * info.size - 160f)
        }
    }

    fun runSearch() {
        val q = query
        if (q.isBlank()) return
        Toast.makeText(context, "Aranıyor...", Toast.LENGTH_SHORT).show()
        scope.launch {
            val r = withContext(Dispatchers.IO) { PdfSearch.find(index, doc.count, q) }
            matches = r.matches
            matchTexts = r.texts
            current = 0
            if (r.matches.isEmpty()) {
                Toast.makeText(context, "Sonuç bulunamadı", Toast.LENGTH_SHORT).show()
            } else {
                goToMatch(r.matches[0])
            }
        }
    }

    fun move(d: Int) {
        if (matches.isEmpty()) return
        current = (current + d + matches.size) % matches.size
        val m = matches[current]
        scope.launch { goToMatch(m) }
    }

    fun share() {
        try {
            val uri = FileProvider.getUriForFile(
                context, "com.belge.goruntuleyici.fileprovider", File(file.path)
            )
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(i, "Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(context, "Paylaşılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = searching || sel != null) {
        if (sel != null) clearSelection() else closeSearch()
    }

    Scaffold(
        topBar = {
            PdfTopBar(
                title = file.name.substringBeforeLast('.'),
                searching = searching,
                query = query,
                resultText = if (matches.isEmpty()) "" else "${current + 1}/${matches.size}",
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
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFE0E0E0))
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pdfGestures(
                        wPx, hPx, density, listState, zoom, selState, textState, actions
                    )
                    .graphicsLayer {
                        scaleX = zoom.scale
                        scaleY = zoom.scale
                        translationX = zoom.offX
                        translationY = zoom.offY
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(count = doc.count) { i ->
                        PdfPageItem(
                            doc,
                            i,
                            widthPx,
                            sel,
                            selText,
                            matchesByPage[i] ?: emptyList(),
                            matchTexts[i],
                            matches.getOrNull(current)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${listState.firstVisibleItemIndex + 1} / ${doc.count}",
                    color = Color.White
                )
            }

            if (sel != null) {
                PdfSelectionBar(
                    onCopy = {
                        val s = sel
                        val t = selText
                        if (s != null && t != null) {
                            val txt = PdfSelect.selectedText(t, s.start, s.end)
                            clipboard.setText(AnnotatedString(txt))
                            Toast.makeText(context, "Kopyalandı", Toast.LENGTH_SHORT).show()
                            clearSelection()
                        }
                    },
                    onSelectAll = {
                        val s = sel
                        val t = selText
                        if (s != null && t != null && t.chars.isNotEmpty()) {
                            sel = PdfSel(s.page, 0, t.chars.size - 1)
                        }
                    },
                    onClose = { clearSelection() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                )
            }
        }
    }
}
