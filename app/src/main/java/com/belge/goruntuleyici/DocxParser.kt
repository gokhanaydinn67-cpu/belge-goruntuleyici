package com.belge.goruntuleyici

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipFile

object DocxParser {

    private class ParaB {
        val runs = ArrayList<DocRun>()
        var align = 0
        var heading = 0
        var indent = 0f
        var bullet = false
        val images = ArrayList<String>()
    }

    fun parse(path: String): List<DocBlock> {
        val zip = ZipFile(path)
        try {
            val rels = readRels(zip)
            val entry = zip.getEntry("word/document.xml") ?: return emptyList()
            val p = Xml.newPullParser()
            p.setInput(zip.getInputStream(entry), null)
            return readBody(p, path, rels)
        } finally {
            zip.close()
        }
    }

    private fun readRels(zip: ZipFile): Map<String, String> {
        val map = HashMap<String, String>()
        val entry = zip.getEntry("word/_rels/document.xml.rels") ?: return map
        val p = Xml.newPullParser()
        p.setInput(zip.getInputStream(entry), null)
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && p.name == "Relationship") {
                val id = p.getAttributeValue(null, "Id")
                val target = p.getAttributeValue(null, "Target")
                if (id != null && target != null) map[id] = target
            }
            ev = p.next()
        }
        return map
    }

    private fun entryName(target: String): String {
        return if (target.startsWith("/")) target.substring(1) else "word/$target"
    }

    private fun flagOn(p: XmlPullParser): Boolean {
        val v = p.getAttributeValue(null, "w:val")
        return !(v == "0" || v == "false" || v == "off")
    }

    private fun headingLevel(style: String): Int {
        val s = style.lowercase()
        if (s == "title") return 1
        val isHeading = s.startsWith("heading") || s.startsWith("balk") ||
            s.startsWith("başlık") || s.startsWith("baslik") || s.startsWith("titre")
        if (!isHeading) return 0
        val d = s.lastOrNull { it.isDigit() }?.digitToInt() ?: 1
        return d.coerceIn(1, 6)
    }

    private fun readBody(
        p: XmlPullParser,
        path: String,
        rels: Map<String, String>
    ): List<DocBlock> {
        val blocks = ArrayList<DocBlock>()
        val stack = ArrayList<ParaB>()
        var tblDepth = 0
        var rows = ArrayList<List<List<ParaBlock>>>()
        var row = ArrayList<List<ParaBlock>>()
        var cell = ArrayList<ParaBlock>()

        var inPPr = false
        var inRun = false
        var inRPr = false
        var bold = false
        var italic = false
        var underline = false
        var size = 0f
        var color = 0

        fun addRun(text: String) {
            val cur = stack.lastOrNull() ?: return
            cur.runs.add(DocRun(text, bold, italic, underline, size, color))
        }

        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                when (p.name) {
                    "w:tbl" -> {
                        tblDepth++
                        if (tblDepth == 1) rows = ArrayList()
                    }
                    "w:tr" -> {
                        if (tblDepth == 1) row = ArrayList()
                    }
                    "w:tc" -> {
                        if (tblDepth == 1) cell = ArrayList()
                    }
                    "w:p" -> stack.add(ParaB())
                    "w:pPr" -> inPPr = true
                    "w:pStyle" -> {
                        if (inPPr) {
                            val s = p.getAttributeValue(null, "w:val") ?: ""
                            stack.lastOrNull()?.heading = headingLevel(s)
                        }
                    }
                    "w:jc" -> {
                        if (inPPr && !inRun) {
                            val v = p.getAttributeValue(null, "w:val")
                            stack.lastOrNull()?.align = when (v) {
                                "center" -> 1
                                "right", "end" -> 2
                                "both", "distribute" -> 3
                                else -> 0
                            }
                        }
                    }
                    "w:ind" -> {
                        if (inPPr) {
                            val left = p.getAttributeValue(null, "w:left")
                                ?: p.getAttributeValue(null, "w:start")
                            stack.lastOrNull()?.indent = (left?.toFloatOrNull() ?: 0f) / 20f
                        }
                    }
                    "w:numPr" -> {
                        if (inPPr) stack.lastOrNull()?.bullet = true
                    }
                    "w:r" -> {
                        inRun = true
                        bold = false
                        italic = false
                        underline = false
                        size = 0f
                        color = 0
                    }
                    "w:rPr" -> inRPr = inRun
                    "w:b" -> {
                        if (inRPr) bold = flagOn(p)
                    }
                    "w:i" -> {
                        if (inRPr) italic = flagOn(p)
                    }
                    "w:u" -> {
                        if (inRPr) underline = p.getAttributeValue(null, "w:val") != "none"
                    }
                    "w:sz" -> {
                        if (inRPr) {
                            val v = p.getAttributeValue(null, "w:val")?.toFloatOrNull()
                            if (v != null) size = v / 2f * (15f / 11f)
                        }
                    }
                    "w:color" -> {
                        if (inRPr) {
                            val v = p.getAttributeValue(null, "w:val")
                            val c = if (v != null && v.length == 6) v.toIntOrNull(16) else null
                            if (c != null) color = c or 0xFF000000.toInt()
                        }
                    }
                    "w:t" -> addRun(p.nextText())
                    "w:tab" -> addRun("    ")
                    "w:br", "w:cr" -> addRun("\n")
                    "a:blip" -> {
                        val embed = p.getAttributeValue(null, "r:embed")
                        val target = if (embed != null) rels[embed] else null
                        if (target != null) {
                            stack.lastOrNull()?.images?.add(entryName(target))
                        }
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                when (p.name) {
                    "w:pPr" -> inPPr = false
                    "w:rPr" -> inRPr = false
                    "w:r" -> inRun = false
                    "w:p" -> {
                        if (stack.isNotEmpty()) {
                            val b = stack.removeAt(stack.size - 1)
                            val para = ParaBlock(b.runs, b.align, b.heading, b.indent, b.bullet)
                            if (tblDepth > 0) cell.add(para) else blocks.add(para)
                            if (tblDepth == 0) {
                                for (img in b.images) blocks.add(ImageBlock(path, img))
                            }
                        }
                    }
                    "w:tc" -> {
                        if (tblDepth == 1) row.add(cell)
                    }
                    "w:tr" -> {
                        if (tblDepth == 1) rows.add(row)
                    }
                    "w:tbl" -> {
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
