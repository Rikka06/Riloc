package com.riloc.app.ui.screen.susfs.component

import androidx.compose.runtime.Composable
import com.riloc.app.ui.LocalUiMode
import com.riloc.app.ui.UiMode
import com.riloc.app.ui.screen.susfs.component.miuix.AddPathDialogMiuix
import com.riloc.app.ui.screen.susfs.component.material.AddPathDialogMaterial

@Composable
fun AddPathDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean,
    titleRes: Int,
    labelRes: Int,
    initialValue: String = ""
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> AddPathDialogMiuix(
            showDialog = showDialog,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            isLoading = isLoading,
            titleRes = titleRes,
            labelRes = labelRes,
            initialValue = initialValue
        )
        UiMode.Material -> AddPathDialogMaterial(
            showDialog = showDialog,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            isLoading = isLoading,
            titleRes = titleRes,
            labelRes = labelRes,
            initialValue = initialValue
        )
    }
}

