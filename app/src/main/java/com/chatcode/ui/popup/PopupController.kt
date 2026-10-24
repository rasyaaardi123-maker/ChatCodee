package com.chatcode.ui.popup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import java.time.LocalDate

class DialogSpec(
    val title: String,
    val message: String,
    val confirmLabel: String = "Ya",
    val dismissLabel: String? = "Batal",
    val destructive: Boolean = false,
    val onConfirm: () -> Unit
)

class MenuItemSpec(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

class MenuSpec(
    val anchor: Offset,
    val items: List<MenuItemSpec>
)

class SheetSpec(
    val fullScreen: Boolean,
    val content: @Composable (dismiss: () -> Unit) -> Unit
)

class DatePickSpec(
    val initial: LocalDate?,
    val onPicked: (LocalDate) -> Unit
)

class ToastSpec(
    val id: Long,
    val message: String
)

@Stable
class PopupController {

    var dialog by mutableStateOf<DialogSpec?>(null)
        private set
    var sheet by mutableStateOf<SheetSpec?>(null)
        private set
    var picker by mutableStateOf<SheetSpec?>(null)
        private set
    var menu by mutableStateOf<MenuSpec?>(null)
        private set
    var datePicker by mutableStateOf<DatePickSpec?>(null)
        private set
    var toast by mutableStateOf<ToastSpec?>(null)
        private set

    private var toastCounter = 0L

    val hasModal: Boolean
        get() = dialog != null || sheet != null || picker != null ||
            menu != null || datePicker != null

    fun showToast(message: String) {
        toastCounter += 1
        toast = ToastSpec(toastCounter, message)
    }

    fun clearToast(id: Long) {
        if (toast?.id == id) toast = null
    }

    fun showDialog(spec: DialogSpec) {
        dialog = spec
    }

    fun dismissDialog() {
        dialog = null
    }

    fun showSheet(
        fullScreen: Boolean = false,
        content: @Composable (dismiss: () -> Unit) -> Unit
    ) {
        sheet = SheetSpec(fullScreen, content)
    }

    fun dismissSheet() {
        sheet = null
    }

    fun showMenu(anchor: Offset, items: List<MenuItemSpec>) {
        menu = MenuSpec(anchor, items)
    }

    fun dismissMenu() {
        menu = null
    }

    fun pickDate(initial: LocalDate?, onPicked: (LocalDate) -> Unit) {
        datePicker = DatePickSpec(initial, onPicked)
    }

    fun dismissDatePicker() {
        datePicker = null
    }

    fun pickOption(
        title: String,
        options: List<String>,
        selected: String?,
        onPicked: (String) -> Unit
    ) {
        picker = SheetSpec(false) { dismiss ->
            OptionList(title, options, selected) { value ->
                dismiss()
                onPicked(value)
            }
        }
    }

    fun dismissPicker() {
        picker = null
    }

    fun dismissTop() {
        when {
            datePicker != null -> dismissDatePicker()
            dialog != null -> dismissDialog()
            menu != null -> dismissMenu()
            picker != null -> dismissPicker()
            sheet != null -> dismissSheet()
        }
    }
}

val LocalPopups = staticCompositionLocalOf<PopupController> {
    error("PopupController belum disediakan")
}
