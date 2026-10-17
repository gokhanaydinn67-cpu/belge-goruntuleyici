package com.belge.goruntuleyici

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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

private fun deckMime(ext: String): String {
    return when (ext) {
        "pptx", "pptm", "ppsx", "potx" ->
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "odp" -> "application/vnd.oasis.opendocument.presentation"
        else -> "*/*"
    }
}

@Composable
fun PptViewerScreen(file: DocFile, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val loaded by produceState<Deck?>(null, file.path) {
        value = withContext(Dispatchers.IO) { DeckLoader.load(file.path, file.ext) }
    }
    val deck = loaded
    val slides = deck?.slides ?: emptyList()

    var scale by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    var offY by remember { mutableFloatStateOf(0f) }

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
                slides.indices.filter { slides[it].text.contains(q, ignoreCase = true) }
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
                type = deckMime(file.ext)
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
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFE0E0E0))
        ) {
            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()

            if (deck == null) {
                Text(
                    "Yükleniyor...",
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (slides.isEmpty()) {
                Text(
                    "Sunum boş veya açılamadı",
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(wPx, hPx) {
                            awaitEachGesture {
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val multi = event.changes.size >= 2
                                    if (multi || scale > 1f) {
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        val c = event.calculateCentroid(useCurrent = false)
                                        if (c.isSpecified) {
                                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                                            val ratio = newScale / scale
                                            val nx = c.x - (c.x - offX) * ratio + pan.x
                                            val ny = c.y - (c.y - offY) * ratio + pan.y
                                            val minX = -(newScale - 1f) * wPx
                                            val minY = -(newScale - 1f) * hPx
                                            val cx = nx.coerceIn(minX, 0f)
                                            val cy = ny.coerceIn(minY, 0f)
                                            listState.dispatchRawDelta(-(ny - cy) / newScale)
                                            scale = newScale
                                            offX = cx
                                            offY = cy
                                        }
                                        event.changes.forEach {
                                            if (it.positionChanged()) it.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offX
                            translationY = offY
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(slides.size) { i ->
                            SlideView(deck, slides[i], activeQuery)
                        }
                    }
                }

                FastScroller(listState, Modifier.align(Alignment.CenterEnd))

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${listState.firstVisibleItemIndex + 1} / ${slides.size}",
                        color = Color.White
                    )
                }
            }
        }
    }
}
