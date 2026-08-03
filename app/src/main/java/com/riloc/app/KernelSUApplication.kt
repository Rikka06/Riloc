package com.riloc.app

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.UserManager
import android.system.Os
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.riloc.app.common.Prefs
import com.riloc.app.ui.viewmodel.SuperUserViewModel
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File

lateinit var ksuApp: KernelSUApplication

/**
 * Application entry: keeps the template's UI infrastructure (view model store,
 * predictive-back, superuser list) and adds the Riloc Xposed service binding so
 * the manager UI can share remote preferences with the Xposed module.
 */
class KernelSUApplication : Application(), ViewModelStoreOwner, XposedServiceHelper.OnServiceListener {

    companion object {
        private val _serviceState = MutableStateFlow<XposedService?>(null)
        /** Emits the bound [XposedService] (or null when LSPosed is unavailable). */
        val serviceState: StateFlow<XposedService?> = _serviceState.asStateFlow()
        val service: XposedService? get() = _serviceState.value

        fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean) {
            runCatching {
                val applicationInfoClass = ApplicationInfo::class.java
                val method = applicationInfoClass.getDeclaredMethod("setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(appInfo, enable)
            }
        }
    }

    private val appViewModelStore by lazy { ViewModelStore() }

    private fun isUserUnlocked(): Boolean =
        getSystemService(UserManager::class.java)?.isUserUnlocked == true

    override fun onCreate() {
        super.onCreate()
        ksuApp = this
        Prefs.init(this)
        com.riloc.app.common.BookmarkManager.init(this)
        com.riloc.app.common.HistoryManager.init(this)
        com.riloc.app.common.PerAppLocationManager.init(this)
        XposedServiceHelper.registerListener(this)



        if (!isUserUnlocked()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val prefs = this.getSharedPreferences("settings", MODE_PRIVATE)
            val enable = prefs.getBoolean("enable_predictive_back", false)
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback")
            setEnableOnBackInvokedCallback(applicationInfo, enable)
        }

        runCatching {
            val superUserViewModel = ViewModelProvider(this)[SuperUserViewModel::class.java]
            superUserViewModel.loadAppList()
        }

        val webroot = File(dataDir, "webroot")
        if (!webroot.exists()) {
            webroot.mkdir()
        }

        // Provide working env for rust's temp_dir()
        Os.setenv("TMPDIR", cacheDir.absolutePath, true)
    }

    override fun onServiceBind(service: XposedService) {
        _serviceState.value = service
    }

    override fun onServiceDied(service: XposedService) {
        _serviceState.value = null
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
