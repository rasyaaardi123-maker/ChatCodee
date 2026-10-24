package com.chatcode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatcode.data.AuthState
import com.chatcode.data.AuthUser
import com.chatcode.data.Backend
import com.chatcode.ui.auth.AuthScreen
import com.chatcode.ui.auth.AuthViewModel
import com.chatcode.ui.components.EmptyState
import com.chatcode.ui.popup.LocalPopups
import com.chatcode.ui.popup.PopupController
import com.chatcode.ui.popup.PopupHost

@Composable
fun AppRoot() {
    val popups = remember { PopupController() }

    CompositionLocalProvider(LocalPopups provides popups) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (Backend.isConfigured) {
                AuthGate()
            } else {
                ConfigMissing()
            }
            PopupHost(popups)
        }
    }
}

@Composable
private fun AuthGate() {
    val authVm: AuthViewModel = viewModel()
    val auth by authVm.state.collectAsStateWithLifecycle()

    when (val current = auth) {
        AuthState.Loading -> Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        AuthState.SignedOut -> AuthScreen(authVm)
        is AuthState.SignedIn -> SignedInRoot(current.user, authVm)
    }
}

@Composable
private fun SignedInRoot(user: AuthUser, authVm: AuthViewModel) {
    val vm: MainViewModel = viewModel(key = user.id)
    val popups = LocalPopups.current

    LaunchedEffect(vm) {
        vm.messages.collect { popups.showToast(it) }
    }

    AppNav(vm = vm, onLogout = { authVm.signOut() })
}

@Composable
private fun ConfigMissing() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            icon = Icons.Outlined.Settings,
            title = "Konfigurasi belum lengkap",
            message = "Isi supabase.url dan supabase.anonKey di local.properties, lalu build ulang aplikasi."
        )
    }
}
