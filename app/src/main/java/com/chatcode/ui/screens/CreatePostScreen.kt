package com.chatcode.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.components.DetailTopBar
import com.chatcode.ui.components.PostImage
import com.chatcode.ui.components.edgeFade
import com.chatcode.ui.popup.LocalPopups
import kotlinx.coroutines.launch

@Composable
fun CreatePostScreen(vm: MainViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val popups = LocalPopups.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { result -> picked = result }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding()
    ) {
        DetailTopBar("Buat Postingan", onBack)
        Column(
            Modifier
                .weight(1f)
                .edgeFade(top = 16.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 2000) text = it },
                label = { Text("Apa yang sedang kamu kerjakan?") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !busy,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (picked == null) "Pilih Gambar" else "Ganti Gambar")
            }
            val current = picked
            if (current != null) {
                Spacer(Modifier.height(16.dp))
                PostImage(
                    uri = current.toString(),
                    seed = -1,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(0.6f)
                )
            }
        }
        Button(
            onClick = {
                if (busy) return@Button
                val body = text.trim()
                if (body.isEmpty() && picked == null) {
                    popups.showToast("Tulis sesuatu atau pilih gambar.")
                    return@Button
                }
                busy = true
                scope.launch {
                    val ok = vm.createPost(context, body, picked)
                    busy = false
                    if (ok) {
                        vm.setQuery("")
                        onDone()
                    }
                }
            },
            enabled = !busy && (text.isNotBlank() || picked != null),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Mengunggah...")
            } else {
                Text("Posting")
            }
        }
    }
}
