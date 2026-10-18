package com.belge.goruntuleyici

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

class ZoomState {
    var scale by mutableFloatStateOf(1f)
    var offX by mutableFloatStateOf(0f)
    var offY by mutableFloatStateOf(0f)
}

class SelectionActions(
    val onLongPress: (Int, Float, Float) -> Unit,
    val onTapOutside: () -> Unit,
    val onChange: (PdfSel) -> Unit
)

private fun itemFor(listState: LazyListState, page: Int) =
    listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == page }

private fun locate(
    listState: LazyListState,
    zoom: ZoomState,
    x: Float,
    y: Float,
    wPx: Float
): Triple<Int, Float, Float>? {
    val cx = (x - zoom.offX) / zoom.scale
    val cy = (y - zoom.offY) / zoom.scale
    val info = listState.layoutInfo.visibleItemsInfo
        .firstOrNull { cy >= it.offset && cy < it.offset + it.size } ?: return null
    return Triple(info.index, cx / wPx, (cy - info.offset) / info.size)
}

private fun handleCenter(
    listState: LazyListState,
    zoom: ZoomState,
    page: Int,
    fx: Float,
    fy: Float,
    wPx: Float,
    dropPx: Float
): Offset? {
    val info = itemFor(listState, page) ?: return null
    val cx = fx * wPx
    val cy = info.offset + fy * info.size + dropPx
    return Offset(cx * zoom.scale + zoom.offX, cy * zoom.scale + zoom.offY)
}

fun Modifier.pdfGestures(
    wPx: Float,
    hPx: Float,
    density: Float,
    listState: LazyListState,
    zoom: ZoomState,
    selection: State<PdfSel?>,
    pageText: State<PdfPageText?>,
    actions: SelectionActions
): Modifier = this.pointerInput(wPx, hPx) {
    val slop = viewConfiguration.touchSlop
    val longPressMs = viewConfiguration.longPressTimeoutMillis
    val dropPx = HANDLE_DROP_DP * density

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

        // 1) Seçim tutamaçları
        var grabbed = 0
        val sel0 = selection.value
        val text0 = pageText.value
        if (sel0 != null && text0 != null && text0.chars.isNotEmpty()) {
            val last = text0.chars.size - 1
            val a = text0.chars[sel0.start.coerceIn(0, last)]
            val b = text0.chars[sel0.end.coerceIn(0, last)]
            val pa = handleCenter(listState, zoom, sel0.page, a.l, a.b, wPx, dropPx)
            val pb = handleCenter(listState, zoom, sel0.page, b.r, b.b, wPx, dropPx)
            val hit = 28f * density * zoom.scale.coerceAtLeast(1f)
            val da = if (pa != null) (pa - down.position).getDistance() else Float.MAX_VALUE
            val db = if (pb != null) (pb - down.position).getDistance() else Float.MAX_VALUE
            if (da <= hit || db <= hit) grabbed = if (da <= db) 1 else 2
        }
        if (grabbed != 0) {
            down.consume()
            do {
                val e = awaitPointerEvent(PointerEventPass.Initial)
                val c = e.changes[0]
                val s = selection.value
                val t = pageText.value
                if (s != null && t != null && t.chars.isNotEmpty()) {
                    val info = itemFor(listState, s.page)
                    if (info != null) {
                        val fx = ((c.position.x - zoom.offX) / zoom.scale) / wPx
                        val fy = (((c.position.y - zoom.offY) / zoom.scale) - dropPx - info.offset) / info.size
                        val idx = PdfSelect.nearest(t, fx, fy).first
                        if (idx >= 0) {
                            val ns = if (grabbed == 1) {
                                PdfSel(s.page, minOf(idx, s.end), s.end)
                            } else {
                                PdfSel(s.page, s.start, maxOf(idx, s.start))
                            }
                            actions.onChange(ns)
                        }
                    }
                }
                e.changes.forEach { it.consume() }
            } while (e.changes.any { it.pressed })
            return@awaitEachGesture
        }

        // 2) Uzun basma / dokunma tespiti
        var outcome = -1
        withTimeoutOrNull(longPressMs) {
            var running = true
            while (running) {
                val e = awaitPointerEvent(PointerEventPass.Initial)
                if (e.changes.size >= 2) {
                    outcome = 1
                    running = false
                } else {
                    val c = e.changes[0]
                    if (!c.pressed) {
                        outcome = 2
                        running = false
                    } else if ((c.position - down.position).getDistance() > slop) {
                        outcome = 3
                        running = false
                    }
                }
            }
        }
        if (outcome == -1) {
            val loc = locate(listState, zoom, down.position.x, down.position.y, wPx)
            if (loc != null) actions.onLongPress(loc.first, loc.second, loc.third)
            do {
                val e = awaitPointerEvent(PointerEventPass.Initial)
                e.changes.forEach { it.consume() }
            } while (e.changes.any { it.pressed })
            return@awaitEachGesture
        }
        if (outcome == 2) {
            if (selection.value != null) actions.onTapOutside()
            return@awaitEachGesture
        }

        // 3) Yakınlaştırma ve kaydırma
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val multi = event.changes.size >= 2
            if (multi || zoom.scale > 1f) {
                val z = event.calculateZoom()
                val pan = event.calculatePan()
                val c = event.calculateCentroid(useCurrent = false)
                if (c.isSpecified) {
                    val newScale = (zoom.scale * z).coerceIn(1f, 5f)
                    val ratio = newScale / zoom.scale
                    val nx = c.x - (c.x - zoom.offX) * ratio + pan.x
                    val ny = c.y - (c.y - zoom.offY) * ratio + pan.y
                    val minX = -(newScale - 1f) * wPx
                    val minY = -(newScale - 1f) * hPx
                    val cx = nx.coerceIn(minX, 0f)
                    val cy = ny.coerceIn(minY, 0f)
                    listState.dispatchRawDelta(-(ny - cy) / newScale)
                    zoom.scale = newScale
                    zoom.offX = cx
                    zoom.offY = cy
                }
                event.changes.forEach {
                    if (it.positionChanged()) it.consume()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}
