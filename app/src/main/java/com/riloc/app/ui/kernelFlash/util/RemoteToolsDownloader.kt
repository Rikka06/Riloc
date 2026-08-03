package com.riloc.app.ui.kernelFlash.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RemoteToolsDownloader(
    private val context: Context,
    private val workDir: String
) {
    interface DownloadProgressListener {
        fun onProgress(fileName: String, progress: Int, total: Int)
        fun onLog(message: String)
        fun onError(fileName: String, error: String)
        fun onSuccess(fileName: String, isRemote: Boolean)
    }

    data class DownloadResult(
        val success: Boolean,
        val isRemoteSource: Boolean,
        val errorMessage: String? = null
    )

    suspend fun downloadToolsAsync(listener: DownloadProgressListener?): Map<String, DownloadResult> =
        withContext(Dispatchers.IO) {
            File(workDir).mkdirs()
            listener?.onLog("UI template: KPM tool download disabled; using placeholder files.")
            listOf("kptools", "kpimg").associateWith { fileName ->
                val target = File(workDir, fileName)
                target.writeText("UI template placeholder for $fileName\n")
                listener?.onProgress(fileName, 1, 1)
                listener?.onSuccess(fileName, false)
                DownloadResult(success = true, isRemoteSource = false)
            }
        }

    fun cleanup() {
        runCatching {
            File(workDir).listFiles()
                ?.filter { it.name.endsWith(".tmp") }
                ?.forEach { it.delete() }
        }
    }
}

