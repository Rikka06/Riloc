package com.riloc.app.ui.screen.susfs.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SuSFSModuleManager {
    data class CommandResult(val isSuccess: Boolean, val output: String, val errorOutput: String = "")

    suspend fun createMagiskModule(context: Context): Boolean = withContext(Dispatchers.IO) { true }

    suspend fun removeMagiskModule(): Boolean = withContext(Dispatchers.IO) { true }

    suspend fun updateMagiskModule(context: Context): Boolean = withContext(Dispatchers.IO) { true }
}

