package com.riloc.app.ui.util.module

data class ModuleDetail(
    val readme: String,
    val readmeHtml: String,
    val latestTag: String,
    val latestTime: String,
    val latestAssetName: String?,
    val latestAssetUrl: String?,
    val releases: List<ReleaseInfo>,
    val homepageUrl: String,
    val sourceUrl: String,
    val url: String
)

data class ReleaseInfo(
    val name: String,
    val tagName: String,
    val publishedAt: String,
    val descriptionHTML: String,
    val assets: List<ReleaseAssetInfo>
)

data class ReleaseAssetInfo(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val downloadCount: Int
)

fun sanitizeVersionString(version: String): String {
    return version.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
}

fun stripTicks(s: String): String {
    val t = s.trim()
    return if (t.startsWith("`") && t.endsWith("`") && t.length >= 2) t.substring(1, t.length - 1) else t
}

fun fetchReleaseDescriptionHtml(moduleId: String, latestTag: String): String? =
    "<p>UI template changelog for <b>$moduleId</b> / $latestTag. No network request was made.</p>"

fun fetchModuleDetail(moduleId: String): ModuleDetail {
    val assetName = "$moduleId-template.zip"
    val assetUrl = "https://example.invalid/$assetName"
    return ModuleDetail(
        readme = """
            # $moduleId

            This is a redacted UI-template module detail page.

            - No download is performed.
            - No module is installed.
            - All package names, URLs and release metadata are placeholders.
        """.trimIndent(),
        readmeHtml = """
            <h1>$moduleId</h1>
            <p>This is a redacted UI-template module detail page.</p>
            <ul>
              <li>No download is performed.</li>
              <li>No module is installed.</li>
              <li>All package names, URLs and release metadata are placeholders.</li>
            </ul>
        """.trimIndent(),
        latestTag = "v0.0.1-template",
        latestTime = "2026-06-01T00:00:00Z",
        latestAssetName = assetName,
        latestAssetUrl = assetUrl,
        releases = listOf(
            ReleaseInfo(
                name = "v0.0.1-template",
                tagName = "v0.0.1-template",
                publishedAt = "2026-06-01T00:00:00Z",
                descriptionHTML = "<p>Initial UI-template release. No binary payload.</p>",
                assets = listOf(
                    ReleaseAssetInfo(
                        name = assetName,
                        downloadUrl = assetUrl,
                        size = 0L,
                        downloadCount = 0,
                    )
                ),
            )
        ),
        homepageUrl = "https://example.invalid/homepage",
        sourceUrl = "https://example.invalid/source",
        url = "https://example.invalid/module/$moduleId",
    )
}

