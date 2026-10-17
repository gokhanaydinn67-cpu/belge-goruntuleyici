package com.belge.goruntuleyici

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class RawShape {
    var x = 0L
    var y = 0L
    var w = 0L
    var h = 0L
    var hasXfrm = false
    var fill = 0
    val paras = ArrayList<SlidePara>()
    var image: String? = null
    var table: List<List<List<SlidePara>>>? = null
    var phType: String? = null
    var phIdx: String? = null
}

class RawSlide(val shapes: List<RawShape>, val bg: Int)

object PptxSlideReader {

    private fun truthy(v: String?): Boolean {
        return v == "1" || v == "true"
    }

    fun read(input: InputStream, rels: Map<String, String>): RawSlide {
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        p.setInput(input, null)

        val shapes = ArrayList<RawShape>()
        val stack = ArrayList<String>()
        var cur: RawShape? = null
        var bg = 0

        var tbl = ArrayList<List<List<SlidePara>>>()
        var tblRow = ArrayList<List<SlidePara>>()
        var tblCell: ArrayList<SlidePara>? = null
        var inTbl = false

        var runs = ArrayList<SlideRun>()
        var align = 0
        var bullet = false
        var inPara = false

        var bold = false
        var italic = false
        var underline = false
        var size = 0f
        var color = 0

        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                val name = p.name
                stack.add(name)
                val parent = if (stack.size >= 2) stack[stack.size - 2] else ""
                when (name) {
                    "sp", "pic", "graphicFrame" -> {
                        cur = RawShape()
                    }
                    "ph" -> {
                        cur?.phType = p.getAttributeValue(null, "type") ?: "body"
                        cur?.phIdx = p.getAttributeValue(null, "idx")
                    }
                    "off" -> {
                        val c = cur
                        if (c != null && parent == "xfrm") {
                            c.x = p.getAttributeValue(null, "x")?.toLongOrNull() ?: 0L
                            c.y = p.getAttributeValue(null, "y")?.toLongOrNull() ?: 0L
                            c.hasXfrm = true
                        }
                    }
                    "ext" -> {
                        val c = cur
                        if (c != null && parent == "xfrm") {
                            c.w = p.getAttributeValue(null, "cx")?.toLongOrNull() ?: 0L
                            c.h = p.getAttributeValue(null, "cy")?.toLongOrNull() ?: 0L
                        }
                    }
                    "blip" -> {
                        val id = p.getAttributeValue(null, "embed")
                        if (id != null) cur?.image = rels[id]
                    }
                    "tbl" -> {
                        inTbl = true
                        tbl = ArrayList()
                    }
                    "tr" -> {
                        if (inTbl) tblRow = ArrayList()
                    }
                    "tc" -> {
                        if (inTbl) tblCell = ArrayList()
                    }
                    "p" -> {
                        if (parent == "txBody") {
                            inPara = true
                            runs = ArrayList()
                            align = 0
                            bullet = false
                        }
                    }
                    "pPr" -> {
                        if (inPara) {
                            align = when (p.getAttributeValue(null, "algn")) {
                                "ctr" -> 1
                                "r" -> 2
                                "just" -> 3
                                else -> 0
                            }
                        }
                    }
                    "buChar", "buAutoNum" -> {
                        if (inPara) bullet = true
                    }
                    "buNone" -> {
                        if (inPara) bullet = false
                    }
                    "r", "fld" -> {
                        bold = false
                        italic = false
                        underline = false
                        size = 0f
                        color = 0
                    }
                    "rPr" -> {
                        if (inPara) {
                            bold = truthy(p.getAttributeValue(null, "b"))
                            italic = truthy(p.getAttributeValue(null, "i"))
                            val u = p.getAttributeValue(null, "u")
                            underline = u != null && u != "none"
                            val sz = p.getAttributeValue(null, "sz")?.toFloatOrNull()
                            size = if (sz != null) sz / 100f else 0f
                        }
                    }
                    "srgbClr" -> {
                        if (stack.size >= 3 && parent == "solidFill") {
                            val ctx = stack[stack.size - 3]
                            val hex = p.getAttributeValue(null, "val")
                            val v = if (hex != null && hex.length == 6) hex.toIntOrNull(16) else null
                            if (v != null) {
                                val c = v or 0xFF000000.toInt()
                                if (ctx == "rPr") {
                                    color = c
                                } else if (ctx == "spPr") {
                                    cur?.fill = c
                                } else if (ctx == "bgPr") {
                                    bg = c
                                }
                            }
                        }
                    }
                    "t" -> {
                        val text = p.nextText()
                        stack.removeAt(stack.size - 1)
                        if (inPara) {
                            runs.add(SlideRun(text, bold, italic, underline, size, color))
                        }
                    }
                    "br" -> {
                        if (inPara) {
                            runs.add(SlideRun("\n", bold, italic, underline, size, color))
                        }
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                when (p.name) {
                    "p" -> {
                        if (inPara) {
                            inPara = false
                            val para = SlidePara(runs, align, bullet)
                            val cell = tblCell
                            if (cell != null) cell.add(para) else cur?.paras?.add(para)
                        }
                    }
                    "tc" -> {
                        if (inTbl) tblRow.add(tblCell ?: ArrayList())
                        tblCell = null
                    }
                    "tr" -> {
                        if (inTbl) tbl.add(tblRow)
                    }
                    "tbl" -> {
                        if (inTbl) cur?.table = tbl
                        inTbl = false
                    }
                    "sp", "pic", "graphicFrame" -> {
                        val c = cur
                        if (c != null) shapes.add(c)
                        cur = null
                    }
                }
                if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            }
            ev = p.next()
        }
        return RawSlide(shapes, bg)
    }
}
