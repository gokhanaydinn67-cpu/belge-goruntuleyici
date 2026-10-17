package com.belge.goruntuleyici

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfTopBar(
    title: String,
    searching: Boolean,
    query: String,
    resultText: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onStartSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onShare: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(searching) {
        if (searching) focusRequester.requestFocus()
    }

    if (searching) {
        TopAppBar(
            title = {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("PDF içinde ara") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
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
            },
            actions = {
                if (resultText.isNotEmpty()) {
                    Text(resultText)
                    IconButton(onClick = onPrev) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Önceki")
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Sonraki")
                    }
                }
            }
        )
    } else {
        CenterAlignedTopAppBar(
            title = {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                }
            },
            actions = {
                IconButton(onClick = onStartSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Ara")
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Menü")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Paylaş") },
                        onClick = {
                            menuOpen = false
                            onShare()
                        }
                    )
                }
            }
        )
    }
}
