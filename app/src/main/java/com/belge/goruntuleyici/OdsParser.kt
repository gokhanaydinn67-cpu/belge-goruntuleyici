package com.belge.goruntuleyici

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipFile

object OdsParser {

    fun parse(path: String): List<SheetData> {
        val zip = ZipFile(path)
        try {
            val entry = zip.getEntry("content.xml") ?: return emptyList()
            val p = Xml.newPullParser()
            p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            p.setInput(zip.getInputStream(entry), null)
            return read(p)
        } finally {
            zip.close()
        }
    }

    private fun intAttr(p: XmlPullParser, name: String): Int {
        return p.getAttributeValue(null, name)?.toIntOrNull() ?: 1
    }

    private fun read(p: XmlPullParser): List<SheetData> {
        val sheets = ArrayList<SheetData>()
        var tblDepth = 0
        var name = ""
        var rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        var rowRepeat = 1
        var pendingRows = 0
        var pendingCells = 0
        var inCell = false
        var inNote = false
        var colRepeat = 1
        var value = ""
        val text = StringBuilder()
        var pCount = 0
        var maxCols = 0

        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                when (p.name) {
                    "table" -> {
                        tblDepth++
                        if (tblDepth == 1) {
                            name = p.getAttributeValue(null, "name") ?: "Sheet"
                            rows = ArrayList()
                            pendingRows = 0
                            maxCols = 0
                        }
                    }
                    "table-row" -> {
                        if (tblDepth == 1) {
                            rowRepeat = intAttr(p, "number-rows-repeated")
                            row = ArrayList()
                            pendingCells = 0
                        }
                    }
                    "table-cell", "covered-table-cell" -> {
                        if (tblDepth == 1) {
                            inCell = true
                            colRepeat = intAttr(p, "number-columns-repeated")
                            value = p.getAttributeValue(null, "value")
                                ?: p.getAttributeValue(null, "date-value")
                                ?: p.getAttributeValue(null, "string-value")
                                ?: ""
                            text.setLength(0)
                            pCount = 0
                        }
                    }
                    "annotation" -> inNote = true
                    "p" -> {
                        if (inCell && !inNote) {
                            if (pCount > 0) text.append("\n")
                            pCount++
                        }
                    }
                    "s" -> {
                        if (inCell && !inNote) {
                            text.append(" ".repeat(intAttr(p, "c").coerceIn(1, 50)))
                        }
                    }
                    "tab" -> {
                        if (inCell && !inNote) text.append("    ")
                    }
                    "line-break" -> {
                        if (inCell && !inNote) text.append("\n")
                    }
                }
            } else if (ev == XmlPullParser.TEXT) {
                if (inCell && !inNote) text.append(p.text)
            } else if (ev == XmlPullParser.END_TAG) {
                when (p.name) {
                    "annotation" -> inNote = false
                    "table-cell", "covered-table-cell" -> {
                        if (tblDepth == 1 && inCell) {
                            inCell = false
                            val shown = if (text.isNotEmpty()) text.toString() else value
                            if (shown.isEmpty()) {
                                pendingCells += colRepeat.coerceAtMost(1000)
                            } else {
                                var k = pendingCells
                                while (k > 0 && row.size < SHEET_MAX_COLS) {
                                    row.add("")
                                    k--
                                }
                                pendingCells = 0
                                var n = colRepeat.coerceIn(1, 50)
                                while (n > 0 && row.size < SHEET_MAX_COLS) {
                                    row.add(shown)
                                    n--
                                }
                            }
                        }
                    }
                    "table-row" -> {
                        if (tblDepth == 1) {
                            if (row.isEmpty()) {
                                pendingRows += rowRepeat.coerceAtMost(1000)
                            } else {
                                var k = pendingRows
                                while (k > 0 && rows.size < SHEET_MAX_ROWS) {
                                    rows.add(emptyList())
                                    k--
                                }
                                pendingRows = 0
                                var n = rowRepeat.coerceIn(1, 50)
                                while (n > 0 && rows.size < SHEET_MAX_ROWS) {
                                    rows.add(row)
                                    n--
                                }
                                if (row.size > maxCols) maxCols = row.size
                            }
                        }
                    }
                    "table" -> {
                        if (tblDepth == 1) {
                            sheets.add(SheetData(name, rows, maxCols.coerceAtLeast(1)))
                        }
                        if (tblDepth > 0) tblDepth--
                    }
                }
            }
            ev = p.next()
        }
        return sheets
    }
}
