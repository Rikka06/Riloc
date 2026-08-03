package com.riloc.app.ui.screen.kpm

import androidx.compose.foundation.lazy.LazyListState
import java.io.File
import java.io.FileInputStream

fun extractModuleName(file: File): String? {
    return file.nameWithoutExtension.ifBlank { "template_kpm_module" }
}

fun isValidKpmFile(file: File, mimeType: String?): Boolean {
    val isCorrectMimeType = mimeType == null || mimeType.contains("application/octet-stream")
    if (isCorrectMimeType) return true

    return file.exists() || isElfFile(file)
}

fun checkStringsCommand(tempFile: File): Int {
    return if (tempFile.exists()) 1 else 0
}

fun isElfFile(tempFile: File): Boolean {
    val elfMagic = byteArrayOf(0x7F, 0x45, 0x4C, 0x46) // "\u007FELF"
    return try {
        FileInputStream(tempFile).use { input ->
            val bytes = ByteArray(4)
            input.read(bytes) == 4 && bytes.contentEquals(elfMagic)
        }
    } catch (_: Exception) {
        false
    }
}

fun isScrolledToEnd(listState: LazyListState): Boolean {
    val layoutInfo = listState.layoutInfo
    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return false
    return lastItem.index == layoutInfo.totalItemsCount - 1 &&
            lastItem.size < layoutInfo.viewportEndOffset
}

