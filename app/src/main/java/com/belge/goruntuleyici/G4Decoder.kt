package com.belge.goruntuleyici

class BitReader(
    private val d: ByteArray,
    start: Int,
    private val end: Int,
    private val lsbFirst: Boolean
) {
    private var pos = start * 8

    fun bit(): Int {
        if (pos >= end * 8) throw IllegalStateException("eof")
        val b = d[pos shr 3].toInt()
        val shift = if (lsbFirst) (pos and 7) else 7 - (pos and 7)
        pos++
        return (b shr shift) and 1
    }
}

object G4Decoder {

    private const val PASS = 1000
    private const val HORIZ = 1001

    private val whiteMap = HashMap<Int, Int>()
    private val blackMap = HashMap<Int, Int>()

    init {
        load(CcittTables.WHITE, whiteMap)
        load(CcittTables.BLACK, blackMap)
    }

    private fun load(spec: String, map: HashMap<Int, Int>) {
        for (item in spec.split(',')) {
            val p = item.split(':')
            val code = p[1]
            map[(code.length shl 16) or code.toInt(2)] = p[0].toInt()
        }
    }

    private fun readRun(br: BitReader, black: Boolean): Int {
        val table = if (black) blackMap else whiteMap
        var total = 0
        while (true) {
            var code = 0
            var len = 0
            var run = -1
            while (len < 14) {
                code = (code shl 1) or br.bit()
                len++
                val r = table[(len shl 16) or code]
                if (r != null) {
                    run = r
                    break
                }
            }
            if (run < 0) throw IllegalStateException("bad run")
            total += run
            if (run < 64) return total
        }
    }

    private fun readMode(br: BitReader): Int {
        var code = 0
        var len = 0
        while (len < 7) {
            code = (code shl 1) or br.bit()
            len++
            if (len == 1 && code == 1) return 0
            if (len == 3) {
                if (code == 3) return 1
                if (code == 2) return -1
                if (code == 1) return HORIZ
            }
            if (len == 4 && code == 1) return PASS
            if (len == 6) {
                if (code == 3) return 2
                if (code == 2) return -2
            }
            if (len == 7) {
                if (code == 3) return 3
                if (code == 2) return -3
            }
        }
        throw IllegalStateException("bad mode")
    }

    // Her satır için onLine(satırNo, değişimNoktaları, adet) çağrılır.
    // Değişim noktası k için: koşunun rengi = k ve 1 (0 = "beyaz kod", 1 = "siyah kod").
    fun decodeStrip(
        data: ByteArray,
        start: Int,
        end: Int,
        lsbFirst: Boolean,
        width: Int,
        rows: Int,
        onLine: (Int, IntArray, Int) -> Unit
    ): Int {
        val br = BitReader(data, start, end, lsbFirst)
        var ref = IntArray(width + 8)
        var refN = 2
        ref[0] = width
        ref[1] = width
        var cur = IntArray(width + 8)
        var done = 0
        try {
            for (r in 0 until rows) {
                var n = 0
                var a0 = -1
                var color = 0
                while (a0 < width) {
                    var i = 0
                    while (i < refN && !(ref[i] > a0 && (i and 1) == color)) i++
                    val b1 = if (i < refN) ref[i] else width
                    val b2 = if (i + 1 < refN) ref[i + 1] else width
                    val m = readMode(br)
                    if (n + 2 >= cur.size - 2) throw IllegalStateException("overflow")
                    if (m == PASS) {
                        a0 = b2
                    } else if (m == HORIZ) {
                        val r1 = readRun(br, color == 1)
                        val r2 = readRun(br, color == 0)
                        val s = if (a0 < 0) 0 else a0
                        val a1 = minOf(s + r1, width)
                        val a2 = minOf(a1 + r2, width)
                        cur[n++] = a1
                        cur[n++] = a2
                        a0 = a2
                    } else {
                        val a1 = (b1 + m).coerceIn(0, width)
                        cur[n++] = a1
                        a0 = a1
                        color = color xor 1
                    }
                }
                onLine(r, cur, n)
                cur[n] = width
                cur[n + 1] = width
                val tmp = ref
                ref = cur
                cur = tmp
                refN = n + 2
                done++
            }
        } catch (e: Exception) {
            // bozuk veri: kalan satırlar boş kalır
        }
        return done
    }
}
