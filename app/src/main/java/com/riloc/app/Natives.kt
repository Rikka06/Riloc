package com.riloc.app

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.riloc.app.Natives.Profile.RootProfileFlag
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * UI-template replacement for the real native bridge.
 *
 * The original manager talks to kernel/native code from this object. This
 * template keeps the public shape used by the Compose UI, but every value is
 * static mock data and no native library is loaded.
 */
object Natives {
    const val MINIMAL_SUPPORTED_KERNEL = 32513
    const val MINIMAL_SUPPORTED_KERNEL_FULL = "v4.0.0"
    const val KERNEL_SU_DOMAIN = "u:r:ksu:s0"
    const val ROOT_UID = 0
    const val ROOT_GID = 0
    const val FLAG_KSU_NO_NEW_PRIVS = 1L

    private val profiles = mutableMapOf<String, Profile>()

    fun getFullVersion(): String = "v4.1.3-ui-template"

    val version: Int
        get() = 40103

    val isSafeMode: Boolean
        get() = false

    val isLkmMode: Boolean
        get() = false

    val isLateLoadMode: Boolean
        get() = false

    val isManager: Boolean
        get() = true

    val isPrBuild: Boolean
        get() = false

    val kernelUAPIVersion: Int
        get() = 12

    val managerUAPIVersion: Int
        get() = 12

    fun uidShouldUmount(uid: Int): Boolean = uid % 2 == 0

    fun getAppProfile(key: String?, uid: Int): Profile {
        val safeKey = key ?: "$"
        return profiles.getOrPut("$safeKey:$uid") {
            Profile(
                name = safeKey,
                currentUid = uid,
                allowSu = safeKey.contains("terminal") || safeKey.contains("toolbox"),
                rootUseDefault = !safeKey.contains("toolbox"),
                nonRootUseDefault = !safeKey.contains("browser"),
                umountModules = uidShouldUmount(uid),
            )
        }
    }

    fun setAppProfile(profile: Profile?): Boolean {
        if (profile != null) {
            profiles["${profile.name}:${profile.currentUid}"] = profile
        }
        return true
    }

    fun isSuEnabled(): Boolean = true
    fun setSuEnabled(enabled: Boolean): Boolean = true
    fun isKernelUmountEnabled(): Boolean = true
    fun setKernelUmountEnabled(enabled: Boolean): Boolean = true
    fun isSelinuxHideEnabled(): Boolean = false
    fun setSelinuxHideEnabled(enabled: Boolean): Int = if (enabled) 1 else 0
    fun getUserName(uid: Int): String? = if (uid >= 100000) "user ${uid / 100000}" else "owner"
    fun getSuperuserCount(): Int = 4
    fun getHookType(): String = "GKI"

    fun isVersionLessThan(v1Full: String, v2Full: String): Boolean {
        fun extractVersionParts(version: String): List<Int> {
            val match = Regex("""v\d+(\.\d+)*""").find(version)
            val simpleVersion = match?.value ?: version
            return simpleVersion.trimStart('v').split('.').map { it.toIntOrNull() ?: 0 }
        }

        val v1Parts = extractVersionParts(v1Full)
        val v2Parts = extractVersionParts(v2Full)
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        for (i in 0 until maxLength) {
            val num1 = v1Parts.getOrElse(i) { 0 }
            val num2 = v2Parts.getOrElse(i) { 0 }
            if (num1 != num2) return num1 < num2
        }
        return false
    }

    fun setDefaultUmountModules(umountModules: Boolean): Boolean {
        profiles["$:9999"] = Profile("$", 9999, false, umountModules = umountModules)
        return true
    }

    fun isDefaultUmountModules(): Boolean = profiles["$:9999"]?.umountModules ?: false

    fun checkUAPIMismatch(): Boolean = false

    fun requireNewKernel(): Boolean = false

    @Keep
    @Immutable
    @Parcelize
    @Serializable
    data class Profile(
        val name: String,
        val currentUid: Int = 0,
        val allowSu: Boolean = false,
        val rootUseDefault: Boolean = true,
        val rootTemplate: String? = null,
        val uid: Int = ROOT_UID,
        val gid: Int = ROOT_GID,
        val groups: List<Int> = mutableListOf(),
        val capabilities: List<Int> = mutableListOf(),
        val context: String = KERNEL_SU_DOMAIN,
        val namespace: Int = Namespace.INHERITED.ordinal,
        val nonRootUseDefault: Boolean = true,
        val umountModules: Boolean = true,
        var rules: String = "",
        val flags: Long = FLAG_KSU_NO_NEW_PRIVS,
    ) : Parcelable {
        @Keep
        enum class RootProfileFlag(val display: String, val desc: Int) {
            NO_NEW_PRIVS(
                "NO_NEW_PRIVS",
                R.string.profile_flags_desc_no_new_privs
            )
        }

        enum class Namespace {
            INHERITED,
            GLOBAL,
            INDIVIDUAL,
        }

        constructor() : this("")
    }
}

fun List<RootProfileFlag>.toRawFlags(): Long =
    fold(0L) { acc, flag -> acc.or(1L.shl(flag.ordinal)) }

fun List<RootProfileFlag>.toOrdinalList(): List<Int> =
    map { it.ordinal }

fun Long.toRootProfileFlags(): List<RootProfileFlag> =
    RootProfileFlag.entries.filter { 1L.shl(it.ordinal).and(this) != 0L }.toList()

