package com.riloc.app.ui.screen.riloc

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.riloc.app.common.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

/**
 * Translates latitude & longitude into official AMap formatted address names (Shadow & GlobalTraveling).
 */
object ReverseGeocoder {

    private val cache = mutableMapOf<String, String>()

    suspend fun getAddress(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        val key = "%.4f,%.4f".format(lat, lon)
        cache[key]?.let { return@withContext it }

        val amapKey = Prefs.amapKey()

        // 1. Official AMap ReGeo REST API
        val fromAmap = runCatching {
            val url = URL("https://restapi.amap.com/v3/geocode/regeo?location=%.6f,%.6f&key=$amapKey".format(lon, lat))
            val conn = url.openConnection().apply {
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 4000
                readTimeout = 4000
            }
            val jsonText = conn.getInputStream().bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)
            if (root.optString("status") == "1") {
                val regeocode = root.optJSONObject("regeocode")
                val formattedAddr = regeocode?.optString("formatted_address", "")
                if (!formattedAddr.isNullOrBlank()) formattedAddr else null
            } else null
        }.getOrNull()

        if (!fromAmap.isNullOrBlank()) {
            cache[key] = fromAmap
            return@withContext fromAmap
        }

        // 2. Fallback to system Geocoder
        val fromGeocoder = runCatching {
            val geocoder = Geocoder(context, Locale.SIMPLIFIED_CHINESE)
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                val sb = StringBuilder()
                addr.adminArea?.let { sb.append(it) }
                addr.locality?.let { if (it != addr.adminArea) sb.append(it) }
                addr.subLocality?.let { sb.append(it) }
                addr.thoroughfare?.let { sb.append(it) }
                addr.featureName?.let { if (it != addr.thoroughfare) sb.append(it) }
                sb.toString().ifBlank { addr.getAddressLine(0) ?: "" }
            } else null
        }.getOrNull()

        if (!fromGeocoder.isNullOrBlank()) {
            cache[key] = fromGeocoder
            return@withContext fromGeocoder
        }

        // 3. Fallback to OpenStreetMap Nominatim Reverse Geocoding API
        val fromNominatim = runCatching {
            val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&accept-language=zh-CN")
            val conn = url.openConnection().apply {
                setRequestProperty("User-Agent", "Riloc/1.0")
                connectTimeout = 4000
                readTimeout = 4000
            }
            val jsonText = conn.getInputStream().bufferedReader().use { it.readText() }
            val obj = JSONObject(jsonText)
            obj.optString("display_name", "")
        }.getOrNull()

        val finalAddress = fromNominatim?.ifBlank { null } ?: "%.6f, %.6f".format(lat, lon)
        cache[key] = finalAddress
        finalAddress
    }
}
