package com.chatcode.data

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

interface FeedRepository {
    val posts: StateFlow<List<Post>>
    val searchPosts: StateFlow<List<Post>>
    val searchUsers: StateFlow<List<UserSummary>>
    val myPosts: StateFlow<List<Post>>
    val stories: StateFlow<List<StoryGroup>>
    val hasMore: StateFlow<Boolean>

    suspend fun refresh(): Result<Unit>
    suspend fun loadMore(): Result<Unit>
    suspend fun loadStories(): Result<Unit>
    suspend fun loadMyPosts(): Result<Unit>
    suspend fun search(query: String): Result<Unit>
    suspend fun postsOf(userId: String): Result<List<Post>>
    suspend fun toggleLike(postId: Long): Result<Unit>
    suspend fun setLike(postId: Long, liked: Boolean): Result<Unit>
    suspend fun createPost(context: Context, text: String, imageUri: Uri?): Result<Unit>
    suspend fun createStory(context: Context, imageUri: Uri): Result<Unit>
    suspend fun deletePost(postId: Long): Result<Unit>
    suspend fun comments(postId: Long): Result<List<Comment>>
    suspend fun addComment(postId: Long, body: String): Result<Unit>
    suspend fun observeRealtime()
}

class SupabaseFeedRepository(private val client: SupabaseClient) : FeedRepository {

    private companion object {
        const val PAGE = 20L
        const val PROFILE_EMBED = "profiles(id, username, display_name, avatar_url)"
        const val POST_COLUMNS =
            "id, author_id, body, image_url, created_at, " + PROFILE_EMBED +
                ", post_likes(count), comments(count)"
    }

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _searchPosts = MutableStateFlow<List<Post>>(emptyList())
    private val _searchUsers = MutableStateFlow<List<UserSummary>>(emptyList())
    private val _myPosts = MutableStateFlow<List<Post>>(emptyList())
    private val _stories = MutableStateFlow<List<StoryGroup>>(emptyList())
    private val _hasMore = MutableStateFlow(false)

    override val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    override val searchPosts: StateFlow<List<Post>> = _searchPosts.asStateFlow()
    override val searchUsers: StateFlow<List<UserSummary>> = _searchUsers.asStateFlow()
    override val myPosts: StateFlow<List<Post>> = _myPosts.asStateFlow()
    override val stories: StateFlow<List<StoryGroup>> = _stories.asStateFlow()
    override val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private fun myId(): String? = client.auth.currentUserOrNull()?.id

    override suspend fun refresh(): Result<Unit> = safeCall {
        val rows = client.from("posts").select(Columns.raw(POST_COLUMNS)) {
            order("created_at", Order.DESCENDING)
            range(0L, PAGE - 1)
        }.decodeList<PostRow>()
        _posts.value = rows.toPosts()
        _hasMore.value = rows.size.toLong() == PAGE
    }

    override suspend fun loadMore(): Result<Unit> = safeCall {
        val from = _posts.value.size.toLong()
        val rows = client.from("posts").select(Columns.raw(POST_COLUMNS)) {
            order("created_at", Order.DESCENDING)
            range(from, from + PAGE - 1)
        }.decodeList<PostRow>()
        val more = rows.toPosts()
        _posts.value = (_posts.value + more).distinctBy { it.id }
        _hasMore.value = rows.size.toLong() == PAGE
    }

    override suspend fun loadStories(): Result<Unit> = safeCall {
        val since = Instant.now().minus(24, ChronoUnit.HOURS).toString()
        val rows = client.from("stories")
            .select(Columns.raw("id, author_id, image_url, created_at, " + PROFILE_EMBED)) {
                filter { gt("created_at", since) }
                order("created_at", Order.DESCENDING)
                limit(100L)
            }.decodeList<StoryDbRow>()
        val me = myId()
        val groups = rows.groupBy { it.authorId }.map { (authorId, list) ->
            val profile = list.first().profiles
            StoryGroup(
                authorId = authorId,
                name = profile?.shownName() ?: "Pengguna",
                username = profile?.username ?: "",
                avatarUrl = profile?.avatarUrl,
                avatarSeed = (profile?.username ?: authorId).hashCode(),
                mine = authorId == me,
                items = list.sortedBy { it.createdAt }
                    .map { StoryItem(it.id, it.imageUrl, it.createdAt) }
            )
        }.sortedWith(
            compareByDescending<StoryGroup> { it.mine }
                .thenByDescending { group -> group.items.last().createdAt }
        )
        _stories.value = groups
    }

