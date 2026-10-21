package com.borc.takip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.borc.takip.calc.DateUtil
import java.util.Locale

private fun parseAmount(text: String): Double? {
    val t = text.trim()
    val norm = if (t.contains(',')) t.replace(".", "").replace(',', '.') else t
    return norm.toDoubleOrNull()
}

@Composable
fun PayDialog(
    suggested: Double,
    unit: String,
    onSubmit: (Double, Long, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember {
        mutableStateOf(
            if (suggested > 0.0) String.format(Locale("tr", "TR"), "%.2f", suggested) else ""
        )
    }
    var day by remember { mutableLongStateOf(DateUtil.today()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ödeme gir") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(
                    label = "Ödenen tutar ($unit)",
                    value = amount,
                    onChange = { amount = it },
                    keyboard = KeyboardType.Decimal
                )
                DateField("Ödeme tarihi", day) { day = it }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    val v = parseAmount(amount)
                    if (v == null || v <= 0.0) {
                        error = "Geçerli bir tutar yaz"
                    } else {
                        busy = true
                        onSubmit(v, day) { err ->
                            busy = false
                            if (err == null) onDismiss() else error = err
                        }
                    }
                }
            ) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}
