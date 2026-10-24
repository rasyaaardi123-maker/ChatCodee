package com.chatcode.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatcode.data.Comment
import com.chatcode.data.Graph
import com.chatcode.data.Post
import com.chatcode.data.Profile
import com.chatcode.data.UserSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainViewModel : ViewModel() {

    private val feed = Graph.feed
    private val profiles = Graph.profiles
    private val chatRepo = Graph.chat
    private val projectRepo = Graph.projects

    val profile: StateFlow<Profile?> = profiles.me
    val stories = feed.stories
    val chats = chatRepo.chats
    val projects = projectRepo.projects
    val myPosts: StateFlow<List<Post>> = feed.myPosts
    val hasMore: StateFlow<Boolean> = feed.hasMore

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var loadingMore = false

    val homePosts: StateFlow<List<Post>> = combine(feed.posts, feed.searchPosts, _query) { feedPosts, found, q ->
        if (q.isBlank()) feedPosts else found
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchUsers: StateFlow<List<UserSummary>> = combine(feed.searchUsers, _query) { users, q ->
        if (q.isBlank()) emptyList() else users
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            report(feed.refresh())
            _loading.value = false
        }
        viewModelScope.launch { report(feed.loadStories()) }
        viewModelScope.launch {
            report(profiles.loadMe())
            report(feed.loadMyPosts())
        }
        viewModelScope.launch {
            try {
                feed.observeRealtime()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Realtime gagal tersambung: feed tetap bisa dimuat ulang manual.
            }
        }
        viewModelScope.launch {
            _query.debounce(350).distinctUntilChanged().collectLatest { q ->
                report(feed.search(q))
            }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            report(feed.refresh())
            report(feed.loadStories())
        }
    }

    fun loadMore() {
        if (loadingMore || !feed.hasMore.value || _query.value.isNotBlank()) return
        loadingMore = true
        viewModelScope.launch {
            report(feed.loadMore())
            loadingMore = false
        }
    }

    fun toggleLike(postId: Long) {
        viewModelScope.launch { report(feed.toggleLike(postId)) }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            val result = feed.deletePost(postId)
            report(result)
            if (result.isSuccess) profiles.loadMe()
        }
    }

    suspend fun createPost(context: Context, text: String, uri: Uri?): Boolean {
        val result = feed.createPost(context.applicationContext, text, uri)
        report(result)
        if (result.isSuccess) {
            feed.loadMyPosts()
            profiles.loadMe()
        }
        return result.isSuccess
    }

    suspend fun createStory(context: Context, uri: Uri): Boolean {
        val result = feed.createStory(context.applicationContext, uri)
        report(result)
        return result.isSuccess
    }

    suspend fun comments(postId: Long): Result<List<Comment>> = feed.comments(postId)

    suspend fun addComment(postId: Long, body: String): Boolean {
        val result = feed.addComment(postId, body)
        report(result)
        return result.isSuccess
    }

    suspend fun updateProfile(name: String, bio: String): Boolean {
        val result = profiles.updateProfile(name, bio)
        report(result)
        return result.isSuccess
    }

    suspend fun updateAvatar(context: Context, uri: Uri): Boolean {
        val result = profiles.updateAvatar(context.applicationContext, uri)
        report(result)
        return result.isSuccess
    }

    suspend fun loadUserProfile(userId: String): Result<Profile> = profiles.profileOf(userId)

    suspend fun loadUserPosts(userId: String): Result<List<Post>> = feed.postsOf(userId)

    suspend fun setLike(postId: Long, liked: Boolean): Boolean {
        val result = feed.setLike(postId, liked)
        report(result)
        return result.isSuccess
    }

    suspend fun setFollow(userId: String, follow: Boolean): Boolean {
        val result = profiles.setFollow(userId, follow)
        report(result)
        return result.isSuccess
    }

    fun sendMessage(chatId: Long, text: String) {
        notReady()
    }

    fun sendVoiceNote(chatId: Long, durationSec: Int) {
        notReady()
    }

    fun sendMedia(chatId: Long, uri: String, isVideo: Boolean) {
        notReady()
    }

    fun sendFile(chatId: Long, fileName: String) {
        notReady()
    }

    fun markRead(chatId: Long) {
        // Status baca diaktifkan bersama chat nyata di fase 3.
    }

    private fun notReady() {
        _messages.tryEmit("Fitur ini diaktifkan di fase berikutnya.")
    }

    private fun <T> report(result: Result<T>) {
        result.onFailure { _messages.tryEmit(it.message ?: "Terjadi kesalahan.") }
    }
}
