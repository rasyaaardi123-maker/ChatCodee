package com.chatcode.data

import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ChatRepository {
    val chats: StateFlow<List<Chat>>
}

interface ProjectRepository {
    val projects: StateFlow<List<Project>>
}

class SupabaseChatRepository(private val client: SupabaseClient) : ChatRepository {
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    override val chats: StateFlow<List<Chat>> = _chats.asStateFlow()
}

class SupabaseProjectRepository(private val client: SupabaseClient) : ProjectRepository {
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    override val projects: StateFlow<List<Project>> = _projects.asStateFlow()
}

object Graph {
    val auth: AuthRepository by lazy { SupabaseAuthRepository(Backend.client) }
    val feed: FeedRepository by lazy { SupabaseFeedRepository(Backend.client) }
    val profiles: ProfileRepository by lazy { SupabaseProfileRepository(Backend.client) }
    val chat: ChatRepository by lazy { SupabaseChatRepository(Backend.client) }
    val projects: ProjectRepository by lazy { SupabaseProjectRepository(Backend.client) }
}
