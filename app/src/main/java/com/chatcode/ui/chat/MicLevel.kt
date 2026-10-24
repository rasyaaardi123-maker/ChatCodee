package com.chatcode.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class MicMeter {
    @Volatile
    private var peak = 0f

    fun report(level: Float) {
        if (level > peak) peak = level
    }

    fun takePeak(): Float {
        val value = peak
        peak = 0f
        return value
    }
}

private fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun rememberMicMeter(active: Boolean): MicMeter {
    val context = LocalContext.current
    val meter = remember { MicMeter() }
    var granted by remember { mutableStateOf(hasMicPermission(context)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result -> granted = result }

    LaunchedEffect(active, granted) {
        if (!active) return@LaunchedEffect
        if (!granted) {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) { readMicLevels(meter) }
    }

    return meter
}

@SuppressLint("MissingPermission")
private suspend fun readMicLevels(meter: MicMeter) {
    val sampleRate = 16000
    val minBuffer = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    if (minBuffer <= 0) return

    val recorder = try {
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 2
        )
    } catch (e: Exception) {
        return
    }

    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
        recorder.release()
        return
    }

    val buffer = ShortArray(sampleRate / 20)
    try {
        recorder.startRecording()
        while (currentCoroutineContext().isActive) {
            val read = recorder.read(buffer, 0, buffer.size)
            if (read > 0) {
                var sum = 0.0
                for (i in 0 until read) {
                    val s = buffer[i].toDouble()
                    sum += s * s
                }
                val rms = sqrt(sum / read) / 32768.0
                val db = 20.0 * log10(max(rms, 0.00001))
                meter.report(((db + 52.0) / 42.0).coerceIn(0.0, 1.0).toFloat())
            } else if (read < 0) {
                break
            } else {
                delay(10)
            }
        }
    } catch (e: Exception) {
        meter.report(0f)
    } finally {
        try {
            recorder.stop()
        } catch (e: IllegalStateException) {
            meter.report(0f)
        }
        recorder.release()
    }
}
