package com.riloc.app.ui.screen.susfs.util

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import androidx.core.content.edit
import com.riloc.app.R
import com.riloc.app.ui.viewmodel.SuperUserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SuSFSManager {
    private const val PREFS_NAME = "susfs_config_template"
    private const val KEY_UNAME_VALUE = "uname_value"
    private const val KEY_BUILD_TIME_VALUE = "build_time_value"
    private const val KEY_AUTO_START_ENABLED = "auto_start_enabled"
    private const val KEY_SUS_PATHS = "sus_paths"
    private const val KEY_SUS_LOOP_PATHS = "sus_loop_paths"
    private const val KEY_SUS_MAPS = "sus_maps"
    private const val KEY_ENABLE_LOG = "enable_log"
    private const val KEY_EXECUTE_IN_POST_FS_DATA = "execute_in_post_fs_data"
    private const val KEY_KSTAT_CONFIGS = "kstat_configs"
    private const val KEY_ADD_KSTAT_PATHS = "add_kstat_paths"
    private const val KEY_HIDE_SUS_MOUNTS_FOR_ALL_PROCS = "hide_sus_mounts_for_all_procs"
    private const val KEY_ENABLE_CLEANUP_RESIDUE = "enable_cleanup_residue"
    private const val KEY_ENABLE_HIDE_BL = "enable_hide_bl"
    private const val KEY_ENABLE_AVC_LOG_SPOOFING = "enable_avc_log_spoofing"

    private const val DEFAULT_UNAME = "default"
    private const val DEFAULT_BUILD_TIME = "default"
    const val MAX_SUSFS_VERSION = "2.1.0"
    private const val SUSFS_BINARY_TARGET_NAME = "ksu_susfs"

    private val defaultSusPaths = setOf(
        "/sdcard/Android/data/com.example.app",
        "/data/user/0/com.example.wallet"
    )
    private val defaultLoopPaths = setOf("/sdcard/Android/obb/com.example.game")
    private val defaultSusMaps = setOf("/proc/self/maps")
    private val defaultKstatConfigs = setOf("/system/bin/app_process64|1000|0:0|1|4096|0|0|0|0|0|0|8|4096")
    private val defaultKstatPaths = setOf("/system/bin/linker64")

    data class SlotInfo(val slotName: String, val uname: String, val buildTime: String)

    data class EnabledFeature(
        val name: String,
        val isEnabled: Boolean,
        val statusText: String,
        val canConfigure: Boolean = false
    ) {
        companion object {
            fun create(context: Context, name: String, isEnabled: Boolean): EnabledFeature {
                val statusText = if (isEnabled) {
                    context.getString(R.string.susfs_feature_enabled)
                } else {
                    context.getString(R.string.susfs_feature_disabled)
                }
                return EnabledFeature(name, isEnabled, statusText, false)
            }
        }
    }

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val packageInfo: PackageInfo,
        val isSystemApp: Boolean
    )

    data class BackupData(
        val version: String,
        val timestamp: Long,
        val deviceInfo: String,
        val configurations: Map<String, Any>
    ) {
        fun toJson(): String {
            val jsonObject = JSONObject().apply {
                put("version", version)
                put("timestamp", timestamp)
                put("deviceInfo", deviceInfo)
                put("configurations", JSONObject(configurations))
            }
            return jsonObject.toString(2)
        }

        companion object {
            fun fromJson(jsonString: String): BackupData? {
                return try {
                    val jsonObject = JSONObject(jsonString)
                    val configurationsJson = jsonObject.getJSONObject("configurations")
                    val configurations = mutableMapOf<String, Any>()

                    configurationsJson.keys().forEach { key ->
                        configurations[key] = when (val value = configurationsJson.get(key)) {
                            is JSONArray -> buildSet {
                                for (i in 0 until value.length()) add(value.getString(i))
                            }
                            else -> value
                        }
                    }

                    BackupData(
                        version = jsonObject.getString("version"),
                        timestamp = jsonObject.getLong("timestamp"),
                        deviceInfo = jsonObject.getString("deviceInfo"),
                        configurations = configurations
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    data class ModuleConfig(
        val targetPath: String,
        val unameValue: String,
        val buildTimeValue: String,
        val executeInPostFsData: Boolean,
        val susPaths: Set<String>,
        val susLoopPaths: Set<String>,
        val susMaps: Set<String>,
        val enableLog: Boolean,
        val kstatConfigs: Set<String>,
        val addKstatPaths: Set<String>,
        val hideSusMountsForAllProcs: Boolean,
        val enableHideBl: Boolean,
        val enableCleanupResidue: Boolean,
        val enableAvcLogSpoofing: Boolean
    ) {
        fun hasAutoStartConfig(): Boolean {
            return unameValue != DEFAULT_UNAME ||
                    buildTimeValue != DEFAULT_BUILD_TIME ||
                    susPaths.isNotEmpty() ||
                    susLoopPaths.isNotEmpty() ||
                    susMaps.isNotEmpty() ||
                    kstatConfigs.isNotEmpty() ||
                    addKstatPaths.isNotEmpty()
        }
    }

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSuSFSTargetPath(): String = "/data/adb/ksu/bin/$SUSFS_BINARY_TARGET_NAME"

    suspend fun copyBinaryFromAssets(context: Context): String? = withContext(Dispatchers.IO) {
        runCatching {
            File(context.cacheDir, "${SUSFS_BINARY_TARGET_NAME}_template").apply {
                writeText("UI template placeholder\n")
            }.absolutePath
        }.getOrNull()
    }

    fun getCurrentModuleConfig(context: Context): ModuleConfig {
        return ModuleConfig(
            targetPath = getSuSFSTargetPath(),
            unameValue = getUnameValue(context),
            buildTimeValue = getBuildTimeValue(context),
            executeInPostFsData = getExecuteInPostFsData(context),
            susPaths = getSusPaths(context),
            susLoopPaths = getSusLoopPaths(context),
            susMaps = getSusMaps(context),
            enableLog = getEnableLogState(context),
            kstatConfigs = getKstatConfigs(context),
            addKstatPaths = getAddKstatPaths(context),
            hideSusMountsForAllProcs = getHideSusMountsForAllProcs(context),
            enableHideBl = getEnableHideBl(context),
            enableCleanupResidue = getEnableCleanupResidue(context),
            enableAvcLogSpoofing = getEnableAvcLogSpoofing(context)
        )
    }

    fun saveUnameValue(context: Context, value: String) =
        getPrefs(context).edit { putString(KEY_UNAME_VALUE, value) }

    fun getUnameValue(context: Context): String =
        getPrefs(context).getString(KEY_UNAME_VALUE, "5.15.148-template-gki") ?: DEFAULT_UNAME

    fun saveBuildTimeValue(context: Context, value: String) =
        getPrefs(context).edit { putString(KEY_BUILD_TIME_VALUE, value) }

    fun getBuildTimeValue(context: Context): String =
        getPrefs(context).getString(KEY_BUILD_TIME_VALUE, "Mon Jan 01 00:00:00 UTC 2026") ?: DEFAULT_BUILD_TIME

    fun getKernelSpoofRelease(context: Context): String = getUnameValue(context)

    fun getKernelSpoofVersion(context: Context): String = getBuildTimeValue(context)

    fun setAutoStartEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit { putBoolean(KEY_AUTO_START_ENABLED, enabled) }

    fun isAutoStartEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_AUTO_START_ENABLED, false)

    fun saveEnableLogState(context: Context, enabled: Boolean) =
        getPrefs(context).edit { putBoolean(KEY_ENABLE_LOG, enabled) }

    fun getEnableLogState(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ENABLE_LOG, true)

    fun getExecuteInPostFsData(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_EXECUTE_IN_POST_FS_DATA, false)

    fun saveExecuteInPostFsData(context: Context, enabled: Boolean) =
        getPrefs(context).edit { putBoolean(KEY_EXECUTE_IN_POST_FS_DATA, enabled) }

    fun saveHideSusMountsForAllProcs(context: Context, hideForAll: Boolean) =
        getPrefs(context).edit { putBoolean(KEY_HIDE_SUS_MOUNTS_FOR_ALL_PROCS, hideForAll) }

    fun getHideSusMountsForAllProcs(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_HIDE_SUS_MOUNTS_FOR_ALL_PROCS, true)

    fun saveEnableHideBl(context: Context, enabled: Boolean) =
        getPrefs(context).edit { putBoolean(KEY_ENABLE_HIDE_BL, enabled) }

    fun getEnableHideBl(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ENABLE_HIDE_BL, true)

    fun saveEnableCleanupResidue(context: Context, enabled: Boolean) =
        getPrefs(context).edit { putBoolean(KEY_ENABLE_CLEANUP_RESIDUE, enabled) }

    fun getEnableCleanupResidue(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ENABLE_CLEANUP_RESIDUE, true)

    fun saveEnableAvcLogSpoofing(context: Context, enabled: Boolean) =
        getPrefs(context).edit { putBoolean(KEY_ENABLE_AVC_LOG_SPOOFING, enabled) }

    fun getEnableAvcLogSpoofing(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ENABLE_AVC_LOG_SPOOFING, true)

    fun saveSusPaths(context: Context, paths: Set<String>) =
        getPrefs(context).edit { putStringSet(KEY_SUS_PATHS, paths) }

    fun getSusPaths(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_SUS_PATHS, defaultSusPaths)?.toSet() ?: defaultSusPaths

    fun saveSusLoopPaths(context: Context, paths: Set<String>) =
        getPrefs(context).edit { putStringSet(KEY_SUS_LOOP_PATHS, paths) }

    fun getSusLoopPaths(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_SUS_LOOP_PATHS, defaultLoopPaths)?.toSet() ?: defaultLoopPaths

    fun saveSusMaps(context: Context, maps: Set<String>) =
        getPrefs(context).edit { putStringSet(KEY_SUS_MAPS, maps) }

    fun getSusMaps(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_SUS_MAPS, defaultSusMaps)?.toSet() ?: defaultSusMaps

    fun saveKstatConfigs(context: Context, configs: Set<String>) =
        getPrefs(context).edit { putStringSet(KEY_KSTAT_CONFIGS, configs) }

    fun getKstatConfigs(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_KSTAT_CONFIGS, defaultKstatConfigs)?.toSet() ?: defaultKstatConfigs

    fun saveAddKstatPaths(context: Context, paths: Set<String>) =
        getPrefs(context).edit { putStringSet(KEY_ADD_KSTAT_PATHS, paths) }

    fun getAddKstatPaths(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_ADD_KSTAT_PATHS, defaultKstatPaths)?.toSet() ?: defaultKstatPaths

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val fromSuperUser = SuperUserViewModel.getAppsSafely().map { app ->
            val flags = app.packageInfo.applicationInfo?.flags ?: 0
            AppInfo(
                packageName = app.packageName,
                appName = app.label,
                packageInfo = app.packageInfo,
                isSystemApp = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }
        fromSuperUser.ifEmpty {
            listOf(
                mockApp("app.example.terminal", "Example Terminal", false),
                mockApp("app.example.system", "Example System UI", true),
                mockApp("app.example.wallet", "Example Wallet", false)
            )
        }
    }

    private fun mockApp(packageName: String, appName: String, isSystem: Boolean): AppInfo {
        val appInfo = ApplicationInfo().apply {
            this.packageName = packageName
            flags = if (isSystem) ApplicationInfo.FLAG_SYSTEM else 0
            nonLocalizedLabel = appName
        }
        val packageInfo = PackageInfo().apply {
            this.packageName = packageName
            applicationInfo = appInfo
            versionName = "1.0-template"
        }
        return AppInfo(packageName, appName, packageInfo, isSystem)
    }

    suspend fun addAppPaths(context: Context, packageName: String): Boolean {
        val newPaths = getSusPaths(context) + setOf(
            "/data/user/0/$packageName",
            "/sdcard/Android/data/$packageName"
        )
        saveSusPaths(context, newPaths)
        return true
    }

    private fun getAllConfigurations(context: Context): Map<String, Any> {
        return mapOf(
            KEY_UNAME_VALUE to getUnameValue(context),
            KEY_BUILD_TIME_VALUE to getBuildTimeValue(context),
            KEY_EXECUTE_IN_POST_FS_DATA to getExecuteInPostFsData(context),
            KEY_SUS_PATHS to getSusPaths(context),
            KEY_SUS_LOOP_PATHS to getSusLoopPaths(context),
            KEY_SUS_MAPS to getSusMaps(context),
            KEY_ENABLE_LOG to getEnableLogState(context),
            KEY_KSTAT_CONFIGS to getKstatConfigs(context),
            KEY_ADD_KSTAT_PATHS to getAddKstatPaths(context),
            KEY_HIDE_SUS_MOUNTS_FOR_ALL_PROCS to getHideSusMountsForAllProcs(context),
            KEY_ENABLE_HIDE_BL to getEnableHideBl(context),
            KEY_ENABLE_CLEANUP_RESIDUE to getEnableCleanupResidue(context),
            KEY_ENABLE_AVC_LOG_SPOOFING to getEnableAvcLogSpoofing(context)
        )
    }

    private fun getDeviceInfo(): String = "Template Device (redacted)"

    suspend fun createBackup(context: Context, backupFilePath: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val backupData = BackupData(
                version = MAX_SUSFS_VERSION,
                timestamp = System.currentTimeMillis(),
                deviceInfo = getDeviceInfo(),
                configurations = getAllConfigurations(context)
            )
            File(backupFilePath).writeText(backupData.toJson())
        }.isSuccess
    }

    suspend fun restoreFromBackup(context: Context, backupFilePath: String): Boolean = withContext(Dispatchers.IO) {
        val backup = validateBackupFile(backupFilePath) ?: return@withContext false
        restoreConfigurations(context, backup.configurations)
        true
    }

    private fun restoreConfigurations(context: Context, configurations: Map<String, Any>) {
        getPrefs(context).edit {
            configurations.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
        }
    }

    suspend fun validateBackupFile(backupFilePath: String): BackupData? = withContext(Dispatchers.IO) {
        runCatching { BackupData.fromJson(File(backupFilePath).readText()) }.getOrNull()
    }

    fun getDefaultBackupFileName(): String {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "susfs_template_backup_$date.susfs_backup"
    }

    suspend fun getCurrentSlotInfo(): List<SlotInfo> = withContext(Dispatchers.IO) {
        listOf(
            SlotInfo("slot_a", "5.15.148-template-gki", "Mon Jan 01 00:00:00 UTC 2026"),
            SlotInfo("slot_b", "5.15.148-template-gki", "Mon Jan 01 00:00:00 UTC 2026")
        )
    }

    suspend fun getCurrentActiveSlot(): String = withContext(Dispatchers.IO) { "slot_a" }

    suspend fun getEnabledFeatures(context: Context): List<EnabledFeature> = withContext(Dispatchers.IO) {
        val enabled = context.getString(R.string.susfs_feature_enabled)
        listOf(
            context.getString(R.string.sus_path_feature_label),
            context.getString(R.string.sus_mount_feature_label),
            context.getString(R.string.spoof_uname_feature_label),
            context.getString(R.string.spoof_cmdline_feature_label),
            context.getString(R.string.open_redirect_feature_label),
            context.getString(R.string.enable_log_feature_label),
            context.getString(R.string.hide_symbols_feature_label),
            context.getString(R.string.sus_kstat_feature_label),
            context.getString(R.string.sus_map_feature_label)
        ).map { name ->
            EnabledFeature(
                name = name,
                isEnabled = true,
                statusText = enabled,
                canConfigure = name == context.getString(R.string.enable_log_feature_label)
            )
        }.sortedBy { it.name }
    }

    suspend fun setEnableLog(context: Context, enabled: Boolean): Boolean {
        saveEnableLogState(context, enabled)
        return true
    }

    suspend fun setEnableAvcLogSpoofing(context: Context, enabled: Boolean): Boolean {
        saveEnableAvcLogSpoofing(context, enabled)
        return true
    }

    suspend fun setHideSusMountsForAllProcs(context: Context, hideForAll: Boolean): Boolean {
        saveHideSusMountsForAllProcs(context, hideForAll)
        return true
    }

    suspend fun setUname(context: Context, unameValue: String, buildTimeValue: String): Boolean {
        saveUnameValue(context, unameValue.ifBlank { DEFAULT_UNAME })
        saveBuildTimeValue(context, buildTimeValue.ifBlank { DEFAULT_BUILD_TIME })
        return true
    }

    suspend fun addSusPath(context: Context, path: String): Boolean {
        if (path.isBlank()) return false
        saveSusPaths(context, getSusPaths(context) + path)
        return true
    }

    suspend fun removeSusPath(context: Context, path: String): Boolean {
        saveSusPaths(context, getSusPaths(context) - path)
        return true
    }

    suspend fun editSusPath(context: Context, oldPath: String, newPath: String): Boolean {
        if (newPath.isBlank()) return false
        saveSusPaths(context, (getSusPaths(context) - oldPath) + newPath)
        return true
    }

    suspend fun addSusLoopPath(context: Context, path: String): Boolean {
        if (path.isBlank()) return false
        saveSusLoopPaths(context, getSusLoopPaths(context) + path)
        return true
    }

    suspend fun removeSusLoopPath(context: Context, path: String): Boolean {
        saveSusLoopPaths(context, getSusLoopPaths(context) - path)
        return true
    }

    suspend fun editSusLoopPath(context: Context, oldPath: String, newPath: String): Boolean {
        if (newPath.isBlank()) return false
        saveSusLoopPaths(context, (getSusLoopPaths(context) - oldPath) + newPath)
        return true
    }

    suspend fun addSusMap(context: Context, map: String): Boolean {
        if (map.isBlank()) return false
        saveSusMaps(context, getSusMaps(context) + map)
        return true
    }

    suspend fun removeSusMap(context: Context, map: String): Boolean {
        saveSusMaps(context, getSusMaps(context) - map)
        return true
    }

    suspend fun editSusMap(context: Context, oldMap: String, newMap: String): Boolean {
        if (newMap.isBlank()) return false
        saveSusMaps(context, (getSusMaps(context) - oldMap) + newMap)
        return true
    }

    suspend fun addKstatStatically(
        context: Context,
        path: String,
        ino: String,
        dev: String,
        nlink: String,
        size: String,
        atime: String,
        atimeNsec: String,
        mtime: String,
        mtimeNsec: String,
        ctime: String,
        ctimeNsec: String,
        blocks: String,
        blksize: String
    ): Boolean {
        if (path.isBlank()) return false
        val config = listOf(path, ino, dev, nlink, size, atime, atimeNsec, mtime, mtimeNsec, ctime, ctimeNsec, blocks, blksize)
            .joinToString("|")
        saveKstatConfigs(context, getKstatConfigs(context) + config)
        return true
    }

    suspend fun removeKstatConfig(context: Context, config: String): Boolean {
        saveKstatConfigs(context, getKstatConfigs(context) - config)
        return true
    }

    suspend fun editKstatConfig(
        context: Context,
        oldConfig: String,
        path: String,
        ino: String,
        dev: String,
        nlink: String,
        size: String,
        atime: String,
        atimeNsec: String,
        mtime: String,
        mtimeNsec: String,
        ctime: String,
        ctimeNsec: String,
        blocks: String,
        blksize: String
    ): Boolean {
        removeKstatConfig(context, oldConfig)
        return addKstatStatically(context, path, ino, dev, nlink, size, atime, atimeNsec, mtime, mtimeNsec, ctime, ctimeNsec, blocks, blksize)
    }

    suspend fun addKstat(context: Context, path: String): Boolean {
        if (path.isBlank()) return false
        saveAddKstatPaths(context, getAddKstatPaths(context) + path)
        return true
    }

    suspend fun removeAddKstat(context: Context, path: String): Boolean {
        saveAddKstatPaths(context, getAddKstatPaths(context) - path)
        return true
    }

    suspend fun editAddKstat(context: Context, oldPath: String, newPath: String): Boolean {
        if (newPath.isBlank()) return false
        saveAddKstatPaths(context, (getAddKstatPaths(context) - oldPath) + newPath)
        return true
    }

    suspend fun updateKstat(context: Context, path: String): Boolean = true

    suspend fun updateKstatFullClone(context: Context, path: String): Boolean = true

    fun hasConfigurationForAutoStart(context: Context): Boolean = getCurrentModuleConfig(context).hasAutoStartConfig()

    suspend fun configureAutoStart(context: Context, enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        setAutoStartEnabled(context, enabled)
        true
    }

    suspend fun resetToDefault(context: Context): Boolean {
        setUname(context, DEFAULT_UNAME, DEFAULT_BUILD_TIME)
        saveSusPaths(context, emptySet())
        saveSusLoopPaths(context, emptySet())
        saveSusMaps(context, emptySet())
        saveKstatConfigs(context, emptySet())
        saveAddKstatPaths(context, emptySet())
        setAutoStartEnabled(context, false)
        return true
    }
}

object AppInfoCache {
    private val appInfoMap = mutableMapOf<String, CachedAppInfo>()

    data class CachedAppInfo(
        val appName: String,
        val packageInfo: PackageInfo?,
        val drawable: Drawable?,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun getAppInfo(packageName: String): CachedAppInfo? = appInfoMap[packageName]

    fun putAppInfo(packageName: String, appInfo: CachedAppInfo) {
        appInfoMap[packageName] = appInfo
    }

    fun clearCache() {
        appInfoMap.clear()
    }

    fun getAppInfoFromSuperUser(packageName: String): CachedAppInfo? {
        val superUserApp = SuperUserViewModel.getAppsSafely().find { it.packageName == packageName }
        return superUserApp?.let { app ->
            CachedAppInfo(
                appName = app.label,
                packageInfo = app.packageInfo,
                drawable = null
            )
        }
    }
}

