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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.components.DetailTopBar
import com.chatcode.ui.components.PostImage
import com.chatcode.ui.popup.LocalPopups
import kotlinx.coroutines.launch

@Composable
fun CreateStoryScreen(vm: MainViewModel, onDone: () -> Unit, onBack: () -> Unit) {
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
    ) {
        DetailTopBar("Buat Story", onBack)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        .weight(1f, fill = false)
                        .fillMaxWidth(0.6f)
                        .aspectRatio(0.56f)
                )
            } else {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Story akan hilang setelah 24 jam.",
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        Button(
            onClick = {
                val uri = picked
                if (uri == null) {
                    popups.showToast("Pilih gambar terlebih dahulu.")
                    return@Button
                }
                if (busy) return@Button
                busy = true
                scope.launch {
                    val ok = vm.createStory(context, uri)
                    busy = false
                    if (ok) onDone()
                }
            },
            enabled = !busy && picked != null,
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
                Text("Bagikan")
            }
        }
    }
}
