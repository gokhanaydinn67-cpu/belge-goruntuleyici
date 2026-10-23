package com.belge.goruntuleyici

class PdfMatch(val page: Int, val start: Int, val end: Int)

class PdfSearchResult(val matches: List<PdfMatch>, val texts: Map<Int, PdfPageText>)

object PdfSearch {

    private const val MAX_MATCHES = 3000

    fun find(index: PdfTextIndex, pageCount: Int, query: String): PdfSearchResult {
        val matches = ArrayList<PdfMatch>()
        val texts = HashMap<Int, PdfPageText>()
        val q = query.trim()
        if (q.isEmpty()) return PdfSearchResult(matches, texts)

        for (p in 0 until pageCount) {
            val t = index.page(p)
            if (t.chars.isEmpty()) continue

            val sb = StringBuilder()
            val map = ArrayList<Int>()
            for (i in t.chars.indices) {
                val c = t.chars[i]
                for (k in c.text.indices) {
                    sb.append(c.text[k])
                    map.add(i)
                }
                if (c.sep != 0) {
                    sb.append(' ')
                    map.add(i)
                }
            }

            val s = sb.toString()
            var from = 0
            var found = false
            while (true) {
                val at = s.indexOf(q, from, ignoreCase = true)
                if (at < 0) break
                val endPos = at + q.length - 1
                matches.add(PdfMatch(p, map[at], map[endPos]))
                found = true
                from = at + q.length
                if (matches.size >= MAX_MATCHES) break
            }
            if (found) texts[p] = t
            if (matches.size >= MAX_MATCHES) break
        }
        return PdfSearchResult(matches, texts)
    }
}
