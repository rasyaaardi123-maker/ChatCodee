package com.chatcode.ui.chat

import androidx.lifecycle.ViewModel
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val STT_DUMMY = "ini hasil suara menjadi teks"

enum class InputMode { Idle, PlusMenu, RecordingVoice, RecordingStt }

data class ChatInputState(
    val mode: InputMode = InputMode.Idle,
    val previousMode: InputMode = InputMode.Idle,
    val text: String = "",
    val paused: Boolean = false
)

class ChatInputViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChatInputState())
    val state: StateFlow<ChatInputState> = _state.asStateFlow()

    private var voiceStartMs = 0L
    private var pausedAtMs = 0L
    private var pausedTotalMs = 0L

    fun onTextChange(value: String) {
        _state.update { it.copy(text = value) }
    }

    fun openPlusMenu() {
        _state.update {
            if (it.mode == InputMode.Idle) it.copy(mode = InputMode.PlusMenu, previousMode = it.mode) else it
        }
    }

    fun cancelMode(): Boolean {
        if (_state.value.mode == InputMode.Idle) return false
        _state.update {
            it.copy(mode = it.previousMode, previousMode = InputMode.Idle, paused = false)
        }
        return true
    }

    fun startVoice() {
        val cur = _state.value
        if (cur.mode != InputMode.Idle || cur.text.isNotEmpty()) return
        voiceStartMs = System.currentTimeMillis()
        pausedAtMs = 0L
        pausedTotalMs = 0L
        _state.value = cur.copy(mode = InputMode.RecordingVoice, previousMode = cur.mode, paused = false)
    }

    fun togglePause() {
        val cur = _state.value
        if (cur.mode != InputMode.RecordingVoice) return
        val now = System.currentTimeMillis()
        if (cur.paused) pausedTotalMs += now - pausedAtMs else pausedAtMs = now
        _state.value = cur.copy(paused = !cur.paused)
    }

    fun finishVoice(): Int {
        val cur = _state.value
        if (cur.mode != InputMode.RecordingVoice) return 0
        val now = System.currentTimeMillis()
        val ongoingPause = if (cur.paused) now - pausedAtMs else 0L
        val elapsedMs = now - voiceStartMs - pausedTotalMs - ongoingPause
        _state.value = cur.copy(mode = InputMode.Idle, previousMode = InputMode.Idle, paused = false)
        return max(1, (elapsedMs / 1000L).toInt())
    }

    fun startStt() {
        _state.update {
            if (it.mode == InputMode.Idle) it.copy(mode = InputMode.RecordingStt, previousMode = it.mode) else it
        }
    }

    fun stopStt() {
        _state.update {
            if (it.mode != InputMode.RecordingStt) it
            else it.copy(text = joinDummy(it.text), mode = InputMode.Idle, previousMode = InputMode.Idle)
        }
    }

    fun finishSttAndTakeText(): String? {
        val cur = _state.value
        if (cur.mode != InputMode.RecordingStt) return null
        val full = joinDummy(cur.text)
        _state.value = cur.copy(text = "", mode = InputMode.Idle, previousMode = InputMode.Idle)
        return full
    }

    fun takeText(): String? {
        val cur = _state.value
        val trimmed = cur.text.trim()
        if (cur.mode != InputMode.Idle || trimmed.isEmpty()) return null
        _state.value = cur.copy(text = "")
        return trimmed
    }

    private fun joinDummy(text: String): String =
        if (text.isBlank()) STT_DUMMY else text.trimEnd() + " " + STT_DUMMY
}
