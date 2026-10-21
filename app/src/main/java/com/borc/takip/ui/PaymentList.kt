package com.borc.takip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borc.takip.calc.DateUtil
import com.borc.takip.calc.DebtView
import com.borc.takip.calc.interestPart
import com.borc.takip.data.Payment

@Composable
fun PaymentList(v: DebtView, payments: List<Payment>, onUndo: (Payment) -> Unit) {
    var toUndo by remember { mutableStateOf<Payment?>(null) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    if (payments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Henüz ödeme girilmedi.", color = muted)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(payments, key = { it.id }) { p ->
                val inst = v.installments.firstOrNull { it.id == p.installmentId }
                ItemCard(onClick = {}) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = moneyOf(v, p.amount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = DateUtil.format(p.day),
                            fontSize = 13.sp,
                            color = muted
                        )
                        if (inst != null) {
                            val interest = p.interestPart()
                            val extra = if (interest > 0.005) {
                                " · faiz " + Fmt.tl(interest) + " dahil"
                            } else {
                                ""
                            }
                            Text(
                                text = "${inst.number}. taksit" + extra,
                                fontSize = 12.sp,
                                color = muted
                            )
                        }
                    }
                    IconButton(onClick = { toUndo = p }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Geri al", tint = muted)
                    }
                }
            }
        }
    }

    toUndo?.let { p ->
        AlertDialog(
            onDismissRequest = { toUndo = null },
            title = { Text("Ödeme geri alınsın mı?") },
            text = { Text("Bu ödeme silinecek ve taksit eski durumuna dönecek.") },
            confirmButton = {
                TextButton(onClick = {
                    onUndo(p)
                    toUndo = null
                }) { Text("Geri al") }
            },
            dismissButton = {
                TextButton(onClick = { toUndo = null }) { Text("Vazgeç") }
            }
        )
    }
}
