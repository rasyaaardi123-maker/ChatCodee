package com.chatcode.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatcode.ui.components.edgeFade
import com.chatcode.ui.popup.LocalPopups
import com.chatcode.ui.theme.TextSecondary

private val FieldColor = Color(0xFF2B2B2B).copy(alpha = 0.85f)
private val ButtonColor = Color(0xFF1E6FE8).copy(alpha = 0.90f)

@Composable
fun AuthScreen(vm: AuthViewModel) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val popups = LocalPopups.current

    var signUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(vm) {
        vm.events.collect { popups.showToast(it) }
    }

    val submit = {
        if (signUpMode) vm.signUp(email, password, username, displayName)
        else vm.signIn(email, password)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .edgeFade(top = 40.dp, bottom = 40.dp)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ChatCode",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (signUpMode) "Buat akun untuk mulai berbagi karya" else "Masuk untuk melanjutkan",
                color = TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            if (signUpMode) {
                FloatingField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    placeholder = "Nama tampilan",
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    onDone = {}
                )
                Spacer(Modifier.height(12.dp))
                FloatingField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "Nama pengguna",
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    onDone = {}
                )
                Spacer(Modifier.height(12.dp))
            }
            FloatingField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onDone = {}
            )
            Spacer(Modifier.height(12.dp))
            FloatingField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Kata sandi",
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onDone = { submit() },
                isPassword = true
            )
            Spacer(Modifier.height(24.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(ButtonColor)
                    .clickable(enabled = !busy) { submit() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        busy -> "Memproses..."
                        signUpMode -> "Daftar"
                        else -> "Masuk"
                    },
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = if (signUpMode) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = !busy) { signUpMode = !signUpMode }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun FloatingField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onDone: () -> Unit,
    isPassword: Boolean = false
) {
    val focus = LocalFocusManager.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
        cursorBrush = SolidColor(Color.White),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { focus.moveFocus(FocusDirection.Down) },
            onDone = { onDone() }
        ),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(FieldColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = TextSecondary, fontSize = 17.sp)
                }
                inner()
            }
        }
    )
}