    override suspend fun loadMyPosts(): Result<Unit> = safeCall {
        val me = myId() ?: error("Kamu belum masuk.")
        _myPosts.value = fetchByAuthor(me)
    }

    override suspend fun postsOf(userId: String): Result<List<Post>> = safeCall {
        fetchByAuthor(userId)
    }

    override suspend fun search(query: String): Result<Unit> = safeCall {
        val q = query.trim().removePrefix("@")
            .filterNot { it == ',' || it == '(' || it == ')' || it == '%' || it == '*' }
        if (q.isEmpty()) {
            _searchPosts.value = emptyList()
            _searchUsers.value = emptyList()
            return@safeCall
        }
        val pattern = "%" + q + "%"

        val users = client.from("profiles")
            .select(Columns.raw("id, username, display_name, avatar_url")) {
                filter {
                    or {
                        ilike("username", pattern)
                        ilike("display_name", pattern)
                    }
                }
                limit(10L)
            }.decodeList<ProfileRow>()

        val ids = users.map { it.id }
        val rows = if (ids.isEmpty()) {
            client.from("posts").select(Columns.raw(POST_COLUMNS)) {
                filter { ilike("body", pattern) }
                order("created_at", Order.DESCENDING)
                limit(40L)
            }.decodeList<PostRow>()
        } else {
            client.from("posts").select(Columns.raw(POST_COLUMNS)) {
                filter {
                    or {
                        ilike("body", pattern)
                        isIn("author_id", ids)
                    }
                }
                order("created_at", Order.DESCENDING)
                limit(40L)
            }.decodeList<PostRow>()
        }

        _searchUsers.value = users.map {
            UserSummary(it.id, it.shownName(), it.username, it.avatarUrl, it.username.hashCode())
        }
        _searchPosts.value = rows.toPosts()
    }

    override suspend fun toggleLike(postId: Long): Result<Unit> {
        val current = (_posts.value + _searchPosts.value + _myPosts.value)
            .firstOrNull { it.id == postId }
            ?: return Result.success(Unit)
        val nowLiked = !current.liked
        patch(postId) {
            it.copy(
                liked = nowLiked,
                likes = (it.likes + if (nowLiked) 1 else -1).coerceAtLeast(0)
            )
        }
        val result = setLike(postId, nowLiked)
        if (result.isFailure) {
            patch(postId) { it.copy(liked = current.liked, likes = current.likes) }
        }
        return result
    }

    override suspend fun setLike(postId: Long, liked: Boolean): Result<Unit> = safeCall {
        val me = myId() ?: error("Kamu belum masuk.")
        if (liked) {
            client.from("post_likes").insert(LikeInsert(postId))
        } else {
            client.from("post_likes").delete {
                filter {
                    eq("post_id", postId)
                    eq("user_id", me)
                }
            }
        }
        Unit
    }

    override suspend fun createPost(context: Context, text: String, imageUri: Uri?): Result<Unit> =
        safeCall {
            val me = myId() ?: error("Kamu belum masuk.")
            var imageUrl: String? = null
            if (imageUri != null) {
                val bytes = Media.compressImage(context, imageUri)
                val path = me + "/" + System.currentTimeMillis() + ".jpg"
                client.storage.from("posts").upload(path, bytes)
                imageUrl = client.storage.from("posts").publicUrl(path)
            }
            client.from("posts").insert(PostInsert(body = text, imageUrl = imageUrl))
            refresh()
            Unit
        }

