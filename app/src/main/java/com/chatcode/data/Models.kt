package com.chatcode.data

data class Post(
    val id: Long,
    val name: String,
    val username: String,
    val time: String,
    val text: String,
    val imageUri: String?,
    val imageSeed: Int,
    val likes: Int,
    val comments: Int,
    val liked: Boolean,
    val avatarSeed: Int,
    val mine: Boolean,
    val authorId: String = "",
    val avatarUrl: String? = null
)

data class StoryItem(
    val id: Long,
    val imageUrl: String,
    val createdAt: String
)

data class StoryGroup(
    val authorId: String,
    val name: String,
    val username: String,
    val avatarUrl: String?,
    val avatarSeed: Int,
    val mine: Boolean,
    val items: List<StoryItem>
)

data class Comment(
    val id: Long,
    val authorId: String,
    val name: String,
    val username: String,
    val avatarUrl: String?,
    val avatarSeed: Int,
    val text: String,
    val time: String
)

data class UserSummary(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String?,
    val avatarSeed: Int
)

enum class MessageKind { TEXT, VOICE, MEDIA, FILE }

data class Message(
    val id: Long,
    val text: String,
    val fromMe: Boolean,
    val kind: MessageKind = MessageKind.TEXT,
    val durationSec: Int = 0,
    val uri: String? = null,
    val isVideo: Boolean = false
) {
    val preview: String
        get() = when (kind) {
            MessageKind.TEXT -> text
            MessageKind.VOICE -> "Pesan suara"
            MessageKind.MEDIA -> if (isVideo) "Video" else "Foto"
            MessageKind.FILE -> text
        }
}

data class Chat(
    val id: Long,
    val name: String,
    val avatarSeed: Int,
    val time: String,
    val unread: Int,
    val messages: List<Message>
) {
    val lastMessage: String get() = messages.lastOrNull()?.preview ?: ""
}

data class Project(
    val id: Long,
    val name: String,
    val stack: List<String>,
    val progress: Float,
    val updated: String,
    val status: String,
    val description: String,
    val files: List<String>,
    val code: String
)

data class Profile(
    val id: String,
    val name: String,
    val username: String,
    val bio: String,
    val avatarUrl: String?,
    val followers: Int,
    val following: Int,
    val postCount: Int = 0,
    val projectCount: Int = 0,
    val isFollowing: Boolean = false
)
