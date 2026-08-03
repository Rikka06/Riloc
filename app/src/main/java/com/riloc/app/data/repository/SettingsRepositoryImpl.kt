package com.riloc.app.data.repository

import android.content.Context
import androidx.core.content.edit
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.riloc.app.Natives
import com.riloc.app.ksuApp
import com.riloc.app.ui.UiMode

class SettingsRepositoryImpl : SettingsRepository {
    private val prefs by lazy {
        ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override var uiMode: String
        get() = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        set(value) = prefs.edit { putString("ui_mode", value) }

    override var checkUpdate: Boolean
        get() = prefs.getBoolean("check_update", false)
        set(value) = prefs.edit { putBoolean("check_update", value) }

    override var checkModuleUpdate: Boolean
        get() = prefs.getBoolean("module_check_update", false)
        set(value) = prefs.edit { putBoolean("module_check_update", value) }

    override var alternativeIcon: Boolean
        get() = prefs.getBoolean("use_alt_icon", false)
        set(value) = prefs.edit { putBoolean("use_alt_icon", value) }

    override var themeMode: Int
        get() = prefs.getInt("color_mode", 0)
        set(value) = prefs.edit { putInt("color_mode", value) }

    override var miuixMonet: Boolean
        get() = prefs.getBoolean("miuix_monet", false)
        set(value) = prefs.edit { putBoolean("miuix_monet", value) }

    override var keyColor: Int
        get() = prefs.getInt("key_color", 0)
        set(value) = prefs.edit { putInt("key_color", value) }

    override var colorStyle: String
        get() = prefs.getString("color_style", PaletteStyle.TonalSpot.name) ?: PaletteStyle.TonalSpot.name
        set(value) = prefs.edit { putString("color_style", value) }

    override var colorSpec: String
        get() = prefs.getString("color_spec", ColorSpec.SpecVersion.Default.name) ?: ColorSpec.SpecVersion.Default.name
        set(value) = prefs.edit { putString("color_spec", value) }

    override var enablePredictiveBack: Boolean
        get() = prefs.getBoolean("enable_predictive_back", false)
        set(value) = prefs.edit { putBoolean("enable_predictive_back", value) }

    override var enableBlur: Boolean
        get() = prefs.getBoolean("enable_blur", false)
        set(value) = prefs.edit { putBoolean("enable_blur", value) }

    override var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar", value) }

    override var enableFloatingBottomBarBlur: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar_blur", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar_blur", value) }

    override var pageScale: Float
        get() = prefs.getFloat("page_scale", 1.0f)
        set(value) = prefs.edit { putFloat("page_scale", value) }

    override var enableWebDebugging: Boolean
        get() = prefs.getBoolean("enable_web_debugging", false)
        set(value) = prefs.edit { putBoolean("enable_web_debugging", value) }

    override var showFullStatus: Boolean
        get() = prefs.getBoolean("show_fingerprint", true)
        set(value) = prefs.edit { putBoolean("show_fingerprint", value) }

    override var autoJailbreak: Boolean
        get() = prefs.getBoolean("auto_jailbreak", false)
        set(value) = prefs.edit { putBoolean("auto_jailbreak", value) }

    override suspend fun getSuCompatStatus(): String = "Enabled (UI Template)"
    override suspend fun getSuCompatPersistValue(): Long? = 1L
    override fun isSuEnabled(): Boolean = Natives.isSuEnabled()
    override fun setSuEnabled(enabled: Boolean): Boolean = true
    override fun setSuCompatModePref(mode: Int) = prefs.edit { putInt("su_compat_mode", mode) }
    override fun getSuCompatModePref(): Int = prefs.getInt("su_compat_mode", 0)

    override suspend fun getKernelUmountStatus(): String = "Enabled (UI Template)"
    override fun isKernelUmountEnabled(): Boolean = Natives.isKernelUmountEnabled()
    override fun setKernelUmountEnabled(enabled: Boolean): Boolean = true

    override suspend fun getSelinuxHideStatus(): String = "Disabled (UI Template)"
    override fun isSelinuxHideEnabled(): Boolean = false
    override fun setSelinuxHideEnabled(enabled: Boolean): Int = if (enabled) 1 else 0

    override suspend fun getSulogStatus(): String = "Enabled (UI Template)"
    override suspend fun getSulogPersistValue(): Long? = 1L
    override fun setSulogEnabled(enabled: Boolean): Boolean = true

    override suspend fun getAdbRootStatus(): String = "Disabled (UI Template)"
    override suspend fun getAdbRootPersistValue(): Long? = 0L
    override fun setAdbRootEnabled(enabled: Boolean): Boolean = true

    override fun isDefaultUmountModules(): Boolean = Natives.isDefaultUmountModules()
    override fun setDefaultUmountModules(enabled: Boolean): Boolean = Natives.setDefaultUmountModules(enabled)

    override fun isLkmMode(): Boolean = Natives.isLkmMode

    override fun execKsudFeatureSave() = Unit
}

