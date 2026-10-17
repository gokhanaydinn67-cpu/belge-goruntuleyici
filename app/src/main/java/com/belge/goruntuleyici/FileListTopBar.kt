package com.belge.goruntuleyici

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListTopBar(
    searching: Boolean,
    query: String,
    selectedCount: Int,
    onQueryChange: (String) -> Unit,
    onStartSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onClearSelection: () -> Unit,
    onShare: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searching) {
        if (searching) focusRequester.requestFocus()
    }

    if (selectedCount > 0) {
        TopAppBar(
            title = { Text("$selectedCount seçildi") },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Filled.Close, contentDescription = "Seçimi kapat")
                }
            },
            actions = {
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "Paylaş")
                }
            }
        )
    } else if (searching) {
        TopAppBar(
            title = {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Ad veya .pdf gibi uzantı") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            navigationIcon = {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Filled.Close, contentDescription = "Aramayı kapat")
                }
            }
        )
    } else {
        TopAppBar(
            title = { Text("Belge Görüntüleyici") },
            actions = {
                IconButton(onClick = onStartSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Ara")
                }
            }
        )
    }
}
