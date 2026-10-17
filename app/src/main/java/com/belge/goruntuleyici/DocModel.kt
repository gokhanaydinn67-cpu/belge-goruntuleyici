package com.belge.goruntuleyici

import java.util.zip.ZipFile

data class DocRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val size: Float = 0f,
    val color: Int = 0
)

sealed class DocBlock

class ParaBlock(
    val runs: List<DocRun>,
    val align: Int = 0,
    val heading: Int = 0,
    val indent: Float = 0f,
    val bullet: Boolean = false
) : DocBlock() {
    val plain: String = runs.joinToString("") { it.text }
}

class TableBlock(val rows: List<List<List<ParaBlock>>>) : DocBlock()

class ImageBlock(val zipPath: String, val entry: String) : DocBlock()

fun DocBlock.text(): String = when (this) {
    is ParaBlock -> plain
    is TableBlock -> rows.joinToString(" ") { r ->
        r.joinToString(" ") { c -> c.joinToString(" ") { it.plain } }
    }
    is ImageBlock -> ""
}

object DocLoader {

    fun load(path: String, ext: String): List<DocBlock> {
        return when (ext) {
            "docx", "docm", "dotx" -> loadDocx(path)
            else -> emptyList()
        }
    }

    private fun msg(text: String): List<DocBlock> {
        return listOf(ParaBlock(listOf(DocRun(text))))
    }

    private fun loadDocx(path: String): List<DocBlock> {
        return try {
            val r = DocxParser.parse(path)
            if (r.isNotEmpty()) {
                r
            } else {
                msg("Okuyucu boş sonuç verdi. Belge başlangıcı: " + snippet(path))
            }
        } catch (t: Throwable) {
            msg("Hata: " + t.javaClass.simpleName + " - " + t.message)
        }
    }

    private fun snippet(path: String): String {
        return try {
            val z = ZipFile(path)
            try {
                val e = z.getEntry("word/document.xml")
                if (e == null) {
                    "document.xml yok"
                } else {
                    val bytes = z.getInputStream(e).use { it.readBytes() }
                    val s = String(bytes, Charsets.UTF_8)
                    val i = s.indexOf("<w:body")
                    val from = if (i >= 0) i else 0
                    s.substring(from, minOf(s.length, from + 600))
                }
            } finally {
                z.close()
            }
        } catch (t: Throwable) {
            "okunamadı: " + t.message
        }
    }
}
