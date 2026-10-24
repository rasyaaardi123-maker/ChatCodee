package com.chatcode.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import com.chatcode.data.Post
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.popup.DialogSpec
import com.chatcode.ui.popup.LocalPopups
import com.chatcode.ui.popup.MenuItemSpec

@Stable
class PostActions(
    val like: (Post) -> Unit,
    val comments: (Post) -> Unit,
    val menu: (Post, Offset) -> Unit,
    val author: (Post) -> Unit
)

@Composable
fun rememberPostActions(
    vm: MainViewModel,
    onOpenUser: (String) -> Unit,
    onLike: (Post) -> Unit,
    onDelete: (Post) -> Unit
): PostActions {
    val popups = LocalPopups.current
    val likeState = rememberUpdatedState(onLike)
    val deleteState = rememberUpdatedState(onDelete)
    val openUserState = rememberUpdatedState(onOpenUser)

    return remember(vm, popups) {
        val openComments: (Post) -> Unit = { post ->
            popups.showSheet { _ ->
                CommentsSheetContent(vm, post.id)
            }
        }

        PostActions(
            like = { post -> likeState.value(post) },
            comments = openComments,
            menu = { post, anchor ->
                val items = mutableListOf<MenuItemSpec>()

                if (!post.mine && post.authorId.isNotBlank()) {
                    items.add(
                        MenuItemSpec("Lihat profil") {
                            openUserState.value(post.authorId)
                        }
                    )
                }

                items.add(MenuItemSpec("Lihat komentar") {
                    openComments(post)
                })

                if (post.mine) {
                    items.add(
                        MenuItemSpec("Hapus postingan", destructive = true) {
                            popups.showDialog(
                                DialogSpec(
                                    title = "Hapus postingan?",
                                    message = "Postingan yang dihapus tidak bisa dikembalikan.",
                                    confirmLabel = "Hapus",
                                    destructive = true,
                                    onConfirm = {
                                        deleteState.value(post)
                                    }
                                )
                            )
                        }
                    )
                }

                popups.showMenu(anchor, items)
            },
            author = { post ->
                if (post.authorId.isNotBlank()) {
                    openUserState.value(post.authorId)
                }
            }
        )
    }
}
