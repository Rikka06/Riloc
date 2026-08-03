package com.riloc.app.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.riloc.app.Natives
import com.riloc.app.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SuperUserRepositoryImpl : SuperUserRepository {
    override suspend fun getAppList(): Result<Pair<List<AppInfo>, List<Int>>> = withContext(Dispatchers.IO) {
        Result.success(mockApps() to listOf(0, 10))
    }

    override suspend fun refreshProfiles(currentApps: List<AppInfo>): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        Result.success(
            currentApps.map {
                it.copy(profile = Natives.getAppProfile(it.packageName, it.uid))
            }
        )
    }

    private fun mockApps(): List<AppInfo> = listOf(
        mockApp(
            label = "Template Terminal",
            packageName = "app.example.terminal",
            uid = 10001,
            allowSu = true,
            rootUseDefault = true,
        ),
        mockApp(
            label = "Template Toolbox",
            packageName = "app.example.toolbox",
            uid = 10002,
            allowSu = true,
            rootUseDefault = false,
        ),
        mockApp(
            label = "Template Browser",
            packageName = "app.example.browser",
            uid = 10003,
            allowSu = false,
            nonRootUseDefault = false,
        ),
        mockApp(
            label = "Template Backup",
            packageName = "app.example.backup",
            uid = 10004,
            allowSu = true,
            rootUseDefault = false,
        ),
        mockApp(
            label = "Template Wallet",
            packageName = "app.example.wallet",
            uid = 110005,
            allowSu = false,
            nonRootUseDefault = true,
        ),
    )

    private fun mockApp(
        label: String,
        packageName: String,
        uid: Int,
        allowSu: Boolean,
        rootUseDefault: Boolean = true,
        nonRootUseDefault: Boolean = true,
    ): AppInfo {
        val now = System.currentTimeMillis()
        val app = ApplicationInfo().apply {
            this.packageName = packageName
            this.uid = uid
            this.nonLocalizedLabel = label
            this.sourceDir = "/template/redacted/$packageName/base.apk"
            this.publicSourceDir = sourceDir
        }
        val pkg = PackageInfo().apply {
            this.packageName = packageName
            this.applicationInfo = app
            this.firstInstallTime = now - uid.toLong() * 17L
            this.lastUpdateTime = now - uid.toLong() * 7L
        }
        val profile = Natives.Profile(
            name = packageName,
            currentUid = uid,
            allowSu = allowSu,
            rootUseDefault = rootUseDefault,
            nonRootUseDefault = nonRootUseDefault,
            umountModules = uid % 2 == 0,
        )
        Natives.setAppProfile(profile)
        return AppInfo(label = label, packageInfo = pkg, profile = profile)
    }
}

