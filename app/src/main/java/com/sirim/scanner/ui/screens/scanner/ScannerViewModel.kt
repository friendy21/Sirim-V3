package com.sirim.scanner.ui.screens.scanner

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirim.scanner.data.db.SirimRecord
import com.sirim.scanner.data.ocr.LabelAnalyzer
import com.sirim.scanner.data.ocr.OcrResult
import com.sirim.scanner.data.repository.SirimRepository
import com.sirim.scanner.data.ocr.toJpegByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScannerViewModel private constructor(
    private val repository: SirimRepository,
    private val analyzer: LabelAnalyzer,
    private val appScope: CoroutineScope
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _recognizedFields = MutableStateFlow<Map<String, String>>(emptyMap())
    val recognizedFields: StateFlow<Map<String, String>> = _recognizedFields.asStateFlow()

    private val _lastResultId = MutableStateFlow<Long?>(null)
    val lastResultId: StateFlow<Long?> = _lastResultId.asStateFlow()

    fun analyzeImage(imageProxy: ImageProxy) {
        if (_isProcessing.value) {
            imageProxy.close()
            return
        }
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val result = analyzer.analyze(imageProxy)
                when (result) {
                    is OcrResult.Success -> {
                        _recognizedFields.value = result.fields
                        persistResult(result)
                    }

                    OcrResult.Empty -> {
                        _recognizedFields.value = emptyMap()
                        _isProcessing.value = false
                    }
                }
            } catch (ex: Exception) {
                _recognizedFields.value = emptyMap()
                _isProcessing.value = false
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun persistResult(result: OcrResult.Success) {
        appScope.launch {
            val imagePath = result.bitmap?.toJpegByteArray()?.let { bytes ->
                repository.persistImage(bytes)
            }
            val record = SirimRecord(
                sirimSerialNo = result.fields["sirimSerialNo"].orEmpty(),
                batchNo = result.fields["batchNo"].orEmpty(),
                brandTrademark = result.fields["brandTrademark"].orEmpty(),
                model = result.fields["model"].orEmpty(),
                type = result.fields["type"].orEmpty(),
                rating = result.fields["rating"].orEmpty(),
                size = result.fields["size"].orEmpty(),
                imagePath = imagePath
            )
            val id = repository.upsert(record)
            _lastResultId.value = id
            _isProcessing.value = false
        }
    }

    companion object {
        fun Factory(
            repository: SirimRepository,
            analyzer: LabelAnalyzer,
            appScope: CoroutineScope
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScannerViewModel(repository, analyzer, appScope) as T
            }
        }
    }
}
