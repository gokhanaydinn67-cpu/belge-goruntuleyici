package com.belge.goruntuleyici

const val SHEET_MAX_ROWS = 10000
const val SHEET_MAX_COLS = 100

class SheetData(val name: String, val rows: List<List<String>>, val cols: Int)

object SheetLoader {

    fun load(path: String, ext: String): List<SheetData> {
        return try {
            when (ext) {
                "xlsx", "xlsm", "xltx" -> XlsxParser.parse(path)
                "ods" -> OdsParser.parse(path)
                else -> emptyList()
            }
        } catch (t: Throwable) {
            val m = "Hata: " + t.javaClass.simpleName + " - " + t.message
            listOf(SheetData("Hata", listOf(listOf(m)), 1))
        }
    }

    fun colName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.insert(0, 'A' + n % 26)
            n = n / 26 - 1
        }
        return sb.toString()
    }
}
