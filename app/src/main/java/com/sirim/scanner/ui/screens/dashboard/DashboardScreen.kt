package com.sirim.scanner.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sirim.scanner.data.db.SirimRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    navigateToScanner: () -> Unit,
    navigateToRecords: () -> Unit,
    navigateToExport: () -> Unit
) {
    val recentRecords by viewModel.recentRecords.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("SIRIM Scanner Dashboard") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = navigateToScanner
                ) { Text("Scan Label") }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = navigateToRecords
                ) { Text("Records") }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = navigateToExport
            ) { Text("Export Data") }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recent Records",
                style = MaterialTheme.typography.titleMedium
            )
            recentRecords.forEach { record ->
                RecentRecordCard(record = record)
            }
        }
    }
}

@Composable
private fun RecentRecordCard(record: SirimRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Serial: ${record.sirimSerialNo}")
            Text(text = "Batch: ${record.batchNo}")
            Text(text = "Brand: ${record.brandTrademark}")
        }
    }
}
