package com.riloc.app.ui.component.uninstalldialog

import androidx.compose.runtime.Composable
import com.riloc.app.ui.LocalUiMode
import com.riloc.app.ui.UiMode

@Composable
fun UninstallDialog(
    show: Boolean,
    onDismissRequest: () -> Unit
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> UninstallDialogMiuix(show, onDismissRequest)
        UiMode.Material -> UninstallDialogMaterial(show, onDismissRequest)
    }
}

