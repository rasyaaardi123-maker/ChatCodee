package com.chatcode.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.chatcode.ui.components.DetailTopBar

@Composable
fun WorkspaceDummyScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        DetailTopBar("Workspace (dummy)", onBack)
    }
}
