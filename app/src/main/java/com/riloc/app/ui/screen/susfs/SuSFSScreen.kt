package com.riloc.app.ui.screen.susfs

import androidx.compose.runtime.Composable
import com.riloc.app.ui.LocalUiMode
import com.riloc.app.ui.UiMode

@Composable
fun SuSFSScreen() {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SuSFSMiuix()
        UiMode.Material -> SuSFSMaterial()
    }
}

