package com.belge.goruntuleyici

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipFile

object OdpParser {

    private fun newParser(input: InputStream): XmlPullParser {
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        p.setInput(input, null)
        return p
    }

    private fun emu(v: String?): Long {
        if (v == null) return 0L
        val s = v.trim()
        val numText = s.takeWhile { it.isDigit() || it == '.' || it == '-' }
        val num = numText.toDoubleOrNull() ?: return 0L
        val unit = s.substring(numText.length)
        val f = when (unit) {
            "cm" -> 360000.0
            "mm" -> 36000.0
            "in" -> 914400.0
            "pt" -> 12700.0
            "px" -> 9525.0
            else -> 360000.0
        }
        return (num * f).toLong()
    }

    private fun pageSize(zip: ZipFile): Pair<Long, Long> {
        val e = zip.getEntry("styles.xml") ?: return Pair(10080000L, 5670000L)
        val p = newParser(zip.getInputStream(e))
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && p.name == "page-layout-properties") {
                val pw = emu(p.getAttributeValue(null, "page-width"))
                val ph = emu(p.getAttributeValue(null, "page-height"))
                if (pw > 0L && ph > 0L) return Pair(pw, ph)
            }
            ev = p.next()
        }
        return Pair(10080000L, 5670000L)
    }

    fun parse(path: String): Deck {
        val zip = ZipFile(path)
        try {
            val size = pageSize(zip)
            val e = zip.getEntry("content.xml")
                ?: return Deck(path, size.first, size.second, emptyList())
            val p = newParser(zip.getInputStream(e))
            return Deck(path, size.first, size.second, readSlides(p))
        } finally {
            zip.close()
        }
    }

    private fun readSlides(p: XmlPullParser): List<SlideData> {
        val slides = ArrayList<SlideData>()
        var shapes = ArrayList<SlideShape>()
        var inPage = false
        var notes = 0
        var frameDepth = 0
        var fx = 0L
        var fy = 0L
        var fw = 0L
        var fh = 0L
        var cls = ""
        var image: String? = null
        val paras = ArrayList<SlidePara>()
        var sb = StringBuilder()
        var inPara = false
        var isHead = false
        var bullet = false

        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                when (p.name) {
                    "page" -> {
                        inPage = true
                        shapes = ArrayList()
                    }
                    "notes" -> notes++
                    "frame", "custom-shape" -> {
                        if (inPage && notes == 0) {
                            frameDepth++
                            if (frameDepth == 1) {
                                fx = emu(p.getAttributeValue(null, "x"))
                                fy = emu(p.getAttributeValue(null, "y"))
                                fw = emu(p.getAttributeValue(null, "width"))
                                fh = emu(p.getAttributeValue(null, "height"))
                                cls = p.getAttributeValue(null, "class") ?: ""
                                image = null
                                paras.clear()
                            }
                        }
                    }
                    "image" -> {
                        if (frameDepth > 0 && image == null) {
                            val href = p.getAttributeValue(null, "href")
                            if (href != null && !href.startsWith("http")) image = href
                        }
                    }
                    "h", "p" -> {
                        if (frameDepth > 0) {
                            inPara = true
                            sb = StringBuilder()
                            isHead = p.name == "h"
                        }
                    }
                    "list-item" -> {
                        if (frameDepth > 0) bullet = true
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
                }
            } else if (ev == XmlPullParser.TEXT) {
                if (inPara) sb.append(p.text)
            } else if (ev == XmlPullParser.END_TAG) {
                when (p.name) {
                    "h", "p" -> {
                        if (inPara) {
                            inPara = false
                            val run = SlideRun(sb.toString(), bold = isHead)
                            paras.add(SlidePara(listOf(run), 0, bullet))
                        }
                    }
                    "list-item" -> bullet = false
                    "frame", "custom-shape" -> {
                        if (frameDepth > 0) {
                            frameDepth--
                            if (frameDepth == 0 && (paras.isNotEmpty() || image != null)) {
                                shapes.add(
                                    SlideShape(
                                        fx, fy, fw, fh, 0,
                                        ArrayList(paras), image, null, cls == "title"
                                    )
                                )
                            }
                        }
                    }
                    "notes" -> {
                        if (notes > 0) notes--
                    }
                    "page" -> {
                        if (inPage) slides.add(SlideData(shapes, 0))
                        inPage = false
                    }
                }
            }
            ev = p.next()
        }
        return slides
    }
}
