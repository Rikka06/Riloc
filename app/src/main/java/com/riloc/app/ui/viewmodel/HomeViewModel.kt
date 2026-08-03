package com.riloc.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.riloc.app.BuildConfig
import com.riloc.app.KernelVersion
import com.riloc.app.Natives
import com.riloc.app.ksuApp
import com.riloc.app.ui.screen.home.HomeUiState
import com.riloc.app.ui.screen.home.SystemInfo
import com.riloc.app.ui.screen.home.getManagerVersion
import com.riloc.app.ui.util.checkNewVersion
import com.riloc.app.ui.util.getModuleCount
import com.riloc.app.ui.util.getSuperuserCount
import com.riloc.app.ui.util.module.LatestVersionInfo
import com.riloc.app.ui.util.rootAvailable

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val baseState = withContext(Dispatchers.IO) { buildState() }
            _uiState.update { baseState }
            if (baseState.checkUpdateEnabled) {
                val latestVersionInfo = withContext(Dispatchers.IO) { checkNewVersion() }
                _uiState.update { it.copy(latestVersionInfo = latestVersionInfo) }
            }
        }
    }

    private fun buildState(): HomeUiState {
        val kernelVersion = KernelVersion(5, 15, 148)
        val isManager = Natives.isManager
        val ksuVersion = if (isManager) Natives.version else null
        val kernelUAPIVersion = if (isManager) Natives.kernelUAPIVersion else null
        val managerUAPIVersion = Natives.managerUAPIVersion
        val lkmMode = ksuVersion?.let { if (kernelVersion.isGKI()) Natives.isLkmMode else null }
        val isRootAvailable = rootAvailable()
        val managerVersion = getManagerVersion(ksuApp)
        val kernelFullVersion = if (isManager) Natives.getFullVersion() else null

        return HomeUiState(
            kernelVersion = kernelVersion,
            ksuVersion = ksuVersion,
            lkmMode = lkmMode,
            isManager = isManager,
            isManagerPrBuild = BuildConfig.DEBUG,
            isKernelPrBuild = Natives.isPrBuild,
            requiresNewKernel = isManager && Natives.requireNewKernel(),
            uapiMismatch = isManager && Natives.checkUAPIMismatch(),
            kernelUAPIVersion = kernelUAPIVersion,
            managerUAPIVersion = managerUAPIVersion,
            isRootAvailable = isRootAvailable,
            isSafeMode = Natives.isSafeMode,
            isLateLoadMode = Natives.isLateLoadMode,
            checkUpdateEnabled = ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("check_update", true),
            showFullStatus = ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("show_fingerprint", true),
            latestVersionInfo = LatestVersionInfo(),
            currentManagerVersionCode = managerVersion.versionCode,
            superuserCount = getSuperuserCount(),
            moduleCount = getModuleCount(),
            systemInfo = SystemInfo(
                kernelVersion = "5.15.148-template-gki",
                managerVersion = "${managerVersion.versionName} (${managerVersion.versionCode}-${managerUAPIVersion})",
                deviceModel = "Template Device",
                kernelFullVersion = kernelFullVersion,
                fingerprint = "vendor/template/device:XX/TEMPLATE/000000:user/release-keys",
                selinuxStatus = "Enforcing",
                seccompStatus = 2,
            ),
        )
    }
}

