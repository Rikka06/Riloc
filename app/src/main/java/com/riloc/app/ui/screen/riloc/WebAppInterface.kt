package com.riloc.app.ui.screen.riloc

import android.util.Log
import android.webkit.JavascriptInterface
import com.riloc.app.common.HistoryManager
import com.riloc.app.common.Prefs
import com.riloc.app.engine.LocationHub

class WebAppInterface(
    private val onMapReadyCallback: () -> Unit = {},
    private val onPointSelectedCallback: (lat: Double, lng: Double, name: String) -> Unit = { _, _, _ -> },
    private val onReceiveTipsCallback: (tipsStr: String) -> Unit = {},
    private val onPathDrawnCallback: (pathStr: String) -> Unit = {},
    private val onStatusCallback: (status: String) -> Unit = {},
) {

    companion object {
        private const val TAG = "RilocWebView"
    }

    @JavascriptInterface
    fun getAMapConfig(): String {
        val key = Prefs.amapKey()
        val secret = Prefs.amapSecret()
        Log.d(TAG, "getAMapConfig: key=${key.take(8)}... secret=${if (secret.isNotEmpty()) "set" else "empty"}")
        return "$key|$secret"
    }

    @JavascriptInterface
    fun reportStatus(status: String) {
        Log.i(TAG, "[STATUS] $status")
        onStatusCallback(status)
    }

    @JavascriptInterface
    fun onMapReady() {
        Log.i(TAG, "onMapReady callback received")
        onMapReadyCallback()
    }

    @JavascriptInterface
    fun onPointSelected(lat: Double, lng: Double, name: String) {
        Log.d(TAG, "onPointSelected: $lat, $lng, $name")
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
