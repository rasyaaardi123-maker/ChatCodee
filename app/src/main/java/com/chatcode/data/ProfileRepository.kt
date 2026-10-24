package com.chatcode.data

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ProfileRepository {
    val me: StateFlow<Profile?>
    suspend fun loadMe(): Result<Unit>
    suspend fun profileOf(userId: String): Result<Profile>
    suspend fun updateProfile(name: String, bio: String): Result<Unit>
    suspend fun updateAvatar(context: Context, uri: Uri): Result<Unit>
    suspend fun setFollow(userId: String, follow: Boolean): Result<Unit>
}

class SupabaseProfileRepository(private val client: SupabaseClient) : ProfileRepository {

    private val _me = MutableStateFlow<Profile?>(null)
    override val me: StateFlow<Profile?> = _me.asStateFlow()

    private fun myId(): String? = client.auth.currentUserOrNull()?.id

    override suspend fun loadMe(): Result<Unit> = safeCall {
        val id = myId() ?: error("Kamu belum masuk.")
        val row = fetchRow(id) ?: error("Profil tidak ditemukan.")
        _me.value = buildProfile(row, id)
    }

    override suspend fun profileOf(userId: String): Result<Profile> = safeCall {
        val row = fetchRow(userId) ?: error("Pengguna tidak ditemukan.")
        buildProfile(row, myId())
    }

    override suspend fun updateProfile(name: String, bio: String): Result<Unit> = safeCall {
        val id = myId() ?: error("Kamu belum masuk.")
        client.from("profiles").update(ProfileUpdate(name, bio)) {
            filter { eq("id", id) }
        }
        loadMe()
        Unit
    }

    override suspend fun updateAvatar(context: Context, uri: Uri): Result<Unit> = safeCall {
        val id = myId() ?: error("Kamu belum masuk.")
        val bytes = Media.compressImage(context, uri, 512, 80)
        val path = id + "/avatar.jpg"
        client.storage.from("avatars").upload(path, bytes) {
            upsert = true
        }
        val url = client.storage.from("avatars").publicUrl(path) + "?v=" + System.currentTimeMillis()
        client.from("profiles").update(AvatarUpdate(url)) {
            filter { eq("id", id) }
        }
        loadMe()
        Unit
    }

    override suspend fun setFollow(userId: String, follow: Boolean): Result<Unit> = safeCall {
        val id = myId() ?: error("Kamu belum masuk.")
        if (follow) {
            client.from("follows").insert(FollowInsert(id, userId))
        } else {
            client.from("follows").delete {
                filter {
                    eq("follower_id", id)
                    eq("following_id", userId)
                }
            }
        }
        Unit
    }

    private suspend fun fetchRow(id: String): ProfileRow? =
        client.from("profiles")
            .select(Columns.raw("id, username, display_name, bio, avatar_url")) {
                filter { eq("id", id) }
            }.decodeSingleOrNull<ProfileRow>()

    private suspend fun buildProfile(row: ProfileRow, viewer: String?): Profile = coroutineScope {
        val posts = async { countOf("posts", "author_id", row.id) }
        val projects = async { countOf("projects", "owner_id", row.id) }
        val followers = async { countOf("follows", "following_id", row.id) }
        val following = async { countOf("follows", "follower_id", row.id) }
        val isFollowing = async {
            if (viewer == null || viewer == row.id) {
                false
            } else {
                client.from("follows").select(Columns.list("follower_id")) {
                    count(Count.EXACT)
                    filter {
                        eq("follower_id", viewer)
                        eq("following_id", row.id)
                    }
                    limit(1L)
                }.countOrNull()?.let { it > 0L } ?: false
            }
        }
        Profile(
            id = row.id,
            name = row.shownName(),
            username = row.username,
            bio = row.bio,
            avatarUrl = row.avatarUrl,
            followers = followers.await(),
            following = following.await(),
            postCount = posts.await(),
            projectCount = projects.await(),
            isFollowing = isFollowing.await()
        )
    }

    private suspend fun countOf(table: String, column: String, value: String): Int =
        client.from(table).select(Columns.list(column)) {
            count(Count.EXACT)
            filter { eq(column, value) }
            limit(1L)
        }.countOrNull()?.toInt() ?: 0
}
