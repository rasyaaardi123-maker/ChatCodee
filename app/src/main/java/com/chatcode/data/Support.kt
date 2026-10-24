package com.chatcode.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun friendlyError(e: Throwable): String {
    if (e is IllegalStateException || e is IllegalArgumentException) {
        val m = e.message
        if (!m.isNullOrBlank()) return m
    }
    val msg = (e.message ?: "").lowercase()
    return when {
        "unable to resolve host" in msg || "timeout" in msg ||
            "failed to connect" in msg || "network" in msg ||
            "unreachable" in msg -> "Tidak ada koneksi internet."
        "row-level security" in msg || "permission denied" in msg ||
            "not authorized" in msg -> "Kamu tidak punya izin untuk aksi ini."
        "payload too large" in msg || "exceeded" in msg || "too large" in msg ->
            "Berkas terlalu besar."
        "jwt" in msg || "token" in msg -> "Sesi berakhir. Masuk lagi."
        else -> "Terjadi kesalahan. Coba lagi."
    }
}

internal inline fun <T> safeCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(IllegalStateException(friendlyError(e)))
    }

fun relativeTime(iso: String): String {
    val moment = try {
        OffsetDateTime.parse(iso).toInstant()
    } catch (e: Exception) {
        return ""
    }
    val seconds = Duration.between(moment, Instant.now()).seconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "Baru saja"
        seconds < 3600 -> (seconds / 60).toString() + " menit yang lalu"
        seconds < 86400 -> (seconds / 3600).toString() + " jam yang lalu"
        seconds < 7 * 86400 -> (seconds / 86400).toString() + " hari yang lalu"
        else -> DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID"))
            .withZone(ZoneId.systemDefault())
            .format(moment)
    }
}

object Media {

    suspend fun compressImage(
        context: Context,
        uri: Uri,
        maxSide: Int = 1280,
        quality: Int = 75
    ): ByteArray = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("Gambar tidak dapat dibaca.")
        }

        var sample = 1
        while (max(width, height) / sample > maxSide * 2) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalArgumentException("Gambar tidak dapat dibaca.")

        val rotation = readRotation(resolver, uri)
        val scale = min(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
        val matrix = Matrix()
        if (scale < 1f) matrix.postScale(scale, scale)
        if (rotation != 0) matrix.postRotate(rotation.toFloat())

        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val out = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (result !== decoded) decoded.recycle()
        result.recycle()
        out.toByteArray()
    }

    private fun readRotation(resolver: ContentResolver, uri: Uri): Int =
        try {
            resolver.openInputStream(uri)?.use { stream ->
                val orientation = ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
}
