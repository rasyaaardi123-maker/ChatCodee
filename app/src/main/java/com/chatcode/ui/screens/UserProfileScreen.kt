package com.chatcode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatcode.data.Post
import com.chatcode.data.Profile
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.components.Avatar
import com.chatcode.ui.components.DetailTopBar
import com.chatcode.ui.components.EmptyState
import com.chatcode.ui.components.PostItem
import com.chatcode.ui.components.edgeFade
import com.chatcode.ui.components.formatCount
import com.chatcode.ui.components.rememberPostActions
import com.chatcode.ui.theme.DividerColor
import com.chatcode.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun UserProfileScreen(
    vm: MainViewModel,
    userId: String,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit = {}
) {
    var profile by remember(userId) { mutableStateOf<Profile?>(null) }
    var posts by remember(userId) { mutableStateOf<List<Post>?>(null) }
    var error by remember(userId) { mutableStateOf<String?>(null) }
    var followBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val actions = rememberPostActions(
        vm = vm,
        onOpenUser = onOpenUser,
        onLike = { post -> vm.toggleLike(post.id) },
        onDelete = { post -> vm.deletePost(post.id) }
    )

    LaunchedEffect(userId) {
        error = null
        profile = null
        posts = null
        val pResult = vm.loadUserProfile(userId)
        if (pResult.isFailure) {
            error = pResult.exceptionOrNull()?.message ?: "Gagal memuat profil."
            return@LaunchedEffect
        }
        profile = pResult.getOrNull()
        val postsResult = vm.loadUserPosts(userId)
        if (postsResult.isSuccess) {
            posts = postsResult.getOrNull() ?: emptyList()
        } else {
            posts = emptyList()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        DetailTopBar(title = profile?.name ?: "Profil", onBack = onBack)

        when {
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text(text = error ?: "Terjadi kesalahan", color = Color.White, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = {
                            scope.launch {
                                error = null
                                val r = vm.loadUserProfile(userId)
                                if (r.isSuccess) {
                                    profile = r.getOrNull()
                                    posts = vm.loadUserPosts(userId).getOrDefault(emptyList())
                                } else {
                                    error = r.exceptionOrNull()?.message
                                }
                            }
                        }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                            Text("Coba lagi")
                        }
                    }
                }
            }
            profile == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            else -> {
                val current = profile!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .edgeFade(top = 16.dp, bottom = 24.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item(key = "header", contentType = "header") {
                        Column {
                            UserProfileHeader(
                                profile = current,
                                postCount = posts?.size ?: current.postCount,
                                followBusy = followBusy,
                                onFollowToggle = {
                                    if (followBusy) return@UserProfileHeader
                                    followBusy = true
                                    scope.launch {
                                        val next = !current.isFollowing
                                        val ok = vm.setFollow(userId, next)
                                        if (ok) {
                                            val refreshed = vm.loadUserProfile(userId).getOrNull()
                                            if (refreshed != null) profile = refreshed
                                        }
                                        followBusy = false
                                    }
                                }
                            )
                            HorizontalDivider(color = DividerColor)
                            Text(
                                text = "Postingan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                            HorizontalDivider(color = DividerColor)
                        }
                    }
                    val list = posts
                    when {
                        list == null -> {
                            item(key = "loading_posts") {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                        list.isEmpty() -> {
                            item(key = "empty_posts") {
                                EmptyState(
                                    icon = Icons.Outlined.Edit,
                                    title = "Belum ada postingan",
                                    message = "@${current.username} belum membuat postingan."
                                )
                            }
                        }
                        else -> {
                            items(list, key = { it.id }, contentType = { "post" }) { post ->
                                Column {
                                    PostItem(post = post, actions = actions)
                                    HorizontalDivider(color = DividerColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileHeader(
    profile: Profile,
    postCount: Int,
    followBusy: Boolean,
    onFollowToggle: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(
            name = profile.name,
            seed = profile.username.hashCode(),
            size = 92.dp,
            url = profile.avatarUrl
        )
        Spacer(Modifier.height(12.dp))
        Text(text = profile.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(text = "@${profile.username}", color = TextSecondary, fontSize = 14.sp)
        if (profile.bio.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = profile.bio,
                color = Color(0xFFDDDDDD),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(formatCount(postCount), "Postingan")
            Stat(formatCount(profile.followers), "Pengikut")
            Stat(formatCount(profile.following), "Mengikuti")
        }
        Spacer(Modifier.height(16.dp))
        // Only show follow button for other users (isFollowing indicates relationship, but we show for all except self handled by caller)
        // Caller ensures this screen is for other user, but we still check: if profile is not self, show button
        // We determine self by isFollowing logic not reliable, so always show toggle for this screen
        Button(
            onClick = onFollowToggle,
            enabled = !followBusy,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (profile.isFollowing) Color(0xFF2A2A2A) else Color.White,
                contentColor = if (profile.isFollowing) Color.White else Color.Black
            ),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            if (followBusy) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = if (profile.isFollowing) Color.White else Color.Black)
                Spacer(Modifier.size(8.dp))
            }
            Text(if (profile.isFollowing) "Mengikuti" else "Ikuti")
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
