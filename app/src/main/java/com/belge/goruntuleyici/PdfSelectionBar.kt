package com.belge.goruntuleyici

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PdfSelectionBar(
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF303030),
        shadowElevation = 6.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCopy) {
                Text("Kopyala", color = Color.White)
            }
            TextButton(onClick = onSelectAll) {
                Text("Sayfayı seç", color = Color.White)
            }
            TextButton(onClick = onClose) {
                Text("Kapat", color = Color.White)
            }
        }
    }
}
