package com.belge.goruntuleyici

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FastScroller(listState: LazyListState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val total = listState.layoutInfo.totalItemsCount
    val scrolling = listState.isScrollInProgress

    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) {
            visible = true
        } else {
            delay(1500)
            visible = false
        }
    }

    val a by animateFloatAsState(if (visible) 1f else 0f, label = "scrollbar")

    BoxWithConstraints(modifier = modifier.fillMaxHeight().width(36.dp)) {
        val trackPx = constraints.maxHeight.toFloat()
        val thumbPx = with(LocalDensity.current) { 56.dp.toPx() }
        val range = (trackPx - thumbPx).coerceAtLeast(1f)

        val shown = if (dragging) {
            dragFraction
        } else if (!listState.canScrollForward) {
            1f
        } else if (total > 1) {
            (listState.firstVisibleItemIndex.toFloat() / (total - 1)).coerceIn(0f, 1f)
        } else {
            0f
        }

        fun jump(y: Float) {
            dragFraction = ((y - thumbPx / 2f) / range).coerceIn(0f, 1f)
            if (total > 1) {
                val index = (dragFraction * (total - 1)).roundToInt()
                scope.launch { listState.scrollToItem(index) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(total, trackPx) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            jump(offset.y)
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            jump(change.position.y)
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, (shown * range).roundToInt()) }
                    .padding(end = 4.dp)
                    .width(if (dragging) 12.dp else 8.dp)
                    .height(56.dp)
                    .graphicsLayer { alpha = a }
                    .background(Color(0xFF757575), RoundedCornerShape(6.dp))
            )
        }
    }
}
