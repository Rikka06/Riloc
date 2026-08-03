package com.riloc.app.ui.screen.riloc

import com.riloc.app.common.Prefs

/** Supported map tile styles: AutoNavi Standard 3D & AutoNavi Satellite. */
enum class MapStyle(val key: String, val label: String) {
    AMAP("amap", "高德地图"),
    AMAP_SAT("amap_sat", "高德卫星");

    companion object {
        fun fromKey(key: String?): MapStyle = entries.firstOrNull { it.key == key } ?: AMAP
    }
}

object MapStyles {
    fun current(): MapStyle = MapStyle.fromKey(Prefs.uiString("map_style", null))

    fun setCurrent(style: MapStyle) = Prefs.setUiString("map_style", style.key)
}
