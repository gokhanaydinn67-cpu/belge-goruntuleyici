package com.belge.goruntuleyici

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PdfPageItem(
    doc: PdfDoc,
    index: Int,
    widthPx: Int,
    sel: PdfSel?,
    text: PdfPageText?,
    matches: List<PdfMatch>,
    matchText: PdfPageText?,
    current: PdfMatch?
) {
    val bmp by produceState<Bitmap?>(null, index) {
        value = withContext(Dispatchers.Default) {
            doc.render(index, (widthPx * 1.5f).toInt())
        }
    }
    val b = bmp
    if (b != null) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            if (matchText != null && matches.isNotEmpty()) {
                MatchOverlay(matchText, matches, current, Modifier.matchParentSize())
            }
            if (sel != null && sel.page == index && text != null && text.chars.isNotEmpty()) {
                SelectionOverlay(text, sel, Modifier.matchParentSize())
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(Color.White)
        )
    }
}

@Composable
private fun MatchOverlay(
    text: PdfPageText,
    matches: List<PdfMatch>,
    current: PdfMatch?,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        for (m in matches) {
            val color = if (m === current) Color(0x99FF9800) else Color(0x88FFEB3B)
            val rects = PdfSelect.lineRects(text, m.start, m.end)
            for (r in rects) {
                drawRect(
                    color = color,
                    topLeft = Offset(r[0] * w, r[1] * h),
                    size = Size((r[2] - r[0]) * w, (r[3] - r[1]) * h)
                )
            }
        }
    }
}

@Composable
private fun SelectionOverlay(text: PdfPageText, sel: PdfSel, modifier: Modifier) {
    val density = LocalDensity.current.density
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val rects = PdfSelect.lineRects(text, sel.start, sel.end)
        for (r in rects) {
            drawRect(
                color = Color(0x663B82F6),
                topLeft = Offset(r[0] * w, r[1] * h),
                size = Size((r[2] - r[0]) * w, (r[3] - r[1]) * h)
            )
        }
        val last = text.chars.size - 1
        val first = text.chars[sel.start.coerceIn(0, last)]
        val end = text.chars[sel.end.coerceIn(0, last)]
        val drop = HANDLE_DROP_DP * density
        val rad = HANDLE_RADIUS_DP * density
        drawHandle(Offset(first.l * w, first.b * h + drop), rad)
        drawHandle(Offset(end.r * w, end.b * h + drop), rad)
    }
}

private fun DrawScope.drawHandle(center: Offset, rad: Float) {
    drawCircle(Color.White, radius = rad + 2f, center = center)
    drawCircle(Color(0xFF2563EB), radius = rad, center = center)
}
