package com.riloc.app.ui.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import com.riloc.app.Natives
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

class Shell : java.io.Closeable {
    class Result(val code: Int = 0, val err: List<String> = emptyList())
    class Builder {
        companion object { fun create() = Builder() }
        fun build(sh: String) = Shell()
    }
    override fun close() {}
}

/**
 * UI-template no-op implementation of the original root/ksud utility layer.
 *
 * These functions keep the exact call surface expected by the original
 * Compose screens, but never request root, execute ksud, flash images, reboot,
 * download tools, edit sepolicy, or touch real module state.
 */
data class FlashResult(val code: Int, val err: String, val showReboot: Boolean) {
    constructor(result: Shell.Result, showReboot: Boolean) : this(result.code, result.err.joinToString("\n"), showReboot)
    constructor(result: Shell.Result) : this(result, false)
}

object KsuCli {
    val SHELL: Shell by lazy { createRootShell() }
    val GLOBAL_MNT_SHELL: Shell by lazy { createRootShell(true) }
}

fun getRootShell(globalMnt: Boolean = false): Shell = if (globalMnt) KsuCli.GLOBAL_MNT_SHELL else KsuCli.SHELL

inline fun <T> withNewRootShell(
    globalMnt: Boolean = false,
    block: Shell.() -> T
): T = createRootShell(globalMnt).use(block)

fun Uri.getFileName(context: Context): String? {
    var fileName: String? = null
    val contentResolver: ContentResolver = context.contentResolver
    val cursor: Cursor? = contentResolver.query(this, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }
    return fileName
}

fun createRootShell(globalMnt: Boolean = false): Shell = Shell.Builder.create().build("sh")

fun execKsud(args: String, newShell: Boolean = false): Boolean = true

suspend fun getFeatureStatus(feature: String): String = withContext(Dispatchers.IO) {
    when (feature) {
        "su_compat" -> "Enabled (UI Template)"
        "kernel_umount" -> "Enabled (UI Template)"
        "selinux_hide" -> "Disabled (UI Template)"
        "sulog" -> "Enabled (UI Template)"
        "adb_root" -> "Disabled (UI Template)"
        else -> "Mocked"
    }
}

suspend fun getFeaturePersistValue(feature: String): Long? = 1L

fun install() = Unit

fun listModules(): String = """
    [
      {
        "id": "template.mount.layer",
        "name": "Template Mount Layer",
        "author": "SukiSU UI Template",
        "version": "v0.0.1",
        "versionCode": 1,
        "description": "UI placeholder module. No files are mounted or changed.",
        "enabled": true,
        "update": false,
        "remove": false,
        "updateJson": "",
        "web": true,
        "action": true,
        "metamodule": false
      },
      {
        "id": "template.webui.panel",
        "name": "Template WebUI Panel",
        "author": "SukiSU UI Template",
        "version": "v0.0.4",
        "versionCode": 4,
        "description": "Shows the original WebUI entry style without loading real module content.",
        "enabled": true,
        "update": false,
        "remove": false,
        "updateJson": "",
        "web": true,
        "action": false,
        "metamodule": false
      },
      {
        "id": "template.policy.pack",
        "name": "Template Policy Pack",
        "author": "SukiSU UI Template",
        "version": "v0.0.2",
        "versionCode": 2,
        "description": "Demonstrates disabled module state with redacted policy data.",
        "enabled": false,
        "update": false,
        "remove": false,
        "updateJson": "",
        "web": false,
        "action": false,
        "metamodule": false
      }
    ]
""".trimIndent()

fun getModuleCount(): Int = 3

fun getSuperuserCount(): Int = Natives.getSuperuserCount()

fun toggleModule(id: String, enable: Boolean): Boolean = true

fun undoUninstallModule(id: String): Boolean = true

fun uninstallModule(id: String): Boolean = true

fun flashModule(
    uri: Uri,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    onStdout("UI template: module install flow preview only.")
    return FlashResult(0, "", false)
}

fun runModuleAction(
    moduleId: String,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): Boolean {
    onStdout("UI template: action script is not executed.")
    return true
}

fun restoreBoot(
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    onStdout("UI template: restore boot preview only.")
    return FlashResult(0, "", false)
}

fun uninstallPermanently(
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    onStdout("UI template: uninstall preview only.")
    return FlashResult(0, "", false)
}

@Parcelize
sealed class LkmSelection : Parcelable {
    @Parcelize
    data class LkmUri(val uri: Uri) : LkmSelection()