    override suspend fun createStory(context: Context, imageUri: Uri): Result<Unit> = safeCall {
        val me = myId() ?: error("Kamu belum masuk.")
        val bytes = Media.compressImage(context, imageUri)
        val path = me + "/" + System.currentTimeMillis() + ".jpg"
        client.storage.from("stories").upload(path, bytes)
        val url = client.storage.from("stories").publicUrl(path)
        client.from("stories").insert(StoryInsert(url))
        loadStories()
        Unit
    }

    override suspend fun deletePost(postId: Long): Result<Unit> = safeCall {
        client.from("posts").delete {
            filter { eq("id", postId) }
        }
        _posts.update { list -> list.filter { it.id != postId } }
        _searchPosts.update { list -> list.filter { it.id != postId } }
        _myPosts.update { list -> list.filter { it.id != postId } }
        Unit
    }

    override suspend fun comments(postId: Long): Result<List<Comment>> = safeCall {
        val rows = client.from("comments")
            .select(Columns.raw("id, author_id, body, created_at, " + PROFILE_EMBED)) {
                filter { eq("post_id", postId) }
                order("created_at", Order.ASCENDING)
                limit(200L)
            }.decodeList<CommentRow>()
        rows.map { row ->
            val profile = row.profiles
            Comment(
                id = row.id,
                authorId = row.authorId,
                name = profile?.shownName() ?: "Pengguna",
                username = profile?.username ?: "",
                avatarUrl = profile?.avatarUrl,
                avatarSeed = (profile?.username ?: row.authorId).hashCode(),
                text = row.body,
                time = relativeTime(row.createdAt)
            )
        }
    }

    override suspend fun addComment(postId: Long, body: String): Result<Unit> = safeCall {
        client.from("comments").insert(CommentInsert(postId, body))
        patch(postId) { it.copy(comments = it.comments + 1) }
        Unit
    }

    override suspend fun observeRealtime() {
        val channel = client.channel("feed-posts")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "posts"
        }
        channel.subscribe()
        try {
            inserts.collect { refresh() }
        } finally {
            withContext(NonCancellable) {
                client.realtime.removeChannel(channel)
            }
        }
    }

    private suspend fun fetchByAuthor(userId: String): List<Post> =
        client.from("posts").select(Columns.raw(POST_COLUMNS)) {
            filter { eq("author_id", userId) }
            order("created_at", Order.DESCENDING)
            limit(50L)
        }.decodeList<PostRow>().toPosts()

    private suspend fun likedAmong(ids: List<Long>): Set<Long> {
        val me = myId() ?: return emptySet()
        if (ids.isEmpty()) return emptySet()
        return client.from("post_likes").select(Columns.list("post_id")) {
            filter {
                eq("user_id", me)
                isIn("post_id", ids)
            }
        }.decodeList<LikeRow>().map { it.postId }.toSet()
    }

    private suspend fun List<PostRow>.toPosts(): List<Post> {
        val liked = likedAmong(map { it.id })
        val me = myId()
        return map { it.toPost(me, liked) }
    }

    private fun PostRow.toPost(me: String?, liked: Set<Long>): Post {
        val profile = profiles
        return Post(
            id = id,
            name = profile?.shownName() ?: "Pengguna",
            username = profile?.username ?: "",
            time = relativeTime(createdAt),
            text = body,
            imageUri = imageUrl,
            imageSeed = -1,
            likes = likes.firstOrNull()?.count ?: 0,
            comments = comments.firstOrNull()?.count ?: 0,
            liked = id in liked,
            avatarSeed = (profile?.username ?: authorId).hashCode(),
            mine = authorId == me,
            authorId = authorId,
            avatarUrl = profile?.avatarUrl
        )
    }

    private fun patch(postId: Long, transform: (Post) -> Post) {
        fun List<Post>.mapped(): List<Post> =
            map { if (it.id == postId) transform(it) else it }
        _posts.update { it.mapped() }
        _searchPosts.update { it.mapped() }
        _myPosts.update { it.mapped() }
    }
}
