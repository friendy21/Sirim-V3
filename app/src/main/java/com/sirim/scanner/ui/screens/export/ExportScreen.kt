package com.sirim.scanner.ui.screens.export

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isExporting by viewModel.isExporting.collectAsState()
    val lastExportUri by viewModel.lastExportUri.collectAsState()
    val lastFormat by viewModel.lastFormat.collectAsState()
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(lastExportUri) {
        lastExportUri?.let { uri ->
            context.grantUriPermission(
                context.packageName,
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Records") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.export(ExportFormat.Pdf) },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export as PDF") }
            Button(
                onClick = { viewModel.export(ExportFormat.Excel) },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export as Excel") }
            Button(
                onClick = { viewModel.export(ExportFormat.Csv) },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export as CSV") }

            if (lastExportUri != null) {
                Text("Last export ready at: ${lastExportUri}")
                Button(
                    onClick = {
                        val format = lastFormat ?: ExportFormat.Pdf
                        val mime = when (format) {
                            ExportFormat.Pdf -> "application/pdf"
                            ExportFormat.Excel -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            ExportFormat.Csv -> "text/csv"
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = mime
                            putExtra(Intent.EXTRA_STREAM, lastExportUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(intent, "Share export")
                        shareLauncher.launch(chooser)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share Export")
                }
            }
        }
    }
}
