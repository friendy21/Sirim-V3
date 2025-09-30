package com.sirim.scanner.ui.screens.scanner

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirim.scanner.data.db.SirimRecord
import com.sirim.scanner.data.ocr.FieldConfidence
import com.sirim.scanner.data.ocr.LabelAnalyzer
import com.sirim.scanner.data.ocr.OcrResult
import com.sirim.scanner.data.ocr.toJpegByteArray
import com.sirim.scanner.data.repository.SirimRepository
import com.sirim.scanner.data.validation.FieldValidator
import com.sirim.scanner.data.validation.ValidationResult
import java.util.concurrent.atomic.AtomicBoolean
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

    private val processing = AtomicBoolean(false)

    private val _extractedFields = MutableStateFlow<Map<String, FieldConfidence>>(emptyMap())
    val extractedFields: StateFlow<Map<String, FieldConfidence>> = _extractedFields.asStateFlow()

    private val _validationWarnings = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationWarnings: StateFlow<Map<String, String>> = _validationWarnings.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    private val _status = MutableStateFlow(ScanStatus())
    val status: StateFlow<ScanStatus> = _status.asStateFlow()

    private val _lastResultId = MutableStateFlow<Long?>(null)
    val lastResultId: StateFlow<Long?> = _lastResultId.asStateFlow()

    fun analyzeImage(imageProxy: ImageProxy) {
        if (!processing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        _status.value = _status.value.copy(state = ScanState.Scanning, message = "Analyzing frame...", confidence = 0f)
        viewModelScope.launch {
            try {
                when (val result = analyzer.analyze(imageProxy)) {
                    is OcrResult.Success -> handleResult(result, autoPersist = true)
                    is OcrResult.Partial -> handleResult(result, autoPersist = false)
                    OcrResult.Empty -> {
                        _status.value = ScanStatus(state = ScanState.Idle, message = "Align the label within the guide")
                        _extractedFields.value = emptyMap()
                        _validationWarnings.value = emptyMap()
                        _validationErrors.value = emptyMap()
                    }
                }
            } catch (ex: Exception) {
                _status.value = ScanStatus(state = ScanState.Error, message = "Scan failed: ${ex.message ?: "Unknown error"}")
            } finally {
                imageProxy.close()
                processing.set(false)
            }
        }
    }

    private suspend fun handleResult(result: OcrResult, autoPersist: Boolean) {
        val fields = when (result) {
            is OcrResult.Success -> result.fields
            is OcrResult.Partial -> result.fields
            else -> emptyMap()
        }
        if (fields.isEmpty()) {
            _status.value = ScanStatus(state = ScanState.Scanning, message = "Still searching for readable text")
            return
        }

        val validation = FieldValidator.validate(fields)
        _extractedFields.value = validation.sanitized
        _validationWarnings.value = validation.warnings
        _validationErrors.value = validation.errors

        val baseStatus = when (result) {
            is OcrResult.Success -> ScanState.Ready
            is OcrResult.Partial -> ScanState.Partial
            else -> ScanState.Scanning
        }

        val message = when {
            validation.errors.isNotEmpty() -> "Review highlighted fields"
            result is OcrResult.Partial -> "Hold steady for clearer capture"
            else -> "Data captured"
        }

        _status.value = ScanStatus(
            state = baseStatus,
            message = message,
            confidence = when (result) {
                is OcrResult.Success -> result.confidence
                is OcrResult.Partial -> result.confidence
                else -> 0f
            },
            frames = when (result) {
                is OcrResult.Success -> result.frames
                is OcrResult.Partial -> result.frames
                else -> 0
            }
        )

        if (autoPersist && validation.errors.isEmpty()) {
            persistIfUnique(validation, result as OcrResult.Success)
        }
    }

    private fun persistIfUnique(validation: ValidationResult, result: OcrResult.Success) {
        val serial = validation.sanitized["sirimSerialNo"]?.value
        if (serial.isNullOrBlank()) {
            _status.value = _status.value.copy(state = ScanState.Error, message = "Serial number missing; manual review required")
            return
        }
        appScope.launch {
            val duplicate = repository.findBySerial(serial)
            if (duplicate != null) {
                _status.value = _status.value.copy(state = ScanState.Duplicate, message = "Duplicate serial detected: $serial")
                return@launch
            }
            val imagePath = result.bitmap?.toJpegByteArray()?.let { bytes ->
                repository.persistImage(bytes)
            }
            val sanitizedValues = validation.sanitized
            val record = SirimRecord(
                sirimSerialNo = sanitizedValues["sirimSerialNo"]?.value.orEmpty(),
                batchNo = sanitizedValues["batchNo"]?.value.orEmpty(),
                brandTrademark = sanitizedValues["brandTrademark"]?.value.orEmpty(),
                model = sanitizedValues["model"]?.value.orEmpty(),
                type = sanitizedValues["type"]?.value.orEmpty(),
                rating = sanitizedValues["rating"]?.value.orEmpty(),
                size = sanitizedValues["size"]?.value.orEmpty(),
                imagePath = imagePath,
                isVerified = false
            )
            val id = repository.upsert(record)
            _lastResultId.value = id
            _status.value = _status.value.copy(state = ScanState.Persisted, message = "Record saved", confidence = result.confidence)
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
data class ScanStatus(
    val state: ScanState = ScanState.Idle,
    val message: String = "",
    val confidence: Float = 0f,
    val frames: Int = 0
)

enum class ScanState {
    Idle,
    Scanning,
    Partial,
    Ready,
    Persisted,
    Duplicate,
    Error
}
