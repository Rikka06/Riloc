package com.riloc.app.ui.screen.settings.tools

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun isSelinuxPermissive(): Boolean = false

fun setSelinuxPermissive(permissive: Boolean): Boolean = true

suspend fun backupAllowlistToUri(context: Context, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openOutputStream(targetUri, "w")?.use { output ->
            output.write("SukiSU UI Template allowlist backup placeholder\n".toByteArray())
        }
        true
    }.getOrDefault(false)
}

suspend fun restoreAllowlistFromUri(context: Context, sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(sourceUri)?.close()
        true
    }.getOrDefault(false)
}

