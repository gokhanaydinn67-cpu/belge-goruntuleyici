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

    private val textTypes = setOf(
        "txt", "log", "csv", "tsv", "md", "xml", "json", "html"
    )

    private val legacyPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = scheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val current = opened.value
                    if (hasAccess.value && current != null) {
                        BackHandler { opened.value = null }
                        if (current.ext == "pdf") {
                            PdfViewerScreen(current) { opened.value = null }
                        } else {
                            TextViewerScreen(current) { opened.value = null }
                        }
                    } else if (hasAccess.value) {
                        FileListScreen(files.value) { f -> openFile(f) }
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

    private fun openFile(f: DocFile) {
        if (f.ext == "pdf" || f.ext in textTypes) {
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
