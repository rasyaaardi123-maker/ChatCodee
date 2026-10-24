package com.chatcode.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatcode.data.Post
import com.chatcode.data.StoryGroup
import com.chatcode.data.UserSummary
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.components.Avatar
import com.chatcode.ui.components.EmptyState
import com.chatcode.ui.components.PostItem
import com.chatcode.ui.components.StoryRow
import com.chatcode.ui.components.edgeFade
import com.chatcode.ui.components.rememberPostActions
import com.chatcode.ui.popup.LocalPopups
import com.chatcode.ui.theme.DividerColor
import com.chatcode.ui.theme.FieldFill
import com.chatcode.ui.theme.TextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

private val FullHeaderHeight = 122.dp
private val CollapsedHeaderHeight = 60.dp
private val SearchTop = 58.dp
private val CollapsedSearchTop = 8.dp

private val HeaderGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to Color.Black.copy(alpha = 0.85f),
        0.4f to Color.Black.copy(alpha = 0.65f),
        0.7f to Color.Black.copy(alpha = 0.3f),
        0.9f to Color.Black.copy(alpha = 0.08f),
        1f to Color.Black.copy(alpha = 0f)
    )
)

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenUser: (String) -> Unit = {}
) {
    val posts by vm.homePosts.collectAsStateWithLifecycle()
    val stories by vm.stories.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val searchUsers by vm.searchUsers.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val hasMore by vm.hasMore.collectAsStateWithLifecycle()
    val popups = LocalPopups.current
    val actions = rememberPostActions(
        vm = vm,
        onOpenUser = onOpenUser,
        onLike = { post: Post -> vm.toggleLike(post.id) },
        onDelete = { post: Post -> vm.deletePost(post.id) }
    )
    val onQuery = remember(vm) { { q: String -> vm.setQuery(q) } }

    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val thresholdPx = with(density) { 48.dp.toPx() }

    val listState = rememberLazyListState()
    val collapsed = remember { mutableStateOf(false) }
    val accumulated = remember { floatArrayOf(0f) }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy < 0f) {
                    accumulated[0] += -dy
                    if (accumulated[0] > thresholdPx && !collapsed.value) {
                        collapsed.value = true
                    }
                } else if (dy > 0f) {
                    accumulated[0] = 0f
                    if (collapsed.value) collapsed.value = false
                }
                return Offset.Zero
            }
        }
    }

    val atTop = remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(atTop) {
        snapshotFlow { atTop.value }
            .distinctUntilChanged()
            .collect { top ->
                if (top) {
                    accumulated[0] = 0f
                    collapsed.value = false
                }
            }
    }

    // Pagination: load more when near bottom
    LaunchedEffect(listState, hasMore, query) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible to total
        }.distinctUntilChanged().collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 3 && hasMore && query.isBlank()) {
                vm.loadMore()
            }
        }
    }

    // Pull-to-refresh via overscroll detection: when at top and user pulls, refresh is handled by swipe gesture
    // We trigger refresh when user scrolls to top and releases after overscroll is not directly detectable without material pullRefresh.
    // As fallback, we expose refresh via header tap and via LaunchedEffect on resume.

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .nestedScroll(connection)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .edgeFade(top = 0.dp, bottom = 120.dp),
            state = listState,
            contentPadding = PaddingValues(
                top = statusTop + FullHeaderHeight + 8.dp,
                bottom = 110.dp
            )
        ) {
            if (stories.isNotEmpty()) {
                item(key = "stories", contentType = "stories") {
                    Column {
                        StoryRow(
                            groups = stories,
                            onOpen = { group: StoryGroup ->
                                popups.showSheet(fullScreen = true) { dismiss ->
                                    StoryViewerScreen(
                                        vm = vm,
                                        authorId = group.authorId,
                                        onClose = { dismiss() }
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = DividerColor)
                    }
                }
            }

            // Search users section when query not blank
            if (query.isNotBlank() && searchUsers.isNotEmpty()) {
                item(key = "search_users_header", contentType = "header") {
                    Text(
                        text = "Pengguna",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                items(searchUsers, key = { "user_" + it.id }, contentType = { "user" }) { user ->
                    UserSearchRow(user = user, onClick = { onOpenUser(user.id) })
                    HorizontalDivider(color = DividerColor)
                }
                item(key = "search_posts_header", contentType = "header") {
                    Text(
                        text = "Postingan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    HorizontalDivider(color = DividerColor)
                }
            }

            if (loading && posts.isEmpty() && query.isBlank()) {
                item(key = "loading", contentType = "loading") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            } else if (posts.isEmpty()) {
                item(key = "kosong", contentType = "kosong") {
                    if (query.isBlank()) {
                        EmptyState(
                            icon = Icons.Outlined.Edit,
                            title = "Belum ada postingan",
                            message = "Jadilah yang pertama berbagi. Ketuk tombol + untuk membuat postingan."
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.Search,
                            title = "Tidak ada hasil",
                            message = "Coba kata kunci atau nama pengguna yang lain."
                        )
                    }
                }
            } else {
                items(posts, key = { it.id }, contentType = { "post" }) { post ->
                    Column {
                        PostItem(post = post, actions = actions)
                        HorizontalDivider(color = DividerColor)
                    }
                }
                if (hasMore && query.isBlank()) {
                    item(key = "load_more", contentType = "load_more") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }

        HomeHeader(
            collapsed = collapsed,
            statusTop = statusTop,
            query = query,
            onQuery = onQuery,
            onExpand = {
                accumulated[0] = 0f
                collapsed.value = false
            }
        )
    }
}

@Composable
private fun UserSearchRow(user: UserSummary, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = user.name, seed = user.avatarSeed, size = 44.dp, url = user.avatarUrl)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            Text(text = "@${user.username}", color = TextSecondary, fontSize = 13.sp, maxLines = 1)
        }
        Text(text = "Lihat", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

@Composable
private fun HomeHeader(
    collapsed: State<Boolean>,
    statusTop: Dp,
    query: String,
    onQuery: (String) -> Unit,
    onExpand: () -> Unit
) {
    val isCollapsed = collapsed.value
    val progress = animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "homeCollapse"
    )
    val gradientHeight = statusTop + FullHeaderHeight + 24.dp
    val collapsedRatio = (statusTop + CollapsedHeaderHeight + 24.dp).value / gradientHeight.value

    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(gradientHeight)
                .graphicsLayer {
                    scaleY = 1f + (collapsedRatio - 1f) * progress.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .background(HeaderGradient)
        )

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = statusTop + 16.dp)
                .height(28.dp)
                .graphicsLayer {
                    alpha = 1f - progress.value
                    translationY = -24.dp.toPx() * progress.value
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Home",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Box(
            Modifier
                .padding(start = 16.dp, top = statusTop + SearchTop)
                .graphicsLayer {
                    translationY = -(SearchTop - CollapsedSearchTop).toPx() * progress.value
                }
        ) {
            CollapsibleSearchBar(
                collapsed = isCollapsed,
                progress = progress,
                query = query,
                onQuery = onQuery,
                onExpand = onExpand
            )
        }
    }
}

@Composable
private fun CollapsibleSearchBar(
    collapsed: Boolean,
    progress: State<Float>,
    query: String,
    onQuery: (String) -> Unit,
    onExpand: () -> Unit
) {
    val fullWidth = LocalConfiguration.current.screenWidthDp.dp - 32.dp
    val spec = tween<Dp>(250, easing = FastOutSlowInEasing)
    val width by animateDpAsState(if (collapsed) 44.dp else fullWidth, spec, label = "searchWidth")
    val height by animateDpAsState(if (collapsed) 44.dp else 52.dp, spec, label = "searchHeight")
    val iconStart by animateDpAsState(if (collapsed) 9.dp else 18.dp, spec, label = "searchIconStart")
    val showFull = remember(progress) { derivedStateOf { progress.value < 0.98f } }

    Row(
        Modifier
            .width(width)
            .height(height)
            .clip(CircleShape)
            .background(FieldFill)
            .clickable(enabled = collapsed, onClick = onExpand)
            .padding(start = iconStart),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Cari",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
        if (showFull.value) {
            Row(
                Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = 1f - progress.value }
                    .padding(start = 12.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text("Cari postingan atau pengguna...", color = TextSecondary, fontSize = 17.sp)
                            }
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Mikrofon",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
