package com.belge.goruntuleyici

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
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
fun SlideView(deck: Deck, slide: SlideData, query: String, modifier: Modifier = Modifier) {
    val bgColor = if (slide.bg != 0) Color(slide.bg) else Color.White
    val textColor = if (bgColor.luminance() < 0.4f) Color.White else Color.Black
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(deck.widthEmu.toFloat() / deck.heightEmu.toFloat())
            .background(bgColor)
            .border(0.5.dp, Color.Gray)
            .clipToBounds()
    ) {
        val dpPerEmu = maxWidth.value / deck.widthEmu.toFloat()
        val dpPerPt = maxWidth.value / (deck.widthEmu / 12700f)
        for (s in slide.shapes) {
            ShapeView(deck, s, dpPerEmu, dpPerPt, query, textColor)
        }
    }
}

@Composable
private fun ShapeView(
    deck: Deck,
    s: SlideShape,
    dpPerEmu: Float,
    dpPerPt: Float,
    query: String,
    textColor: Color
) {
    var m = Modifier
        .offset((s.x * dpPerEmu).dp, (s.y * dpPerEmu).dp)
        .width((s.w * dpPerEmu).dp)
    if (s.fill != 0) m = m.background(Color(s.fill))
    val img = s.image
    val tbl = s.table

    if (img != null) {
        val bmp by produceState<Bitmap?>(null, img) {
            value = withContext(Dispatchers.IO) {
                DocImage.load(deck.path, img, 1080)
            }
        }
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = m.height((s.h * dpPerEmu).dp)
            )
        }
    } else if (tbl != null) {
        Column(modifier = m) {
            for (row in tbl) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (cell in row) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(0.5.dp, Color.Gray)
                                .padding(2.dp)
                        ) {
                            for (p in cell) {
                                ParaText(p, false, dpPerPt, query, textColor, 12f)
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier = m.heightIn(min = (s.h * dpPerEmu).dp)) {
            for (p in s.paras) {
                ParaText(p, s.title, dpPerPt, query, textColor, if (s.title) 32f else 18f)
            }
        }
    }
}

@Composable
private fun ParaText(
    p: SlidePara,
    title: Boolean,
    dpPerPt: Float,
    query: String,
    color: Color,
    defSize: Float
) {
    val text = remember(p, title, dpPerPt, query, color, defSize) {
        buildSlidePara(p, title, dpPerPt, query, color, defSize)
    }
    val align = when (p.align) {
        1 -> TextAlign.Center
        2 -> TextAlign.End
        3 -> TextAlign.Justify
        else -> TextAlign.Start
    }
    Text(text = text, textAlign = align, modifier = Modifier.fillMaxWidth())
}

private fun buildSlidePara(
    p: SlidePara,
    title: Boolean,
    dpPerPt: Float,
    query: String,
    color: Color,
    defSize: Float
): AnnotatedString {
    val hl = Color(0xFFFFF176)
    return buildAnnotatedString {
        if (p.bullet) append("•  ")
        if (p.runs.isEmpty()) {
            withStyle(SpanStyle(fontSize = (defSize * dpPerPt).coerceAtLeast(5f).sp)) {
                append(" ")
            }
        }
        for (r in p.runs) {
            val pt = if (r.size > 0f) r.size else defSize
            val style = SpanStyle(
                fontSize = (pt * dpPerPt).coerceAtLeast(5f).sp,
                fontWeight = if (r.bold || title) FontWeight.Bold else null,
                fontStyle = if (r.italic) FontStyle.Italic else null,
                textDecoration = if (r.underline) TextDecoration.Underline else null,
                color = if (r.color != 0) Color(r.color) else color
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
