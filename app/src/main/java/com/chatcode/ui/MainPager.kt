package com.chatcode.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.chatcode.ui.components.FloatingBottomBar
import com.chatcode.ui.popup.LocalPopups
import com.chatcode.ui.screens.AccountScreen
import com.chatcode.ui.screens.ChatListScreen
import com.chatcode.ui.screens.CreateSheetContent
import com.chatcode.ui.screens.HomeScreen
import com.chatcode.ui.screens.ProjectsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPager(
    vm: MainViewModel,
    onOpenChat: (Long) -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenUser: (String) -> Unit,
    onNewPost: () -> Unit,
    onNewStory: () -> Unit,
    onLogout: () -> Unit
) {
    val pagerState = rememberPagerState { 4 }
    val scope = rememberCoroutineScope()
    val popups = LocalPopups.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> page }
        ) { page ->
            when (page) {
                0 -> HomeScreen(vm, onOpenUser = onOpenUser)
                1 -> ChatListScreen(vm, onOpenChat)
                2 -> ProjectsScreen(vm, onOpenProject)
                else -> AccountScreen(vm, onLogout = onLogout, onOpenUser = onOpenUser)
            }
        }

        FloatingBottomBar(
            pagePosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
            onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
            onPlus = {
                popups.showSheet { dismiss ->
                    CreateSheetContent(
                        onPost = {
                            dismiss()
                            onNewPost()
                        },
                        onStory = {
                            dismiss()
                            onNewStory()
                        }
                    )
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
