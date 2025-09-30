package com.sirim.scanner.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirim.scanner.data.db.SirimRecord
import com.sirim.scanner.data.repository.SirimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel private constructor(
    private val repository: SirimRepository
) : ViewModel() {

    private val _recentRecords = MutableStateFlow<List<SirimRecord>>(emptyList())
    val recentRecords: StateFlow<List<SirimRecord>> = _recentRecords.asStateFlow()

    init {
        viewModelScope.launch {
            repository.records.collect { records ->
                _recentRecords.value = records.take(5)
            }
        }
    }

    companion object {
        fun Factory(repository: SirimRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(repository) as T
                }
            }
    }
}
