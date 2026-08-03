package com.riloc.app.data.repository

import com.riloc.app.data.model.Author
import com.riloc.app.data.model.ReleaseAsset
import com.riloc.app.data.model.RepoModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModuleRepoRepositoryImpl : ModuleRepoRepository {
    override suspend fun fetchModules(): Result<List<RepoModule>> = withContext(Dispatchers.IO) {
        Result.success(
            listOf(
                repoModule(
                    moduleId = "template-busybox-card",
                    moduleName = "Template Busybox Card",
                    summary = "Repository item preview. Download URL is a redacted placeholder.",
                    stars = 1280,
                    release = "v0.0.1",
                ),
                repoModule(
                    moduleId = "template-overlay-card",
                    moduleName = "Template Overlay Card",
                    summary = "Demonstrates sorting, tags and release detail sections.",
                    stars = 842,
                    release = "v0.0.2",
                ),
                repoModule(
                    moduleId = "template-action-card",
                    moduleName = "Template Action Card",
                    summary = "Shows module action affordances without installing anything.",
                    stars = 315,
                    release = "v0.0.3",
                ),
            )
        )
    }

    private fun repoModule(
        moduleId: String,
        moduleName: String,
        summary: String,
        stars: Int,
        release: String,
    ): RepoModule = RepoModule(
        moduleId = moduleId,
        moduleName = moduleName,
        authors = "SukiSU UI Template",
        authorList = listOf(Author("SukiSU UI Template", "https://example.invalid/redacted")),
        summary = summary,
        metamodule = false,
        stargazerCount = stars,
        updatedAt = "2026-06-01T00:00:00Z",
        createdAt = "2026-05-01T00:00:00Z",
        latestRelease = release,
        latestReleaseTime = "2026-06-01T00:00:00Z",
        latestVersionCode = 1,
        latestAsset = ReleaseAsset(
            name = "$moduleId.zip",
            downloadUrl = "https://example.invalid/$moduleId.zip",
            size = 0L,
        ),
    )
}

