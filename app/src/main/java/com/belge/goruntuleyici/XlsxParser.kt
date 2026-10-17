package com.belge.goruntuleyici

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.math.BigDecimal
import java.math.MathContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipFile

object XlsxParser {

    private fun parser(zip: ZipFile, name: String): XmlPullParser? {
        val e = zip.getEntry(name) ?: return null
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        p.setInput(zip.getInputStream(e), null)
        return p
    }

    fun parse(path: String): List<SheetData> {
        val zip = ZipFile(path)
        try {
            val shared = readShared(zip)
            val dateStyles = readDateStyles(zip)
            val rels = readRels(zip)
            val result = ArrayList<SheetData>()
            for ((name, rid) in readWorkbook(zip)) {
                val target = rels[rid] ?: continue
                val entry = if (target.startsWith("/")) target.substring(1) else "xl/$target"
                val p = parser(zip, entry) ?: continue
                result.add(readSheet(p, name, shared, dateStyles))
            }
            return result
        } finally {
            zip.close()
        }
    }

    private fun readWorkbook(zip: ZipFile): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val p = parser(zip, "xl/workbook.xml") ?: return out
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && p.name == "sheet") {
                val name = p.getAttributeValue(null, "name") ?: "Sheet"
                var rid = ""
                for (i in 0 until p.attributeCount) {
                    if (p.getAttributeName(i) == "id") rid = p.getAttributeValue(i)
                }
                out.add(Pair(name, rid))
            }
            ev = p.next()
        }
        return out
    }

    private fun readRels(zip: ZipFile): Map<String, String> {
        val map = HashMap<String, String>()
        val p = parser(zip, "xl/_rels/workbook.xml.rels") ?: return map
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

    private fun readShared(zip: ZipFile): List<String> {
        val out = ArrayList<String>()
        val p = parser(zip, "xl/sharedStrings.xml") ?: return out
        var sb = StringBuilder()
        var inSi = false
        var inPhonetic = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                when (p.name) {
                    "si" -> {
                        sb = StringBuilder()
                        inSi = true
                    }
                    "rPh" -> inPhonetic = true
                    "t" -> {
                        if (inSi && !inPhonetic) sb.append(p.nextText())
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                when (p.name) {
                    "rPh" -> inPhonetic = false
                    "si" -> {
                        out.add(sb.toString())
                        inSi = false
                    }
                }
            }
            ev = p.next()
        }
        return out
    }

    private fun isBuiltinDate(id: Int): Boolean {
        return id in 14..22 || id in 27..36 || id in 45..47 || id in 50..58
    }

    private fun isDateCode(code: String): Boolean {
        var s = code.lowercase()
        s = s.replace(Regex("\"[^\"]*\""), "")
        s = s.replace(Regex("\\[[^\\]]*\\]"), "")
        s = s.replace(Regex("\\\\."), "")
        return s.any { it == 'y' || it == 'd' || it == 'm' || it == 'h' || it == 's' }
    }

    private fun readDateStyles(zip: ZipFile): List<Boolean> {
        val out = ArrayList<Boolean>()
        val p = parser(zip, "xl/styles.xml") ?: return out
        val custom = HashMap<Int, Boolean>()
        var inXfs = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                when (p.name) {
                    "numFmt" -> {
                        val id = p.getAttributeValue(null, "numFmtId")?.toIntOrNull()
                        val code = p.getAttributeValue(null, "formatCode") ?: ""
                        if (id != null) custom[id] = isDateCode(code)
                    }
                    "cellXfs" -> inXfs = true
                    "xf" -> {
                        if (inXfs) {
                            val id = p.getAttributeValue(null, "numFmtId")?.toIntOrNull() ?: 0
                            out.add(isBuiltinDate(id) || custom[id] == true)
                        }
                    }
                }
            } else if (ev == XmlPullParser.END_TAG && p.name == "cellXfs") {
                inXfs = false
            }
            ev = p.next()
        }
        return out
    }

    private fun colIndex(ref: String): Int {
        var n = 0
        for (ch in ref) {
            if (ch in 'A'..'Z') {
                n = n * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                n = n * 26 + (ch - 'a' + 1)
            } else {
                break
            }
        }
        return n - 1
    }

    private fun dateText(d: Double): String {
        val days = Math.floor(d)
        val frac = d - days
        val fmt = if (days < 1.0) {
            "HH:mm"
        } else if (frac > 0.0000001) {
            "dd.MM.yyyy HH:mm"
        } else {
            "dd.MM.yyyy"
        }
        val sdf = SimpleDateFormat(fmt, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val ms = Math.round((d - 25569.0) * 86400000.0)
        return sdf.format(Date(ms))
    }

    private fun number(v: String, isDate: Boolean): String {
        val s = v.trim()
        if (s.isEmpty()) return ""
        val d = s.toDoubleOrNull() ?: return v
        if (isDate) return dateText(d)
        if (d == Math.floor(d) && Math.abs(d) < 1e15) return d.toLong().toString()
        return BigDecimal.valueOf(d).round(MathContext(12)).stripTrailingZeros().toPlainString()
    }

    private fun display(
        type: String,
        style: Int,
        v: String,
        shared: List<String>,
        dateStyles: List<Boolean>
    ): String {
        return when (type) {
            "s" -> shared.getOrNull(v.trim().toIntOrNull() ?: -1) ?: ""
            "b" -> if (v.trim() == "1") "TRUE" else "FALSE"
            "str", "inlineStr", "e" -> v
            else -> number(v, dateStyles.getOrNull(style) == true)
        }
    }

    private fun readSheet(
        p: XmlPullParser,
        name: String,
        shared: List<String>,
        dateStyles: List<Boolean>
    ): SheetData {
        val rows = ArrayList<List<String>>()
        var maxCols = 0
        var row = ArrayList<String>()
        var rowNum = 0
        var col = -1
        var type = ""
        var style = 0
        var value = ""
        var inIs = false
        var done = false

        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT && !done) {
            if (ev == XmlPullParser.START_TAG) {
                when (p.name) {
                    "row" -> {
                        val r = p.getAttributeValue(null, "r")?.toIntOrNull()
                        rowNum = r ?: (rowNum + 1)
                        col = -1
                        if (rowNum > SHEET_MAX_ROWS) {
                            done = true
                        } else {
                            while (rows.size < rowNum - 1) rows.add(emptyList())
                            row = ArrayList()
                        }
                    }
                    "c" -> {
                        val ref = p.getAttributeValue(null, "r")
                        col = if (ref != null) colIndex(ref) else col + 1
                        type = p.getAttributeValue(null, "t") ?: ""
                        style = p.getAttributeValue(null, "s")?.toIntOrNull() ?: 0
                        value = ""
                    }
                    "is" -> inIs = true
                    "v" -> value = p.nextText()
                    "t" -> {
                        if (inIs) value += p.nextText()
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                when (p.name) {
                    "is" -> inIs = false
                    "c" -> {
                        if (col in 0 until SHEET_MAX_COLS) {
                            val text = display(type, style, value, shared, dateStyles)
                            while (row.size <= col) row.add("")
                            row[col] = text
                        }
                    }
                    "row" -> {
                        if (!done) {
                            rows.add(row)
                            if (row.size > maxCols) maxCols = row.size
                        }
                    }
                }
            }
            ev = p.next()
        }
        return SheetData(name, rows, maxCols.coerceAtLeast(1))
    }
}
