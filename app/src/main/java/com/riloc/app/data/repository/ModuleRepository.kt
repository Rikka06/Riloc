package com.riloc.app.data.repository

import com.riloc.app.data.model.Module
import com.riloc.app.data.model.ModuleUpdateInfo

interface ModuleRepository {
    suspend fun getModules(): Result<List<Module>>
    suspend fun checkUpdate(module: Module): Result<ModuleUpdateInfo>
}

