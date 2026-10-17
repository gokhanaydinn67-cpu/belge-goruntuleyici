package com.belge.goruntuleyici

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    fun mimeOf(ext: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    fun share(context: Context, files: List<DocFile>) {
        if (files.isEmpty()) return
        try {
            val uris = ArrayList<Uri>()
            for (f in files) {
                uris.add(
                    FileProvider.getUriForFile(
                        context,
                        "com.belge.goruntuleyici.fileprovider",
                        File(f.path)
                    )
                )
            }
            val intent: Intent
            if (uris.size == 1) {
                intent = Intent(Intent.ACTION_SEND)
                intent.type = mimeOf(files[0].ext)
                intent.putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                val types = files.map { mimeOf(it.ext) }.distinct()
                intent = Intent(Intent.ACTION_SEND_MULTIPLE)
                intent.type = if (types.size == 1) types[0] else "*/*"
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, "Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(context, "Paylaşılamadı", Toast.LENGTH_SHORT).show()
        }
    }
}
