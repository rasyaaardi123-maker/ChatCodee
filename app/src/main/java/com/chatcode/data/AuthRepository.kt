package com.chatcode.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String,
    val username: String
)

sealed interface AuthState {
    object Loading : AuthState
    object SignedOut : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

enum class SignUpOutcome { SignedIn, NeedsConfirmation }

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun observe()
    suspend fun signIn(emailInput: String, passwordInput: String): Result<Unit>
    suspend fun signUp(
        emailInput: String,
        passwordInput: String,
        usernameInput: String,
        displayNameInput: String
    ): Result<SignUpOutcome>
    suspend fun signOut(): Result<Unit>
}

class SupabaseAuthRepository(private val client: SupabaseClient) : AuthRepository {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    override suspend fun observe() {
        try {
            client.auth.awaitInitialization()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Lanjut: status akan dihitung dari sesi yang tersedia.
        }
        publish()
        client.auth.sessionStatus.collect { publish() }
    }

    override suspend fun signIn(emailInput: String, passwordInput: String): Result<Unit> =
        try {
            client.auth.signInWith(Email) {
                email = emailInput
                password = passwordInput
            }
            publish()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(friendlyError(e)))
        }

    override suspend fun signUp(
        emailInput: String,
        passwordInput: String,
        usernameInput: String,
        displayNameInput: String
    ): Result<SignUpOutcome> =
        try {
            client.auth.signUpWith(Email) {
                email = emailInput
                password = passwordInput
                data = buildJsonObject {
                    put("username", usernameInput)
                    put("display_name", displayNameInput)
                }
            }
            publish()
            val outcome = if (client.auth.currentSessionOrNull() != null) {
                SignUpOutcome.SignedIn
            } else {
                SignUpOutcome.NeedsConfirmation
            }
            Result.success(outcome)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(friendlyError(e)))
        }

    override suspend fun signOut(): Result<Unit> =
        try {
            client.auth.signOut()
            publish()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(friendlyError(e)))
        }

    private fun publish() {
        val user = client.auth.currentUserOrNull()
        _state.value = if (user == null) {
            AuthState.SignedOut
        } else {
            AuthState.SignedIn(user.toAuthUser())
        }
    }

    private fun UserInfo.toAuthUser(): AuthUser {
        val meta = userMetadata
        val display = meta?.get("display_name")?.jsonPrimitive?.contentOrNull
        val uname = meta?.get("username")?.jsonPrimitive?.contentOrNull
        val mail = email ?: ""
        val fallback = mail.substringBefore('@')
        return AuthUser(
            id = id,
            email = mail,
            displayName = display?.takeIf { it.isNotBlank() } ?: fallback,
            username = uname?.takeIf { it.isNotBlank() } ?: fallback
        )
    }

    private fun friendlyError(e: Throwable): String {
        val msg = (e.message ?: "").lowercase()
        return when {
            "invalid login credentials" in msg -> "Email atau kata sandi salah."
            "already registered" in msg || "already been registered" in msg ->
                "Email ini sudah terdaftar."
            "email not confirmed" in msg ->
                "Email belum dikonfirmasi. Cek kotak masuk emailmu."
            "password should be at least" in msg || "weak" in msg ->
                "Kata sandi terlalu lemah."
            "rate limit" in msg || "too many" in msg ->
                "Terlalu banyak percobaan. Coba lagi nanti."
            "unable to resolve host" in msg || "timeout" in msg ||
                "failed to connect" in msg || "network" in msg ->
                "Tidak ada koneksi internet."
            "database error" in msg -> "Nama pengguna tidak valid atau sudah dipakai."
            else -> "Terjadi kesalahan. Coba lagi."
        }
    }
}
