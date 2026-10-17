package com.belge.goruntuleyici

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun sheetColWidths(sheet: SheetData): List<Int> {
    val maxLen = IntArray(sheet.cols)
    val n = minOf(sheet.rows.size, 300)
    for (r in 0 until n) {
        val row = sheet.rows[r]
        for (c in row.indices) {
            if (c < maxLen.size && row[c].length > maxLen[c]) maxLen[c] = row[c].length
        }
    }
    return maxLen.map { (it * 8 + 20).coerceIn(64, 260) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SheetGrid(
    sheet: SheetData,
    zoom: Float,
    query: String,
    focusRow: Int,
    focusCol: Int,
    listState: LazyListState,
    hScroll: ScrollState,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val widths = remember(sheet) { sheetColWidths(sheet) }
    val rowNumW = 44.dp * zoom
    val cellH = 30.dp * zoom
    val headBg = Color(0xFFF2F2F2)
    val line = Color(0xFFBDBDBD)

    Box(modifier = modifier.horizontalScroll(hScroll)) {
        Column {
            Row {
                Box(
                    modifier = Modifier
                        .width(rowNumW)
                        .height(cellH)
                        .background(headBg)
                        .border(0.5.dp, line)
                )
                for (c in 0 until sheet.cols) {
                    Box(
                        modifier = Modifier
                            .width(widths[c].dp * zoom)
                            .height(cellH)
                            .background(headBg)
                            .border(0.5.dp, line),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = SheetLoader.colName(c),
                            color = Color.Black,
                            fontSize = (12f * zoom).sp
                        )
                    }
                }
            }
            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                items(sheet.rows.size) { r ->
                    val row = sheet.rows[r]
                    Row {
                        Box(
                            modifier = Modifier
                                .width(rowNumW)
                                .height(cellH)
                                .background(headBg)
                                .border(0.5.dp, line),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (r + 1).toString(),
                                color = Color.Black,
                                fontSize = (12f * zoom).sp
                            )
                        }
                        for (c in 0 until sheet.cols) {
                            val text = row.getOrNull(c) ?: ""
                            val isFocus = r == focusRow && c == focusCol
                            val hit = query.isNotBlank() &&
                                text.contains(query, ignoreCase = true)
                            val bg = if (isFocus) {
                                Color(0xFFFF9800)
                            } else if (hit) {
                                Color(0xFFFFF176)
                            } else {
                                Color.White
                            }
                            Box(
                                modifier = Modifier
                                    .width(widths[c].dp * zoom)
                                    .height(cellH)
                                    .background(bg)
                                    .border(0.5.dp, line)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { onCopy(text) }
                                    )
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.Black,
                                    fontSize = (13f * zoom).sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
