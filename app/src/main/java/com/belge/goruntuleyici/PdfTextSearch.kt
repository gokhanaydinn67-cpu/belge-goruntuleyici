package com.belge.goruntuleyici

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

object PdfTextSearch {

    private var started = false

    fun search(context: Context, path: String, query: String): List<Int> {
        val result = ArrayList<Int>()
        if (query.isBlank()) return result
        if (!started) {
            PDFBoxResourceLoader.init(context.applicationContext)
            started = true
        }
        try {
            val doc = PDDocument.load(File(path))
            try {
                val stripper = PDFTextStripper()
                val total = doc.numberOfPages
                for (p in 1..total) {
                    stripper.startPage = p
                    stripper.endPage = p
                    val text = stripper.getText(doc)
                    if (text.contains(query, ignoreCase = true)) {
                        result.add(p - 1)
                    }
                }
            } finally {
                doc.close()
            }
        } catch (e: Exception) {
            // okunamayan PDF: boş sonuç döner
        }
        return result
    }
}
