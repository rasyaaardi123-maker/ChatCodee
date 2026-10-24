package com.chatcode.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRow(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String = "",
    val bio: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class CountRow(val count: Int = 0)

@Serializable
data class PostRow(
    val id: Long,
    @SerialName("author_id") val authorId: String,
    val body: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    val profiles: ProfileRow? = null,
    @SerialName("post_likes") val likes: List<CountRow> = emptyList(),
    val comments: List<CountRow> = emptyList()
)

@Serializable
data class LikeRow(@SerialName("post_id") val postId: Long)

@Serializable
data class StoryDbRow(
    val id: Long,
    @SerialName("author_id") val authorId: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("created_at") val createdAt: String,
    val profiles: ProfileRow? = null
)

@Serializable
data class CommentRow(
    val id: Long,
    @SerialName("author_id") val authorId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    val profiles: ProfileRow? = null
)

@Serializable
data class PostInsert(
    val body: String,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class StoryInsert(@SerialName("image_url") val imageUrl: String)

@Serializable
data class CommentInsert(
    @SerialName("post_id") val postId: Long,
    val body: String
)

@Serializable
data class LikeInsert(@SerialName("post_id") val postId: Long)

@Serializable
data class FollowInsert(
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String
)

@Serializable
data class ProfileUpdate(
    @SerialName("display_name") val displayName: String,
    val bio: String
)

@Serializable
data class AvatarUpdate(@SerialName("avatar_url") val avatarUrl: String)

fun ProfileRow.shownName(): String =
    displayName.takeIf { it.isNotBlank() } ?: username
