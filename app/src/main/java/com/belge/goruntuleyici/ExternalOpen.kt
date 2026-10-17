package com.belge.goruntuleyici

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File

object ExternalOpen {

    fun fromIntent(context: Context, intent: Intent?): DocFile? {
        if (intent == null || intent.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        return try {
            copyToCache(context, uri, intent.type)
        } catch (t: Throwable) {
            null
        }
    }

    private fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                val cols = arrayOf(OpenableColumns.DISPLAY_NAME)
                context.contentResolver.query(uri, cols, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (i >= 0) return c.getString(i)
                    }
                }
            } catch (t: Throwable) {
                // ad alınamazsa yoldan tahmin edilir
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun cleanOld(dir: File) {
        val limit = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        val list = dir.listFiles() ?: return
        for (f in list) {
            if (f.lastModified() < limit) f.delete()
        }
    }

    private fun copyToCache(context: Context, uri: Uri, mime: String?): DocFile? {
        var name = displayName(context, uri) ?: "belge"
        name = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        var ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty() || ext.length > 6) {
            val fromMime = if (mime != null) {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            } else {
                null
            }
            if (fromMime != null) {
                name = "$name.$fromMime"
                ext = fromMime
            }
        }
        val dir = File(context.cacheDir, "external")
        dir.mkdirs()
        cleanOld(dir)
        val out = File(dir, System.currentTimeMillis().toString() + "_" + name)
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { i ->
            out.outputStream().use { o -> i.copyTo(o) }
        }
        return DocFile(name, out.absolutePath, ext, out.length(), out.lastModified())
    }
}
