package com.belge.goruntuleyici

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val files = mutableStateOf<List<DocFile>>(emptyList())
    private val hasAccess = mutableStateOf(false)
    private val opened = mutableStateOf<DocFile?>(null)
    private val listQuery = mutableStateOf("")
    private val listSearching = mutableStateOf(false)
    private val externalOpen = mutableStateOf(false)
    private val loading = mutableStateOf(false)

    private val textTypes = setOf(
        "txt", "log", "csv", "tsv", "md", "xml", "json", "html"
    )

    private val docTypes = setOf("docx", "docm", "dotx", "odt", "rtf")

    private val sheetTypes = setOf("xlsx", "xlsm", "xltx", "ods")

    private val deckTypes = setOf("pptx", "pptm", "ppsx", "odp")

    private val tiffTypes = setOf("tif", "tiff")

    private val legacyPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val scheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            val listState = rememberLazyListState()
            MaterialTheme(colorScheme = scheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val current = opened.value
                    if (current != null) {
                        BackHandler { closeViewer() }
                        if (current.ext == "pdf") {
                            PdfViewerScreen(current) { closeViewer() }
                        } else if (current.ext in docTypes) {
                            DocViewerScreen(current) { closeViewer() }
                        } else if (current.ext in sheetTypes) {
                            SheetViewerScreen(current) { closeViewer() }
                        } else if (current.ext in deckTypes) {
                            PptViewerScreen(current) { closeViewer() }
                        } else if (current.ext in tiffTypes) {
                            TiffViewerScreen(current) { closeViewer() }
                        } else {
                            TextViewerScreen(current) { closeViewer() }
                        }
                    } else if (loading.value) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Açılıyor...")
                        }
                    } else if (hasAccess.value) {
                        FileListScreen(
                            files = files.value,
                            listState = listState,
                            query = listQuery.value,
                            searching = listSearching.value,
                            onQueryChange = { listQuery.value = it },
                            onSearchingChange = { listSearching.value = it },
                            onOpen = { f -> openFile(f) }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Belgeleri listelemek için dosya erişim izni gerekli",
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { askAccess() }) {
                                    Text("İzin ver")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun closeViewer() {
        if (externalOpen.value) {
            finish()
        } else {
            opened.value = null
        }
    }

    private fun isSupported(f: DocFile): Boolean {
        return f.ext == "pdf" || f.ext in textTypes || f.ext in docTypes ||
            f.ext in sheetTypes || f.ext in deckTypes || f.ext in tiffTypes
    }

    private fun handleIntent(i: Intent?) {
        if (i == null || i.action != Intent.ACTION_VIEW) return
        loading.value = true
        Thread {
            val f = ExternalOpen.fromIntent(this, i)
            runOnUiThread {
                loading.value = false
                if (f == null) {
                    Toast.makeText(this, "Dosya açılamadı", Toast.LENGTH_SHORT).show()
                    if (opened.value == null) finish()
                } else if (isSupported(f)) {
                    externalOpen.value = true
                    opened.value = f
                } else {
                    Toast.makeText(this, "Bu tür henüz desteklenmiyor", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }.start()
    }

    private fun openFile(f: DocFile) {
        if (isSupported(f)) {
            opened.value = f
        } else {
            Toast.makeText(this, "Bu tür henüz desteklenmiyor", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun checkAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun refresh() {
        val ok = checkAccess()
        hasAccess.value = ok
        if (ok) {
            Thread {
                val list = FileScanner.scan()
                runOnUiThread { files.value = list }
            }.start()
        }
    }

    private fun askAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            val i = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(i)
        } else {
            legacyPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
