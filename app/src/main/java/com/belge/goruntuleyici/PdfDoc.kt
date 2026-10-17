package com.belge.goruntuleyici

import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

class PdfDoc(path: String) {
    private val pfd = ParcelFileDescriptor.open(
        File(path), ParcelFileDescriptor.MODE_READ_ONLY
    )
    private val renderer = PdfRenderer(pfd)
    val count: Int = renderer.pageCount

    fun render(index: Int, width: Int): Bitmap {
        synchronized(this) {
            val page = renderer.openPage(index)
            val h = (width.toFloat() / page.width * page.height).toInt()
            val bmp = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(AColor.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return bmp
        }
    }

    fun close() {
        synchronized(this) {
            renderer.close()
            pfd.close()
        }
    }
}
