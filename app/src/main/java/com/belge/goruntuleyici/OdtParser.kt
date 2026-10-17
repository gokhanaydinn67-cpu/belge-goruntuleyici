package com.belge.goruntuleyici

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipFile

object OdtParser {

    fun parse(path: String): List<DocBlock> {
        val zip = ZipFile(path)
        try {
            val entry = zip.getEntry("content.xml") ?: return emptyList()
            val p = Xml.newPullParser()
            p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            p.setInput(zip.getInputStream(entry), null)
            return read(p, path)
        } finally {
            zip.close()
        }
    }

    private fun read(p: XmlPullParser, path: String): List<DocBlock> {
        val blocks = ArrayList<DocBlock>()
        var tblDepth = 0
        var rows = ArrayList<List<List<ParaBlock>>>()
        var row = ArrayList<List<ParaBlock>>()
        var cell = ArrayList<ParaBlock>()

        var sb = StringBuilder()
        var inPara = false
        var heading = 0
        var bullet = false
        var listDepth = 0
        val images = ArrayList<String>()

        fun finishPara() {
            if (!inPara) return
            val text = sb.toString()
            val runs = if (text.isEmpty()) emptyList() else listOf(DocRun(text))
            val para = ParaBlock(runs, 0, heading, 0f, bullet)
            if (tblDepth > 0) cell.add(para) else blocks.add(para)
            if (tblDepth == 0) {
                for (img in images) blocks.add(ImageBlock(path, img))
            }
            images.clear()
            sb = StringBuilder()
            inPara = false
            heading = 0
        }

        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                when (p.name) {
                    "table" -> {
                        tblDepth++
                        if (tblDepth == 1) rows = ArrayList()
                    }
                    "table-row" -> {
                        if (tblDepth == 1) row = ArrayList()
                    }
                    "table-cell" -> {
                        if (tblDepth == 1) cell = ArrayList()
                    }
                    "list" -> listDepth++
                    "list-item" -> bullet = true
                    "h" -> {
                        inPara = true
                        sb = StringBuilder()
                        val lv = p.getAttributeValue(null, "outline-level")?.toIntOrNull() ?: 1
                        heading = lv.coerceIn(1, 6)
                    }
                    "p" -> {
                        inPara = true
                        sb = StringBuilder()
                        heading = 0
                    }
                    "tab" -> {
                        if (inPara) sb.append("    ")
                    }
                    "line-break" -> {
                        if (inPara) sb.append("\n")
                    }
                    "s" -> {
                        if (inPara) {
                            val c = p.getAttributeValue(null, "c")?.toIntOrNull() ?: 1
                            sb.append(" ".repeat(c.coerceIn(1, 50)))
                        }
                    }
                    "image" -> {
                        val href = p.getAttributeValue(null, "href")
                        if (href != null && !href.startsWith("http")) images.add(href)
                    }
                }
            } else if (ev == XmlPullParser.TEXT) {
                if (inPara) sb.append(p.text)
            } else if (ev == XmlPullParser.END_TAG) {
                when (p.name) {
                    "h", "p" -> finishPara()
                    "list-item" -> bullet = false
                    "list" -> {
                        if (listDepth > 0) listDepth--
                    }
                    "table-cell" -> {
                        if (tblDepth == 1) row.add(cell)
                    }
                    "table-row" -> {
                        if (tblDepth == 1) rows.add(row)
                    }
                    "table" -> {
                        if (tblDepth == 1) blocks.add(TableBlock(rows))
                        if (tblDepth > 0) tblDepth--
                    }
                }
            }
            ev = p.next()
        }
        return blocks
    }
}
