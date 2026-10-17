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
                msg("Okuyucu boş sonuç verdi. Dosya içeriği: " + entries(path))
            }
        } catch (t: Throwable) {
            msg("Hata: " + t.javaClass.simpleName + " - " + t.message)
        }
    }

    private fun entries(path: String): String {
        return try {
            val z = ZipFile(path)
            try {
                z.entries().toList().take(12).joinToString(", ") { it.name }
            } finally {
                z.close()
            }
        } catch (t: Throwable) {
            "zip açılamadı: " + t.message
        }
    }
}
