package com.belge.goruntuleyici

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipFile

private class Rel(val id: String, val type: String, val target: String)

object PptxPackage {

    fun load(path: String): Deck {
        val zip = ZipFile(path)
        try {
            val size = readSize(zip)
            val w = size.first
            val h = size.second
            val cache = HashMap<String, Map<String, LongArray>>()
            val slides = ArrayList<SlideData>()
            for (entry in slideOrder(zip)) {
                try {
                    slides.add(readSlide(zip, entry, w, h, cache))
                } catch (t: Throwable) {
                    slides.add(SlideData(emptyList(), 0))
                }
            }
            return Deck(path, w, h, slides)
        } finally {
            zip.close()
        }
    }

    private fun open(zip: ZipFile, name: String): XmlPullParser? {
        val e = zip.getEntry(name) ?: return null
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        p.setInput(zip.getInputStream(e), null)
        return p
    }

    private fun resolve(base: String, target: String): String {
        if (target.startsWith("/")) return target.substring(1)
        val parts = ArrayList<String>()
        for (s in base.substringBeforeLast('/', "").split('/')) {
            if (s.isNotEmpty()) parts.add(s)
        }
        for (seg in target.split('/')) {
            if (seg == "..") {
                if (parts.isNotEmpty()) parts.removeAt(parts.size - 1)
            } else if (seg != "." && seg.isNotEmpty()) {
                parts.add(seg)
            }
        }
        return parts.joinToString("/")
    }

    private fun relsOf(zip: ZipFile, entry: String): List<Rel> {
        val dir = entry.substringBeforeLast('/', "")
        val file = entry.substringAfterLast('/')
        val relPath = if (dir.isEmpty()) "_rels/$file.rels" else "$dir/_rels/$file.rels"
        val out = ArrayList<Rel>()
        val p = open(zip, relPath) ?: return out
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && p.name == "Relationship") {
                val id = p.getAttributeValue(null, "Id")
                val type = p.getAttributeValue(null, "Type")
                val target = p.getAttributeValue(null, "Target")
                if (id != null && type != null && target != null) {
                    out.add(Rel(id, type, resolve(entry, target)))
                }
            }
            ev = p.next()
        }
        return out
    }

    private fun readSize(zip: ZipFile): Pair<Long, Long> {
        val p = open(zip, "ppt/presentation.xml") ?: return Pair(9144000L, 6858000L)
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && p.name == "sldSz") {
                val cx = p.getAttributeValue(null, "cx")?.toLongOrNull()
                val cy = p.getAttributeValue(null, "cy")?.toLongOrNull()
                if (cx != null && cy != null && cx > 0 && cy > 0) return Pair(cx, cy)
            }
            ev = p.next()
        }
        return Pair(9144000L, 6858000L)
    }

    private fun slideOrder(zip: ZipFile): List<String> {
        val out = ArrayList<String>()
        val map = HashMap<String, String>()
        for (r in relsOf(zip, "ppt/presentation.xml")) map[r.id] = r.target
        val p = open(zip, "ppt/presentation.xml")
        if (p != null) {
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && p.name == "sldId") {
                    for (i in 0 until p.attributeCount) {
                        val ns = p.getAttributeNamespace(i) ?: ""
                        if (p.getAttributeName(i) == "id" && ns.contains("relationships")) {
                            val t = map[p.getAttributeValue(i)]
                            if (t != null) out.add(t)
                        }
                    }
                }
                ev = p.next()
            }
        }
        if (out.isEmpty()) {
            val re = Regex("ppt/slides/slide(\\d+)\\.xml")
            val found = zip.entries().toList()
                .map { it.name }
                .filter { re.matches(it) }
                .sortedBy { re.matchEntire(it)!!.groupValues[1].toInt() }
            out.addAll(found)
        }
        return out
    }

    private fun norm(type: String?): String {
        return when (type) {
            null, "obj", "subTitle" -> "body"
            "ctrTitle" -> "title"
            else -> type
        }
    }

    private fun collect(zip: ZipFile, entry: String, out: HashMap<String, LongArray>) {
        val e = zip.getEntry(entry) ?: return
        val raw = zip.getInputStream(e).use { PptxSlideReader.read(it, emptyMap()) }
        for (s in raw.shapes) {
            if (s.phType == null || !s.hasXfrm) continue
            val box = longArrayOf(s.x, s.y, s.w, s.h)
            out["type:" + norm(s.phType)] = box
            if (s.phIdx != null) out["idx:" + s.phIdx] = box
        }
    }

    private fun placeholders(
        zip: ZipFile,
        layout: String,
        cache: HashMap<String, Map<String, LongArray>>
    ): Map<String, LongArray> {
        val cached = cache[layout]
        if (cached != null) return cached
        val out = HashMap<String, LongArray>()
        val master = relsOf(zip, layout).firstOrNull { it.type.endsWith("/slideMaster") }
        if (master != null) collect(zip, master.target, out)
        collect(zip, layout, out)
        cache[layout] = out
        return out
    }

    private fun readSlide(
        zip: ZipFile,
        entry: String,
        w: Long,
        h: Long,
        cache: HashMap<String, Map<String, LongArray>>
    ): SlideData {
        val rels = relsOf(zip, entry)
        val relMap = HashMap<String, String>()
        for (r in rels) relMap[r.id] = r.target
        val layout = rels.firstOrNull { it.type.endsWith("/slideLayout") }
        val holders: Map<String, LongArray> =
            if (layout != null) placeholders(zip, layout.target, cache) else emptyMap()

        val e = zip.getEntry(entry) ?: return SlideData(emptyList(), 0)
        val raw = zip.getInputStream(e).use { PptxSlideReader.read(it, relMap) }

        val shapes = ArrayList<SlideShape>()
        for (r in raw.shapes) {
            val type = r.phType
            if (type == "dt" || type == "ftr" || type == "sldNum") continue
            var x = r.x
            var y = r.y
            var ww = r.w
            var hh = r.h
            if (!r.hasXfrm) {
                val idxKey = if (r.phIdx != null) "idx:" + r.phIdx else ""
                val box = holders[idxKey] ?: holders["type:" + norm(type)]
                if (box != null) {
                    x = box[0]
                    y = box[1]
                    ww = box[2]
                    hh = box[3]
                } else {
                    x = w / 16
                    y = h / 10
                    ww = w * 7 / 8
                    hh = h * 4 / 5
                }
            }
            val hasText = r.paras.any { it.plain.isNotBlank() }
            if (!hasText && r.image == null && r.table == null && r.fill == 0) continue
            val isTitle = type == "title" || type == "ctrTitle"
            shapes.add(SlideShape(x, y, ww, hh, r.fill, r.paras, r.image, r.table, isTitle))
        }
        return SlideData(shapes, raw.bg)
    }
}
