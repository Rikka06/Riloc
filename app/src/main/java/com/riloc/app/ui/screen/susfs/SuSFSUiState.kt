package com.riloc.app.ui.screen.susfs

import androidx.compose.runtime.Immutable
import com.riloc.app.R
import com.riloc.app.ui.screen.susfs.util.SuSFSManager

@Immutable
data class SuSFSUiState(
    val isLoading: Boolean = false,
    val isNavigatingBack: Boolean = false,

    val selectedTab: SuSFSTab = SuSFSTab.BASIC_SETTINGS,

    // 鍩虹閰嶇疆
    val unameValue: String = "",
    val buildTimeValue: String = "",
    val autoStartEnabled: Boolean = false,
    val canEnableAutoStart: Boolean = false,
    val executeInPostFsData: Boolean = false,
    val enableHideBl: Boolean = true,
    val enableCleanupResidue: Boolean = false,
    val enableAvcLogSpoofing: Boolean = false,
    val hideSusMountsForAllProcs: Boolean = true,

    // 妲戒綅淇℃伅
    val slotInfoList: List<SuSFSManager.SlotInfo> = emptyList(),
    val currentActiveSlot: String = "",
    val isLoadingSlotInfo: Boolean = false,
    val showSlotInfoDialog: Boolean = false,

    // 璺緞閰嶇疆
    val susPaths: Set<String> = emptySet(),
    val susLoopPaths: Set<String> = emptySet(),
    val susMaps: Set<String> = emptySet(),

    // Kstat 閰嶇疆
    val kstatConfigs: Set<String> = emptySet(),
    val addKstatPaths: Set<String> = emptySet(),

    // 宸插惎鐢ㄥ姛鑳?
    val enabledFeatures: List<SuSFSManager.EnabledFeature> = emptyList(),
    val isLoadingFeatures: Boolean = false,

    // 搴旂敤鍒楄〃锛堢敤浜庢坊鍔犺矾寰勶級
    val installedApps: List<SuSFSManager.AppInfo> = emptyList(),

    // 瀵硅瘽妗嗙姸鎬?
    val showConfirmReset: Boolean = false,
    val showAddPathDialog: Boolean = false,
    val showAddLoopPathDialog: Boolean = false,
    val showAddSusMapDialog: Boolean = false,
    val showAddAppPathDialog: Boolean = false,
    val showAddKstatStaticallyDialog: Boolean = false,
    val showAddKstatDialog: Boolean = false,

    val showResetPathsDialog: Boolean = false,
    val showResetLoopPathsDialog: Boolean = false,
    val showResetSusMapsDialog: Boolean = false,
    val showResetKstatDialog: Boolean = false,

    // 缂栬緫涓」
    val editingPath: String? = null,
    val editingLoopPath: String? = null,
    val editingSusMap: String? = null,
    val editingKstatConfig: String? = null,
    val editingKstatPath: String? = null,

    val error: Throwable? = null,
)

enum class SuSFSTab(val displayNameRes: Int) {
    BASIC_SETTINGS(R.string.susfs_tab_basic_settings),
    SUS_PATHS(R.string.susfs_tab_sus_paths),
    SUS_LOOP_PATHS(R.string.susfs_tab_sus_loop_paths),
    SUS_MAPS(R.string.susfs_tab_sus_maps),
    KSTAT_CONFIG(R.string.susfs_tab_kstat_config),
    ENABLED_FEATURES(R.string.susfs_tab_enabled_features);

    companion object {
        fun getAllTabs(): List<SuSFSTab> {
            return entries.toList()
        }
    }
}

