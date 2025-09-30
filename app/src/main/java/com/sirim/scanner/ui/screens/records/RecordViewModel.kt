package com.sirim.scanner.ui.screens.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirim.scanner.data.db.SirimRecord
import com.sirim.scanner.data.repository.SirimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordViewModel private constructor(
    private val repository: SirimRepository
) : ViewModel() {

    val records: StateFlow<List<SirimRecord>> = repository.records
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeRecord = MutableStateFlow<SirimRecord?>(null)
    val activeRecord: StateFlow<SirimRecord?> = _activeRecord.asStateFlow()

    fun loadRecord(id: Long) {
        viewModelScope.launch {
            _activeRecord.value = repository.getRecord(id)
        }
    }

    fun resetActiveRecord() {
        _activeRecord.value = null
    }

    fun createOrUpdate(record: SirimRecord, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.upsert(record)
            _activeRecord.value = repository.getRecord(id)
            onSaved(id)
        }
    }

    fun delete(record: SirimRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    companion object {
        fun Factory(repository: SirimRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RecordViewModel(repository) as T
                }
            }
    }
}
