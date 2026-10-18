package com.belge.goruntuleyici

import android.graphics.Bitmap
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TiffPageInfo(
    val width: Int,
    val height: Int,
    val compression: Int,
    val photometric: Int,
    val fillOrder: Int,
    val bits: Int,
    val samples: Int,
    val tiled: Boolean,
    val rowsPerStrip: Int,
    val offsets: LongArray,
    val counts: LongArray
)

object TiffG4 {

    private const val BLACK = -0x1000000
    private const val WHITE = -1

    private var cachePath = ""
    private var cacheStamp = 0L
    private var cacheData: ByteArray? = null
    private var cacheInfos: List<TiffPageInfo>? = null

    fun open(path: String): Pair<ByteArray, List<TiffPageInfo>>? {
        val f = File(path)
        val stamp = f.length() * 31L + f.lastModified()
        val d = cacheData
        val c = cacheInfos
        if (d != null && c != null && cachePath == path && cacheStamp == stamp) {
            return Pair(d, c)
        }
        return try {
            val data = f.readBytes()
            val infos = parse(data)
            if (infos.isEmpty()) {
                null
            } else {
                cachePath = path
                cacheStamp = stamp
                cacheData = data
                cacheInfos = infos
                Pair(data, infos)
            }
        } catch (t: Throwable) {
            null
        }
    }

    fun supports(p: TiffPageInfo): Boolean {
        return p.compression == 4 && p.bits == 1 && p.samples == 1 && !p.tiled &&
            p.width > 0 && p.height > 0 && p.offsets.isNotEmpty()
    }

    private fun parse(d: ByteArray): List<TiffPageInfo> {
        val out = ArrayList<TiffPageInfo>()
        if (d.size < 8) return out
        val order: ByteOrder
        if (d[0] == 'I'.code.toByte() && d[1] == 'I'.code.toByte()) {
            order = ByteOrder.LITTLE_ENDIAN
        } else if (d[0] == 'M'.code.toByte() && d[1] == 'M'.code.toByte()) {
            order = ByteOrder.BIG_ENDIAN
        } else {
            return out
        }
        val bb = ByteBuffer.wrap(d).order(order)
        if ((bb.getShort(2).toInt() and 0xFFFF) != 42) return out
        var off = bb.getInt(4).toLong() and 0xFFFFFFFFL
        var guard = 0
        while (off > 0 && off + 2 <= d.size && guard < 5000) {
            guard++
            val base = off.toInt()
            val n = bb.getShort(base).toInt() and 0xFFFF
            val tags = HashMap<Int, LongArray>()
            for (k in 0 until n) {
                val e = base + 2 + 12 * k
                if (e + 12 > d.size) break
                val tag = bb.getShort(e).toInt() and 0xFFFF
                val type = bb.getShort(e + 2).toInt() and 0xFFFF
                val cnt = bb.getInt(e + 4).toLong() and 0xFFFFFFFFL
                val size = when (type) {
                    1, 2, 6, 7 -> 1
                    3, 8 -> 2
                    4, 9, 11 -> 4
                    else -> 8
                }
                val total = size.toLong() * cnt
                val dataPos: Long = if (total <= 4L) {
                    (e + 8).toLong()
                } else {
                    bb.getInt(e + 8).toLong() and 0xFFFFFFFFL
                }
                if (cnt > 200000L || dataPos + total > d.size) continue
                val vals = LongArray(cnt.toInt())
                for (j in vals.indices) {
                    val p = (dataPos + j.toLong() * size).toInt()
                    vals[j] = when (size) {
                        1 -> d[p].toLong() and 0xFFL
                        2 -> bb.getShort(p).toLong() and 0xFFFFL
                        4 -> bb.getInt(p).toLong() and 0xFFFFFFFFL
                        else -> bb.getLong(p)
                    }
                }
                tags[tag] = vals
            }

            val w = tags[256]?.getOrNull(0)?.toInt() ?: 0
            val h = tags[257]?.getOrNull(0)?.toInt() ?: 0
            var rps = tags[278]?.getOrNull(0)?.toInt() ?: h
            if (rps <= 0 || rps > h) rps = h
            out.add(
                TiffPageInfo(
                    width = w,
                    height = h,
                    compression = tags[259]?.getOrNull(0)?.toInt() ?: 1,
                    photometric = tags[262]?.getOrNull(0)?.toInt() ?: 0,
                    fillOrder = tags[266]?.getOrNull(0)?.toInt() ?: 1,
                    bits = tags[258]?.getOrNull(0)?.toInt() ?: 1,
                    samples = tags[277]?.getOrNull(0)?.toInt() ?: 1,
                    tiled = tags.containsKey(322),
                    rowsPerStrip = rps,
                    offsets = tags[273] ?: LongArray(0),
                    counts = tags[279] ?: LongArray(0)
                )
            )

            val nextPos = base + 2 + 12 * n
            if (nextPos + 4 > d.size) break
            off = bb.getInt(nextPos).toLong() and 0xFFFFFFFFL
        }
        return out
    }

    fun render(data: ByteArray, p: TiffPageInfo): Bitmap? {
        val w = p.width
        val h = p.height
        if (w.toLong() * h.toLong() > 60000000L) return null
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(WHITE)
        val blackBit = if (p.photometric == 1) 0 else 1
        val row = IntArray(w)
        var y = 0
        for (s in p.offsets.indices) {
            val rows = minOf(p.rowsPerStrip, h - y)
            if (rows <= 0) break
            val start = p.offsets[s].toInt()
            var end = if (s < p.counts.size) start + p.counts[s].toInt() else data.size
            if (end > data.size) end = data.size
            if (start >= 0 && start < end) {
                val baseY = y
                G4Decoder.decodeStrip(data, start, end, p.fillOrder == 2, w, rows) { r, cur, n ->
                    var prev = 0
                    for (k in 0 until n) {
                        val pos = cur[k]
                        if (pos > prev) {
                            val c = if ((k and 1) == blackBit) BLACK else WHITE
                            java.util.Arrays.fill(row, prev, pos, c)
                            prev = pos
                        }
                    }
                    if (prev < w) {
                        val c = if ((n and 1) == blackBit) BLACK else WHITE
                        java.util.Arrays.fill(row, prev, w, c)
                    }
                    bmp.setPixels(row, 0, w, 0, baseY + r, w, 1)
                }
            }
            y += rows
        }
        return bmp
    }
}
