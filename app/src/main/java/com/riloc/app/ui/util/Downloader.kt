package com.riloc.app.ui.util

import android.net.Uri
import com.riloc.app.ui.util.module.LatestVersionInfo

suspend fun download(
    url: String,
    fileName: String,
    onDownloaded: (Uri) -> Unit = {},
    onDownloading: () -> Unit = {},
    onProgress: (Int) -> Unit = {}
) {
    onDownloading()
    onProgress(100)
    onDownloaded(Uri.parse("content://com.riloc.app.template/mock/$fileName"))
}

fun checkNewVersion(): LatestVersionInfo = LatestVersionInfo()

