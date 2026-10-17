package com.belge.goruntuleyici

import android.graphics.Bitmap
import org.beyka.tiffbitmapfactory.TiffBitmapFactory
import java.io.File

object TiffLoader {

    private val lock = Any()

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
                TiffBitmapFactory.decodeFile(file, d)
            } catch (t: Throwable) {
                null
            }
        }
    }
}
