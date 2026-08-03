package com.riloc.app.data.repository

import com.riloc.app.ui.viewmodel.KpmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KpmRepositoryImpl : KpmRepository {
    override suspend fun getModuleList(): Result<List<KpmViewModel.ModuleInfo>> = withContext(Dispatchers.IO) {
        Result.success(
            listOf(
                KpmViewModel.ModuleInfo(
                    id = "template_kpm_guard",
                    name = "Template KPM Guard",
                    version = "v0.0.1",
                    author = "SukiSU UI Template",
                    description = "KPM placeholder. No kernel module is loaded.",
                    args = "--template",
                    enabled = true,
                    hasAction = true,
                ),
                KpmViewModel.ModuleInfo(
                    id = "template_kpm_panel",
                    name = "Template KPM Panel",
                    version = "v0.0.2",
                    author = "SukiSU UI Template",
                    description = "Demonstrates the original KPM card layout with mock data.",
                    args = "",
                    enabled = true,
                    hasAction = false,
                ),
            )
        )
    }

    override suspend fun getModuleInfo(moduleId: String): Result<String> = withContext(Dispatchers.IO) {
        Result.success(
            """
            name=$moduleId
            version=v0.0.1
            author=SukiSU UI Template
            description=KPM detail preview only.
            """.trimIndent()
        )
    }

    override suspend fun loadModule(path: String, args: String?): Result<Unit> = Result.success(Unit)

    override suspend fun unloadModule(moduleId: String): Result<Unit> = Result.success(Unit)

    override suspend fun controlModule(moduleId: String, args: String?): Result<Int> = Result.success(0)
}

