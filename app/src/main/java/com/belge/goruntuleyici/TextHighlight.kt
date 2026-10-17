package com.belge.goruntuleyici

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

fun highlightLine(line: String, query: String, strong: Boolean): AnnotatedString {
    val text = if (line.isEmpty()) " " else line
    if (query.isBlank()) return AnnotatedString(text)
    val color = if (strong) Color(0xFFFF9800) else Color(0xFFFFF176)
    return buildAnnotatedString {
        var start = 0
        while (true) {
            val i = text.indexOf(query, start, ignoreCase = true)
            if (i < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, i))
            withStyle(SpanStyle(background = color)) {
                append(text.substring(i, i + query.length))
            }
            start = i + query.length
        }
    }
}
