package com.belge.goruntuleyici

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiffViewerScreen(file: DocFile, onBack: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var menuOpen by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    var offY by remember { mutableFloatStateOf(0f) }

    val count by produceState(0, file.path) {
        value = withContext(Dispatchers.IO) { TiffLoader.pageCount(file.path) }
    }

    fun share() {
        try {
            val uri = FileProvider.getUriForFile(
                context, "com.belge.goruntuleyici.fileprovider", File(file.path)
            )
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "image/tiff"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(i, "Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(context, "Paylaşılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        file.name.substringBeforeLast('.'),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menü")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Paylaş") },
                            onClick = {
                                menuOpen = false
                                share()
                            }
                        )
                    }
                }
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

            if (count == 0) {
                Text(
                    "Yükleniyor...",
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(count) { i ->
                            val st by produceState<Pair<Boolean, Bitmap?>>(Pair(false, null), i) {
                                val bmp = withContext(Dispatchers.IO) {
                                    TiffLoader.page(file.path, i)
                                }
                                value = Pair(true, bmp)
                            }
                            val b = st.second
                            if (b != null) {
                                Image(
                                    bitmap = b.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    filterQuality = FilterQuality.High,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (st.first) "Sayfa açılamadı" else "Yükleniyor...",
                                        color = Color.Black
                                    )
                                }
                            }
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
                        text = "${listState.firstVisibleItemIndex + 1} / $count",
                        color = Color.White
                    )
                }
            }
        }
    }
}
