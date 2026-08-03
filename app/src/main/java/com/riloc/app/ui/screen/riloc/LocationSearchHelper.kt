package com.riloc.app.ui.screen.riloc

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.riloc.app.common.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class SearchResult(
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
)

/**
 * Provides official AMap InputTips search autocomplete and geocoding (Shadow & GlobalTraveling).
 */
object LocationSearchHelper {

    suspend fun getSuggestions(context: Context, query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()

        val amapKey = Prefs.amapKey()

        // 1. Official AMap InputTips REST API (Shadow & GlobalTraveling)
        val fromAmap = runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://restapi.amap.com/v3/assistant/inputtips?keywords=$encoded&key=$amapKey")
            val conn = url.openConnection().apply {
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 4000
                readTimeout = 4000
            }
            val jsonText = conn.getInputStream().bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)
            if (root.optString("status") == "1") {
                val tips = root.optJSONArray("tips") ?: JSONArray()
                val list = mutableListOf<SearchResult>()
                for (i in 0 until tips.length()) {
                    val tip = tips.getJSONObject(i)
                    val locationStr = tip.optString("location", "")
                    if (locationStr.isNotBlank() && locationStr.contains(",")) {
                        val coords = locationStr.split(",")
                        val lon = coords[0].toDoubleOrNull() ?: continue
                        val lat = coords[1].toDoubleOrNull() ?: continue
                        val name = tip.optString("name", query)
                        val district = tip.optString("district", "")
                        val address = tip.optString("address", "")
                        val fullAddr = listOf(district, address).filter { it.isNotBlank() }.joinToString(" ")
                        list.add(SearchResult(name = name, address = fullAddr.ifBlank { name }, lat = lat, lon = lon))
                    }
                }
                list
            } else null
        }.getOrNull()

        if (!fromAmap.isNullOrEmpty()) return@withContext fromAmap

        // 2. Fallback to system Geocoder
        val fromGeocoder = runCatching {
            val geocoder = Geocoder(context, Locale.SIMPLIFIED_CHINESE)
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocationName(query, 6)
            addresses?.mapNotNull { addr ->
                val title = addr.featureName ?: addr.thoroughfare ?: addr.subLocality ?: query
                val fullAddr = addr.getAddressLine(0) ?: title
                SearchResult(
                    name = title,
                    address = fullAddr,
                    lat = addr.latitude,
                    lon = addr.longitude,
                )
            }
        }.getOrNull()

        if (!fromGeocoder.isNullOrEmpty()) return@withContext fromGeocoder

        // 3. Fallback to OpenStreetMap Nominatim Search API
        runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=6&accept-language=zh-CN")
            val conn = url.openConnection().apply {
                setRequestProperty("User-Agent", "Riloc/1.0")
                connectTimeout = 4000
                readTimeout = 4000
            }
            val jsonText = conn.getInputStream().bufferedReader().use { it.readText() }
            val array = JSONArray(jsonText)
            val list = mutableListOf<SearchResult>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val displayName = obj.optString("display_name", query)
                val parts = displayName.split(",")
                val name = parts.firstOrNull()?.trim() ?: query
                val lat = obj.getDouble("lat")
                val lon = obj.getDouble("lon")
                list.add(SearchResult(name = name, address = displayName, lat = lat, lon = lon))
            }
            list
        }.getOrElse { emptyList() }
    }

    suspend fun searchLocation(context: Context, query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val results = getSuggestions(context, query)
        results.firstOrNull()?.let { it.lat to it.lon }
    }
}
