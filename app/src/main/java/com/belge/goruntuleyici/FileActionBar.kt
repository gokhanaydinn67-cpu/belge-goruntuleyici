package com.belge.goruntuleyici

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun BarItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label)
        Text(text = label, fontSize = 12.sp)
    }
}

@Composable
fun FileActionBar(
    single: Boolean,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onOpenOther: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BarItem(Icons.Outlined.Share, "Gönder", onShare)
            BarItem(Icons.Outlined.DriveFileMove, "Taşı", onMove)
            BarItem(Icons.Outlined.Delete, "Sil", onDelete)
            Box {
                BarItem(Icons.Outlined.MoreHoriz, "Daha fazla") { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Başka uygulamada aç") },
                        enabled = single,
                        onClick = {
                            menu = false
                            onOpenOther()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Yeniden adlandır") },
                        enabled = single,
                        onClick = {
                            menu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ayrıntılar") },
                        onClick = {
                            menu = false
                            onDetails()
                        }
                    )
                }
            }
        }
    }
}
