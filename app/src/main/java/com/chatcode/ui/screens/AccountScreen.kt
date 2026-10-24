package com.chatcode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatcode.data.Profile
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.components.Avatar
import com.chatcode.ui.components.EmptyState
import com.chatcode.ui.components.PostItem
import com.chatcode.ui.components.ScreenTitle
import com.chatcode.ui.components.edgeFade
import com.chatcode.ui.components.formatCount
import com.chatcode.ui.components.rememberPostActions
import com.chatcode.ui.popup.DialogSpec
import com.chatcode.ui.popup.LocalPopups
import com.chatcode.ui.theme.DividerColor
import com.chatcode.ui.theme.TextSecondary

@Composable
fun AccountScreen(
    vm: MainViewModel,
    onLogout: () -> Unit,
    onOpenUser: (String) -> Unit = {}
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val myPosts by vm.myPosts.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val popups = LocalPopups.current
    val actions = rememberPostActions(
        vm = vm,
        onOpenUser = onOpenUser,
        onLike = { post -> vm.toggleLike(post.id) },
        onDelete = { post -> vm.deletePost(post.id) }
    )

    val confirmLogout = {
        popups.showDialog(
            DialogSpec(
                title = "Keluar dari akun?",
                message = "Kamu perlu masuk lagi untuk memakai ChatCode.",
                confirmLabel = "Keluar",
                destructive = true,
                onConfirm = onLogout
            )
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ScreenTitle("Akun")
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .edgeFade(top = 24.dp, bottom = 120.dp),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            val current = profile
            if (current != null) {
                item(key = "header", contentType = "header") {
                    Column {
                        ProfileHeader(
                            profile = current,
                            postCount = myPosts.size,
                            projectCount = projects.size,
                            onEditProfile = {
                                popups.showSheet { dismiss ->
                                    EditProfileSheetContent(vm, current, onDismiss = dismiss)
                                }
                            },
                            onLogout = confirmLogout
                        )
                        HorizontalDivider(color = DividerColor)
                        Text(
                            text = "Postingan Saya",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                        HorizontalDivider(color = DividerColor)
                    }
                }
            }
            if (myPosts.isEmpty()) {
                item(key = "kosong", contentType = "kosong") {
                    EmptyState(
                        icon = Icons.Outlined.Edit,
                        title = "Belum ada postingan",
                        message = "Postingan yang kamu buat akan muncul di sini."
                    )
                }
            }
            items(myPosts, key = { it.id }, contentType = { "post" }) { post ->
                Column {
                    PostItem(post = post, actions = actions)
                    HorizontalDivider(color = DividerColor)
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: Profile,
    postCount: Int,
    projectCount: Int,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(profile.name, profile.username.hashCode(), 92.dp, url = profile.avatarUrl)
        Spacer(Modifier.height(12.dp))
        Text(
            text = profile.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(text = "@" + profile.username, color = TextSecondary, fontSize = 14.sp)
        if (profile.bio.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = profile.bio,
                color = Color(0xFFDDDDDD),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Stat(postCount.toString(), "Postingan")
            Stat(projectCount.toString(), "Proyek")
            Stat(formatCount(profile.followers), "Pengikut")
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onEditProfile,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Edit Profil")
            }
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Keluar")
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
    }
}
