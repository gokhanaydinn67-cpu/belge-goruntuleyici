package com.belge.goruntuleyici

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.zip.ZipFile

object DocImage {

    fun load(zipPath: String, entry: String, maxWidth: Int): Bitmap? {
        return try {
            val zip = ZipFile(zipPath)
            try {
                val e = zip.getEntry(entry)
                if (e == null) {
                    null
                } else {
                    val bounds = BitmapFactory.Options()
                    bounds.inJustDecodeBounds = true
                    zip.getInputStream(e).use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }
                    var sample = 1
                    while (bounds.outWidth / sample > maxWidth * 2) sample *= 2
                    val opts = BitmapFactory.Options()
                    opts.inSampleSize = sample
                    zip.getInputStream(e).use {
                        BitmapFactory.decodeStream(it, null, opts)
                    }
                }
            } finally {
                zip.close()
            }
        } catch (t: Throwable) {
            null
        }
    }
}
