package com.riloc.app.ui.screen.riloc

import android.webkit.JavascriptInterface
import com.riloc.app.common.HistoryManager
import com.riloc.app.common.Prefs
import com.riloc.app.engine.LocationHub

/**
 * Android <-> JavaScript bridge interface aligned with GlobalTraveling / Shadow v1.2.
 */
class WebAppInterface(
    private val onMapReadyCallback: () -> Unit = {},
    private val onPointSelectedCallback: (lat: Double, lng: Double, name: String) -> Unit = { _, _, _ -> },
    private val onReceiveTipsCallback: (tipsStr: String) -> Unit = {},
    private val onPathDrawnCallback: (pathStr: String) -> Unit = {},
) {

    @JavascriptInterface
    fun getAMapConfig(): String {
        val key = Prefs.amapKey()
        val secret = Prefs.amapSecret()
        return "$key|$secret"
    }

    @JavascriptInterface
    fun onMapReady() {
        onMapReadyCallback()
    }

    @JavascriptInterface
    fun onPointSelected(lat: Double, lng: Double, name: String) {
        LocationHub.update(lat, lng)
        HistoryManager.add(name.ifBlank { "地图选点" }, lat, lng)
        onPointSelectedCallback(lat, lng, name)
    }

    @JavascriptInterface
    fun onReceiveTips(tipsStr: String) {
        onReceiveTipsCallback(tipsStr)
    }

    @JavascriptInterface
    fun onPathDrawn(pathStr: String) {
        onPathDrawnCallback(pathStr)
    }
}
