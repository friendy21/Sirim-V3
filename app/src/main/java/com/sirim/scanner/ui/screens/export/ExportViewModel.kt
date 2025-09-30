package com.sirim.scanner.ui.screens.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirim.scanner.data.db.SirimRecord
import com.sirim.scanner.data.export.ExportManager
import com.sirim.scanner.data.repository.SirimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExportViewModel private constructor(
    private val repository: SirimRepository,
    private val exportManager: ExportManager
) : ViewModel() {

    val records: StateFlow<List<SirimRecord>> = repository.records
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _lastExportUri = MutableStateFlow<Uri?>(null)
    val lastExportUri: StateFlow<Uri?> = _lastExportUri.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _lastFormat = MutableStateFlow<ExportFormat?>(null)
    val lastFormat: StateFlow<ExportFormat?> = _lastFormat.asStateFlow()

    fun export(format: ExportFormat) {
        if (_isExporting.value) return
        _isExporting.value = true
        viewModelScope.launch {
            val currentRecords = records.value
            val uri = when (format) {
                ExportFormat.Pdf -> exportManager.exportToPdf(currentRecords)
                ExportFormat.Csv -> exportManager.exportToCsv(currentRecords)
                ExportFormat.Excel -> exportManager.exportToExcel(currentRecords)
            }
            _lastExportUri.value = uri
            _lastFormat.value = format
            _isExporting.value = false
        }
    }

    companion object {
        fun Factory(
            repository: SirimRepository,
            exportManager: ExportManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExportViewModel(repository, exportManager) as T
            }
        }
    }
}

enum class ExportFormat { Pdf, Excel, Csv }
