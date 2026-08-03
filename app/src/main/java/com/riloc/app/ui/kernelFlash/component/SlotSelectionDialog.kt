package com.riloc.app.ui.kernelFlash.component

import androidx.compose.runtime.Composable
import com.riloc.app.ui.LocalUiMode
import com.riloc.app.ui.UiMode

@Composable
fun SlotSelectionDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onSlotSelected: (String) -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SlotSelectionDialogMiuix(
            show = show,
            onDismiss = onDismiss,
            onSlotSelected = onSlotSelected
        )
        UiMode.Material -> SlotSelectionDialogMaterial(
            show = show,
            onDismiss = onDismiss,
            onSlotSelected = onSlotSelected
        )
    }
}

