package com.chatcode.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatcode.data.AuthState
import com.chatcode.data.Graph
import com.chatcode.data.SignUpOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repo = Graph.auth

    val state: StateFlow<AuthState> = repo.state

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch { repo.observe() }
    }

    fun signIn(email: String, password: String) {
        val mail = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            _events.tryEmit("Format email tidak valid.")
            return
        }
        if (password.isEmpty()) {
            _events.tryEmit("Kata sandi wajib diisi.")
            return
        }
        launchBusy {
            repo.signIn(mail, password).onFailure { _events.tryEmit(it.message ?: "Gagal masuk.") }
        }
    }

    fun signUp(email: String, password: String, username: String, displayName: String) {
        val mail = email.trim()
        val uname = username.trim().lowercase()
        val name = displayName.trim()
        when {
            name.isEmpty() -> _events.tryEmit("Nama tampilan wajib diisi.")
            !Regex("^[a-z0-9_]{3,20}$").matches(uname) ->
                _events.tryEmit("Nama pengguna 3 sampai 20 karakter: huruf kecil, angka, atau garis bawah.")
            !Patterns.EMAIL_ADDRESS.matcher(mail).matches() ->
                _events.tryEmit("Format email tidak valid.")
            password.length < 6 -> _events.tryEmit("Kata sandi minimal 6 karakter.")
            else -> launchBusy {
                repo.signUp(mail, password, uname, name)
                    .onSuccess { outcome ->
                        if (outcome == SignUpOutcome.NeedsConfirmation) {
                            _events.tryEmit("Akun dibuat. Cek emailmu untuk konfirmasi, lalu masuk.")
                        }
                    }
                    .onFailure { _events.tryEmit(it.message ?: "Gagal mendaftar.") }
            }
        }
    }

    fun signOut() {
        launchBusy {
            repo.signOut().onFailure { _events.tryEmit(it.message ?: "Gagal keluar.") }
        }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }
}
