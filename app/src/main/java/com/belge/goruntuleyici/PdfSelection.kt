package com.belge.goruntuleyici

import kotlin.math.abs

const val HANDLE_DROP_DP = 10f
const val HANDLE_RADIUS_DP = 9f

class PdfChar(
    val text: String,
    val l: Float,
    val t: Float,
    val r: Float,
    val b: Float,
    var sep: Int
)

class PdfPageText(val chars: List<PdfChar>, val pageW: Float, val pageH: Float)

class PdfSel(val page: Int, val start: Int, val end: Int)

object PdfSelect {

    fun nearest(text: PdfPageText, fx: Float, fy: Float): Pair<Int, Float> {
        var best = -1
        var bestScore = Float.MAX_VALUE
        for (i in text.chars.indices) {
            val c = text.chars[i]
            val dx = if (fx < c.l) c.l - fx else if (fx > c.r) fx - c.r else 0f
            val dy = if (fy < c.t) c.t - fy else if (fy > c.b) fy - c.b else 0f
            val s = dy * text.pageH * 2f + dx * text.pageW
            if (s < bestScore) {
                bestScore = s
                best = i
            }
        }
        return Pair(best, bestScore)
    }

    fun wordRange(text: PdfPageText, idx: Int): Pair<Int, Int> {
        var a = idx
        var b = idx
        while (a > 0 && text.chars[a - 1].sep == 0) a--
        while (b < text.chars.size - 1 && text.chars[b].sep == 0) b++
        return Pair(a, b)
    }

    fun selectedText(text: PdfPageText, start: Int, end: Int): String {
        if (text.chars.isEmpty()) return ""
        val lo = minOf(start, end).coerceIn(0, text.chars.size - 1)
        val hi = maxOf(start, end).coerceIn(0, text.chars.size - 1)
        val sb = StringBuilder()
        for (i in lo..hi) {
            val c = text.chars[i]
            sb.append(c.text)
            if (i < hi) {
                if (c.sep == 1) sb.append(' ') else if (c.sep == 2) sb.append('\n')
            }
        }
        return sb.toString()
    }

    fun lineRects(text: PdfPageText, start: Int, end: Int): List<FloatArray> {
        val out = ArrayList<FloatArray>()
        if (text.chars.isEmpty()) return out
        val lo = minOf(start, end).coerceIn(0, text.chars.size - 1)
        val hi = maxOf(start, end).coerceIn(0, text.chars.size - 1)
        var l = 0f
        var t = 0f
        var r = 0f
        var b = 0f
        var open = false
        for (i in lo..hi) {
            val c = text.chars[i]
            if (!open) {
                l = c.l
                t = c.t
                r = c.r
                b = c.b
                open = true
            } else {
                val h = b - t
                val sameLine = abs(c.b - b) < h * 0.6f
                val gapOk = (c.l - r) * text.pageW < h * text.pageH * 2.5f
                if (sameLine && gapOk) {
                    l = minOf(l, c.l)
                    r = maxOf(r, c.r)
                    t = minOf(t, c.t)
                    b = maxOf(b, c.b)
                } else {
                    out.add(floatArrayOf(l, t, r, b))
                    l = c.l
                    t = c.t
                    r = c.r
                    b = c.b
                }
            }
        }
        if (open) out.add(floatArrayOf(l, t, r, b))
        return out
    }
}
