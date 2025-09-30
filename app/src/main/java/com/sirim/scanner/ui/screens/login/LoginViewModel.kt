package com.sirim.scanner.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel private constructor() : ViewModel() {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun onUsernameChanged(value: String) {
        _username.value = value
        _error.value = null
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
        _error.value = null
    }

    fun authenticate(onSuccess: () -> Unit) {
        if (_username.value == "admin" && _password.value == "admin") {
            _error.value = null
            onSuccess()
        } else {
            _error.value = "Invalid credentials"
        }
    }

    companion object {
        fun Factory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoginViewModel() as T
                }
            }
    }
}
