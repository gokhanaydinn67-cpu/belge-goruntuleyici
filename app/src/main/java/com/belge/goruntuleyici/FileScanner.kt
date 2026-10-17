package com.belge.goruntuleyici

import android.os.Environment
import java.io.File

data class DocFile(
    val name: String,
    val path: String,
    val ext: String,
    val size: Long,
    val modified: Long
)

object FileScanner {

    val extensions = setOf(
        "doc", "docx", "docm", "dot", "dotx",
        "xls", "xlsx", "xlsm", "xlsb", "xlt", "xltx",
        "ppt", "pptx", "pps", "ppsx", "pptm",
        "odt", "ods", "odp",
        "txt", "rtf", "log",
        "csv", "tsv",
        "md", "xml", "json", "html", "epub",
        "pdf", "udf", "tif", "tiff"
    )

    fun scan(): List<DocFile> {
        val result = ArrayList<DocFile>()
        walk(Environment.getExternalStorageDirectory(), result)
        return result.sortedByDescending { it.modified }
    }

    private fun walk(dir: File, out: MutableList<DocFile>) {
        val children = dir.listFiles() ?: return
        for (f in children) {
            if (f.name.startsWith(".")) continue
            if (f.isDirectory) {
                if (f.name == "Android") continue
                walk(f, out)
            } else {
                val ext = f.extension.lowercase()
                if (ext in extensions) {
                    out.add(
                        DocFile(
                            f.name,
                            f.absolutePath,
                            ext,
                            f.length(),
                            f.lastModified()
                        )
                    )
                }
            }
        }
    }
}
