package com.belge.goruntuleyici

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

object TextLoader {

    private const val LIMIT = 5 * 1024 * 1024

    fun load(path: String): String {
        if (path.endsWith(".udf", ignoreCase = true)) {
            return UdfReader.read(path)
        }
        val f = File(path)
        val size = minOf(f.length(), LIMIT.toLong()).toInt()
        val bytes = ByteArray(size)
        FileInputStream(f).use { input ->
            var read = 0
            while (read < size) {
                val n = input.read(bytes, read, size - read)
                if (n < 0) break
                read += n
            }
        }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: Exception) {
            String(bytes, charset("windows-1254"))
        }
    }
}
