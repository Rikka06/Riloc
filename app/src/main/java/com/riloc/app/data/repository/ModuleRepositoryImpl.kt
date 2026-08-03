package com.riloc.app.data.repository

import com.riloc.app.data.model.Module
import com.riloc.app.data.model.ModuleUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModuleRepositoryImpl : ModuleRepository {
    override suspend fun getModules(): Result<List<Module>> = withContext(Dispatchers.IO) {
        Result.success(mockModules)
    }

    override suspend fun checkUpdate(module: Module): Result<ModuleUpdateInfo> = withContext(Dispatchers.IO) {
        Result.success(ModuleUpdateInfo.Empty)
    }

    private val mockModules = listOf(
        Module(
            id = "template.mount.layer",
            name = "Template Mount Layer",
            author = "SukiSU UI Template",
            version = "v0.0.1",
            versionCode = 1,
            description = "UI placeholder module. No files are mounted or changed.",
            enabled = true,
            update = false,
            remove = false,
            updateJson = "",
            hasWebUi = true,
            hasActionScript = true,
            metamodule = false,
            actionIconPath = null,
            webUiIconPath = null,
        ),
        Module(
            id = "template.webui.panel",
            name = "Template WebUI Panel",
            author = "SukiSU UI Template",
            version = "v0.0.4",
            versionCode = 4,
            description = "Shows the original WebUI entry style without loading real module content.",
            enabled = true,
            update = false,
            remove = false,
            updateJson = "",
            hasWebUi = true,
            hasActionScript = false,
            metamodule = false,
            actionIconPath = null,
            webUiIconPath = null,
        ),
        Module(
            id = "template.policy.pack",
            name = "Template Policy Pack",
            author = "SukiSU UI Template",
            version = "v0.0.2",
            versionCode = 2,
            description = "Demonstrates disabled module state with redacted policy data.",
            enabled = false,
            update = false,
            remove = false,
            updateJson = "",
            hasWebUi = false,
            hasActionScript = false,
            metamodule = false,
            actionIconPath = null,
            webUiIconPath = null,
        ),
    )
}

