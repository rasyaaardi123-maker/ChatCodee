package com.chatcode.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chatcode.ui.chat.WorkspaceDummyScreen
import com.chatcode.ui.screens.ChatDetailScreen
import com.chatcode.ui.screens.CreatePostScreen
import com.chatcode.ui.screens.CreateStoryScreen
import com.chatcode.ui.screens.ProjectDetailScreen
import com.chatcode.ui.screens.UserProfileScreen

object Routes {
    const val MAIN = "main"
    const val CHAT_DETAIL = "chat/{id}"
    const val PROJECT_DETAIL = "project/{id}"
    const val NEW_POST = "new_post"
    const val NEW_STORY = "new_story"
    const val WORKSPACE = "workspace"
    const val USER_PROFILE = "user/{userId}"

    fun chatDetail(id: Long): String = "chat/" + id
    fun projectDetail(id: Long): String = "project/" + id
    fun userProfile(userId: String): String = "user/" + userId
}

private const val NavMs = 260

@Composable
fun AppNav(vm: MainViewModel, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backToMain = { nav.popBackStack(Routes.MAIN, false); Unit }
    val openUser: (String) -> Unit = { userId ->
        if (userId.isNotBlank()) nav.navigate(Routes.userProfile(userId))
    }

    NavHost(
        navController = nav,
        startDestination = Routes.MAIN,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(NavMs, easing = FastOutSlowInEasing)
            ) { fullWidth -> fullWidth / 6 } +
                fadeIn(animationSpec = tween(NavMs, easing = FastOutSlowInEasing))
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(NavMs, easing = FastOutSlowInEasing)
            ) { fullWidth -> -fullWidth / 8 } +
                fadeOut(animationSpec = tween(NavMs, easing = FastOutSlowInEasing))
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(NavMs, easing = FastOutSlowInEasing)
            ) { fullWidth -> -fullWidth / 8 } +
                fadeIn(animationSpec = tween(NavMs, easing = FastOutSlowInEasing))
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(NavMs, easing = FastOutSlowInEasing)
            ) { fullWidth -> fullWidth / 6 } +
                fadeOut(animationSpec = tween(NavMs, easing = FastOutSlowInEasing))
        }
    ) {
        composable(Routes.MAIN) {
            MainPager(
                vm = vm,
                onOpenChat = { id -> nav.navigate(Routes.chatDetail(id)) },
                onOpenProject = { id -> nav.navigate(Routes.projectDetail(id)) },
                onOpenUser = openUser,
                onNewPost = { nav.navigate(Routes.NEW_POST) },
                onNewStory = { nav.navigate(Routes.NEW_STORY) },
                onLogout = onLogout
            )
        }
        composable(
            route = Routes.CHAT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            ChatDetailScreen(
                vm = vm,
                chatId = id,
                onBack = { nav.popBackStack() },
                onWorkspace = { nav.navigate(Routes.WORKSPACE) }
            )
        }
        composable(Routes.WORKSPACE) {
            WorkspaceDummyScreen { nav.popBackStack() }
        }
        composable(
            route = Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            ProjectDetailScreen(vm, id) { nav.popBackStack() }
        }
        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { entry ->
            val userId = entry.arguments?.getString("userId") ?: ""
            UserProfileScreen(
                vm = vm,
                userId = userId,
                onBack = { nav.popBackStack() },
                onOpenUser = openUser
            )
        }
        composable(Routes.NEW_POST) {
            CreatePostScreen(vm, onDone = backToMain, onBack = { nav.popBackStack() })
        }
        composable(Routes.NEW_STORY) {
            CreateStoryScreen(vm, onDone = backToMain, onBack = { nav.popBackStack() })
        }
    }
}
