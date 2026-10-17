package com.belge.goruntuleyici

import android.graphics.Bitmap
import org.beyka.tiffbitmapfactory.TiffBitmapFactory
import java.io.File

object TiffLoader {

    private val lock = Any()
    private const val TARGET_WIDTH = 2000

    fun pageCount(path: String): Int {
        synchronized(lock) {
            return try {
                val o = TiffBitmapFactory.Options()
                o.inJustDecodeBounds = true
                TiffBitmapFactory.decodeFile(File(path), o)
                o.outDirectoryCount.coerceAtLeast(1)
            } catch (t: Throwable) {
                1
            }
        }
    }

    private fun downscale(src: Bitmap, target: Int): Bitmap {
        var b = src
        while (b.width / 2 >= target) {
            val half = Bitmap.createScaledBitmap(b, b.width / 2, b.height / 2, true)
            if (b !== src) b.recycle()
            b = half
        }
        if (b.width > target) {
            val h = (b.height.toLong() * target / b.width).toInt().coerceAtLeast(1)
            val s = Bitmap.createScaledBitmap(b, target, h, true)
            if (b !== src) b.recycle()
            b = s
        }
        return b
    }

    fun page(path: String, index: Int): Bitmap? {
        synchronized(lock) {
            return try {
                val file = File(path)
                val o = TiffBitmapFactory.Options()
                o.inJustDecodeBounds = true
                o.inDirectoryNumber = index
                TiffBitmapFactory.decodeFile(file, o)
                var sample = 1
                while ((o.outWidth.toLong() / sample) * (o.outHeight.toLong() / sample) > 16000000L) {
                    sample *= 2
                }
                val d = TiffBitmapFactory.Options()
                d.inDirectoryNumber = index
                d.inSampleSize = sample
                val full = TiffBitmapFactory.decodeFile(file, d)
                if (full == null) {
                    null
                } else {
                    val result = downscale(full, TARGET_WIDTH)
                    if (result !== full) full.recycle()
                    result
                }
            } catch (t: Throwable) {
                null
            }
        }
    }
}
