package com.belge.goruntuleyici

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BlockView(b: DocBlock, zoom: Float, query: String, strong: Boolean) {
    when (b) {
        is ParaBlock -> ParaView(b, zoom, query, strong)
        is TableBlock -> TableView(b, zoom, query)
        is ImageBlock -> ImageView(b)
    }
}

private fun headingSize(level: Int): Float {
    return when (level) {
        0 -> 15f
        1 -> 24f
        2 -> 20f
        3 -> 17f
        else -> 15f
    }
}

private fun buildPara(
    p: ParaBlock,
    zoom: Float,
    query: String,
    strong: Boolean
): AnnotatedString {
    val hl = if (strong) Color(0xFFFF9800) else Color(0xFFFFF176)
    return buildAnnotatedString {
        if (p.bullet) append("•  ")
        if (p.runs.isEmpty()) {
            withStyle(SpanStyle(fontSize = (15f * zoom).sp)) { append(" ") }
        }
        for (r in p.runs) {
            val size = if (r.size > 0f) r.size else headingSize(p.heading)
            val style = SpanStyle(
                fontSize = (size * zoom).sp,
                fontWeight = if (r.bold || p.heading > 0) FontWeight.Bold else null,
                fontStyle = if (r.italic) FontStyle.Italic else null,
                textDecoration = if (r.underline) TextDecoration.Underline else null,
                color = if (r.color != 0) Color(r.color) else Color.Black
            )
            withStyle(style) { append(r.text) }
        }
        if (query.isNotBlank()) {
            val off = if (p.bullet) 3 else 0
            var start = 0
            while (true) {
                val i = p.plain.indexOf(query, start, ignoreCase = true)
                if (i < 0) break
                addStyle(SpanStyle(background = hl), off + i, off + i + query.length)
                start = i + query.length
            }
        }
    }
}

@Composable
fun ParaView(p: ParaBlock, zoom: Float, query: String, strong: Boolean) {
    val text = remember(p, zoom, query, strong) { buildPara(p, zoom, query, strong) }
    val align = when (p.align) {
        1 -> TextAlign.Center
        2 -> TextAlign.End
        3 -> TextAlign.Justify
        else -> TextAlign.Start
    }
    Text(
        text = text,
        color = Color.Black,
        textAlign = align,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = p.indent.coerceIn(0f, 200f).dp, top = 3.dp, bottom = 3.dp)
    )
}

@Composable
fun TableView(t: TableBlock, zoom: Float, query: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        for (row in t.rows) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                for (cell in row) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, Color.Gray)
                            .padding(4.dp)
                    ) {
                        for (p in cell) {
                            ParaView(p, zoom, query, false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageView(b: ImageBlock) {
    val bmp by produceState<Bitmap?>(null, b) {
        value = withContext(Dispatchers.IO) {
            DocImage.load(b.zipPath, b.entry, 1080)
        }
    }
    val bm = bmp
    if (bm != null) {
        Image(
            bitmap = bm.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Inside,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
    }
}
