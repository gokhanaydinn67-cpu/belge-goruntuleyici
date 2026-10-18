package com.belge.goruntuleyici

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileOps {

    fun size(b: Long): String {
        return when {
            b < 1024 -> "$b B"
            b < 1024 * 1024 -> "${b / 1024} KB"
            else -> String.format(Locale.US, "%.1f MB", b / 1048576.0)
        }
    }

    private fun uniqueTarget(dir: File, name: String): File {
        val first = File(dir, name)
        if (!first.exists()) return first
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        for (i in 1..9999) {
            val f = File(dir, "$base ($i)$ext")
            if (!f.exists()) return f
        }
        return File(dir, "$base (" + System.currentTimeMillis() + ")$ext")
    }

    fun move(files: List<DocFile>, dest: File): Int {
        var ok = 0
        for (f in files) {
            try {
                val src = File(f.path)
                if (src.parentFile?.absolutePath == dest.absolutePath) continue
                val target = uniqueTarget(dest, src.name)
                if (!src.renameTo(target)) {
                    src.copyTo(target, overwrite = false)
                    src.delete()
                }
                ok++
            } catch (t: Throwable) {
                // bu dosya taşınamadı
            }
        }
        return ok
    }

    fun delete(files: List<DocFile>): Int {
        var ok = 0
        for (f in files) {
            try {
                if (File(f.path).delete()) ok++
            } catch (t: Throwable) {
                // bu dosya silinemedi
            }
        }
        return ok
    }

    fun rename(file: DocFile, newName: String): Boolean {
        val n = newName.trim()
        if (n.isEmpty() || n.contains('/') || n.startsWith(".")) return false
        return try {
            val src = File(file.path)
            val target = File(src.parentFile, n)
            if (target.exists()) false else src.renameTo(target)
        } catch (t: Throwable) {
            false
        }
    }

    fun openOther(context: Context, f: DocFile) {
        try {
            val uri = FileProvider.getUriForFile(
                context, "com.belge.goruntuleyici.fileprovider", File(f.path)
            )
            val view = Intent(Intent.ACTION_VIEW)
            view.setDataAndType(uri, ShareHelper.mimeOf(f.ext))
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(view, "Şununla aç")
            chooser.putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                arrayOf(ComponentName(context, MainActivity::class.java))
            )
            context.startActivity(chooser)
        } catch (t: Throwable) {
            Toast.makeText(context, "Açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    fun details(files: List<DocFile>): String {
        if (files.size == 1) {
            val f = files[0]
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(Date(f.modified))
            return "Ad: " + f.name + "\n" +
                "Klasör: " + (File(f.path).parent ?: "") + "\n" +
                "Tür: " + f.ext.uppercase() + "\n" +
                "Boyut: " + size(f.size) + " (" + f.size + " bayt)\n" +
                "Değiştirilme: " + date
        }
        var total = 0L
        for (f in files) total += f.size
        return files.size.toString() + " dosya seçildi\nToplam boyut: " + size(total)
    }
}