    @Parcelize
    data class KmiString(val value: String) : LkmSelection()

    @Parcelize
    data object KmiNone : LkmSelection()
}

fun installBoot(
    bootUri: Uri?,
    lkm: LkmSelection,
    ota: Boolean,
    partition: String?,
    allowShell: Boolean,
    enableAdb: Boolean,
    spoofRelease: String,
    spoofVersion: String,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit,
): FlashResult {
    onStdout("UI template: boot patch/install flow preview only.")
    return FlashResult(0, "", false)
}

fun reboot(reason: String = "") = Unit

fun rootAvailable(): Boolean = true

suspend fun getCurrentKmi(): String = "android-template"

suspend fun getSupportedKmis(): List<String> = listOf("android12-5.10", "android13-5.15", "android14-6.1")

suspend fun isAbDevice(): Boolean = true

suspend fun getDefaultPartition(): String = "boot"

suspend fun getSlotSuffix(ota: Boolean): String = if (ota) "_b" else "_a"

suspend fun getAvailablePartitions(): List<String> = listOf("boot", "init_boot", "vendor_boot")

fun hasMagisk(): Boolean = false

fun isSepolicyValid(rules: String?): Boolean = true

fun getSepolicy(pkg: String): String = "allow $pkg self:process { getattr };"

fun setSepolicy(pkg: String, rules: String): Boolean = true

private val templateStore = linkedMapOf(
    "default-template" to """{"id":"default-template","name":"Default Template","description":"Local UI template","local":true}""",
    "isolated-template" to """{"id":"isolated-template","name":"Isolated Template","description":"Redacted profile template","local":true}""",
)

fun listAppProfileTemplates(): List<String> = templateStore.keys.toList()

fun getAppProfileTemplate(id: String): String = templateStore[id].orEmpty()

fun setAppProfileTemplate(id: String, template: String): Boolean {
    templateStore[id] = template
    return true
}

fun deleteAppProfileTemplate(id: String): Boolean {
    templateStore.remove(id)
    return true
}

fun forceStopApp(packageName: String, userId: Int? = null) = Unit

fun launchApp(packageName: String, userId: Int? = null) = Unit

fun restartApp(packageName: String, userId: Int? = null) = Unit

fun loadKpmModule(path: String, args: String? = null): Boolean = true

fun unloadKpmModule(name: String): Boolean = true

fun getKpmModuleCount(): Int = 2

fun runCmd(shell: Shell, cmd: String): String = ""

suspend fun streamFile(path: String): List<String> = withContext(Dispatchers.IO) {
    listOf(
        "[09:12:04] allow request from app.example.terminal uid=10xxx",
        "[09:12:06] profile selected: default-template",
        "[09:18:21] module scan completed: 3 mock entries",
        "[09:20:10] denied request from app.example.browser uid=10xxx",
        "[09:26:33] sensitive identifier replaced with <redacted>",
    )
}

fun listKpmModules(): String = "template_kpm_guard\ntemplate_kpm_panel"

fun getKpmModuleInfo(name: String): String = """
    name=$name
    version=v0.0.1
    author=SukiSU UI Template
    description=KPM placeholder module. No kernel module is loaded.
    args=--template
""".trimIndent()

fun controlKpmModule(name: String, args: String? = null): Int = 0

fun getKpmVersion(): String = "KPM UI Template"

fun getSuSFSStatus(): String = "true"

fun getSuSFSVersion(): String = "v0.0.0-template"

fun getSuSFSFeatures(): String = """
    sus_su: supported
    hide_mounts: supported
    spoof_uname: mock
""".trimIndent()

fun spoofKernelUname(release: String, version: String): Boolean = true

fun addUmountPath(path: String, flags: Int): Boolean = true

fun removeUmountPath(path: String): Boolean = true

fun listUmountPaths(): String = "/system/bin/app_process\n/vendor/bin/example"

fun clearCustomUmountPaths(): Boolean = true

fun saveUmountConfig(): Boolean = true

fun applyUmountConfigToKernel(): Boolean = true

@Composable
fun rememberKpmAvailable(): Boolean = true

data class BootConfig(
    val allowShell: Boolean = false,
    val spoofRelease: String = "",
    val spoofVersion: String = "",
)

suspend fun getBootConfig(): BootConfig = BootConfig(
    allowShell = false,
    spoofRelease = "template-release",
    spoofVersion = "template-version",
)

