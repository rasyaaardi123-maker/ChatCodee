package com.chatcode.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatcode.data.Profile
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.components.Avatar
import com.chatcode.ui.popup.LocalPopups
import kotlinx.coroutines.launch

@Composable
fun EditProfileSheetContent(
    vm: MainViewModel,
    profile: Profile,
    onDismiss: () -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var bio by remember(profile) { mutableStateOf(profile.bio) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val popups = LocalPopups.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) avatarUri = uri }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Edit Profil", color = Color.White, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier
                .size(92.dp)
                .clip(CircleShape)
                .clickable {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center
        ) {
            if (avatarUri != null) {
                Avatar(name = name, seed = profile.username.hashCode(), size = 92.dp, url = avatarUri.toString())
            } else {
                Avatar(name = profile.name, seed = profile.username.hashCode(), size = 92.dp, url = profile.avatarUrl)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ketuk foto untuk mengganti",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.clickable {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }.padding(4.dp)
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 50) name = it },
            label = { Text("Nama tampilan") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= 200) bio = it },
            label = { Text("Bio") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (busy) return@Button
                if (name.trim().isEmpty()) {
                    popups.showToast("Nama tidak boleh kosong.")
                    return@Button
                }
                busy = true
                scope.launch {
                    var ok = true
                    val avatar = avatarUri
                    if (avatar != null) {
                        ok = vm.updateAvatar(context, avatar)
                    }
                    if (ok) {
                        ok = vm.updateProfile(name.trim(), bio.trim())
                    }
                    busy = false
                    if (ok) onDismiss()
                }
            },
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Menyimpan...")
            } else {
                Text("Simpan")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onDismiss,
            enabled = !busy,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Batal")
        }
        Spacer(Modifier.height(16.dp))
    }
}
