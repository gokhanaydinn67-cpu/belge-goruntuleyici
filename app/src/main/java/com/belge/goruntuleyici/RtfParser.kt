package com.belge.goruntuleyici

import java.io.File
import java.nio.charset.Charset

object RtfParser {

    private val skipGroups = setOf(
        "fonttbl", "colortbl", "stylesheet", "info", "pict", "header", "footer",
        "generator", "listtable", "listoverridetable", "rsidtbl", "themedata",
        "colorschememapping", "datastore", "latentstyles", "filetbl", "revtbl",
        "xmlnstbl", "fldinst"
    )

    fun parse(path: String): List<DocBlock> {
        val s = String(File(path).readBytes(), Charsets.ISO_8859_1)
        val n = s.length
        val blocks = ArrayList<DocBlock>()
        val sb = StringBuilder()
        val stack = ArrayList<Boolean>()
        var skip = false
        var cs: Charset = Charset.forName("windows-1254")

        fun flush() {
            val text = sb.toString()
            val runs = if (text.isEmpty()) emptyList() else listOf(DocRun(text))
            blocks.add(ParaBlock(runs))
            sb.setLength(0)
        }

        var i = 0
        while (i < n) {
            val c = s[i]
            if (c == '{') {
                stack.add(skip)
                i++
            } else if (c == '}') {
                if (stack.isNotEmpty()) skip = stack.removeAt(stack.size - 1)
                i++
            } else if (c == '\\') {
                i++
                if (i >= n) break
                val d = s[i]
                if (d == '\\' || d == '{' || d == '}') {
                    if (!skip) sb.append(d)
                    i++
                } else if (d == '\'') {
                    val end = minOf(i + 3, n)
                    val b = s.substring(i + 1, end).toIntOrNull(16)
                    if (b != null && !skip) {
                        sb.append(String(byteArrayOf(b.toByte()), cs))
                    }
                    i += 3
                } else if (d == '*') {
                    skip = true
                    i++
                } else if (d == '~') {
                    if (!skip) sb.append(' ')
                    i++
                } else if (d == '\n' || d == '\r') {
                    if (!skip) flush()
                    i++
                } else if (d.isLetter()) {
                    var j = i
                    while (j < n && s[j].isLetter()) j++
                    val word = s.substring(i, j)
                    var k = j
                    var neg = false
                    if (k < n && s[k] == '-') {
                        neg = true
                        k++
                    }
                    val ds = k
                    while (k < n && s[k].isDigit()) k++
                    var param: Int? = null
                    if (k > ds) {
                        param = s.substring(ds, k).toIntOrNull()
                        if (param != null && neg) param = -param
                    }
                    if (k < n && s[k] == ' ') k++
                    i = k

                    if (word in skipGroups) {
                        skip = true
                    } else if (word == "ansicpg" && param != null) {
                        try {
                            cs = Charset.forName("windows-$param")
                        } catch (e: Exception) {
                            cs = Charset.forName("windows-1254")
                        }
                    } else if (!skip) {
                        if (word == "par") {
                            flush()
                        } else if (word == "line") {
                            sb.append('\n')
                        } else if (word == "tab") {
                            sb.append("    ")
                        } else if (word == "cell") {
                            sb.append("   ")
                        } else if (word == "row") {
                            flush()
                        } else if (word == "u" && param != null) {
                            val code = if (param < 0) param + 65536 else param
                            sb.append(code.toChar())
                            if (i < n && s[i] == '\\' && i + 1 < n && s[i + 1] == '\'') {
                                i += 4
                            } else if (i < n) {
                                i++
                            }
                        }
                    }
                } else {
                    i++
                }
            } else if (c == '\r' || c == '\n') {
                i++
            } else {
                if (!skip) sb.append(String(byteArrayOf(c.code.toByte()), cs))
                i++
            }
        }
        if (sb.isNotEmpty()) flush()
        return blocks
    }
}
