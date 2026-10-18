package com.belge.goruntuleyici

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File

private class Collector : PDFTextStripper() {

    val chars = ArrayList<PdfChar>()
    var pageW = 1f
    var pageH = 1f

    override fun writeString(text: String, textPositions: List<TextPosition>) {
        for (tp in textPositions) {
            val w = tp.getPageWidth()
            val h = tp.getPageHeight()
            if (w <= 0f || h <= 0f) continue
            pageW = w
            pageH = h
            val u: String = tp.getUnicode() ?: continue
            val x = tp.getXDirAdj()
            val y = tp.getYDirAdj()
            val cw = tp.getWidthDirAdj()
            val ch = tp.getHeightDir()
            chars.add(
                PdfChar(
                    u,
                    x / w,
                    (y - ch) / h,
                    (x + cw) / w,
                    (y + ch * 0.2f) / h,
                    0
                )
            )
        }
    }

    override fun writeWordSeparator() {
        mark(1)
    }

    override fun writeLineSeparator() {
        mark(2)
    }

    private fun mark(v: Int) {
        val last = chars.lastOrNull() ?: return
        if (v > last.sep) last.sep = v
    }
}

class PdfTextIndex(private val context: Context, private val path: String) {

    private var doc: PDDocument? = null
    private val cache = HashMap<Int, PdfPageText>()

    @Synchronized
    fun page(index: Int): PdfPageText {
        val cached = cache[index]
        if (cached != null) return cached
        val result = try {
            PDFBoxResourceLoader.init(context.applicationContext)
            var d = doc
            if (d == null) {
                d = PDDocument.load(File(path))
                doc = d
            }
            val s = Collector()
            s.setSortByPosition(true)
            s.setStartPage(index + 1)
            s.setEndPage(index + 1)
            s.getText(d)
            PdfPageText(s.chars, s.pageW, s.pageH)
        } catch (t: Throwable) {
            PdfPageText(emptyList(), 1f, 1f)
        }
        cache[index] = result
        return result
    }

    @Synchronized
    fun close() {
        try {
            doc?.close()
        } catch (t: Throwable) {
            // yoksay
        }
        doc = null
    }
}
