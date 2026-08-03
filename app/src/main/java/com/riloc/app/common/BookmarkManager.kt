package com.riloc.app.common

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
)

/**
 * Manages user's saved location bookmarks/favorites.
 */
object BookmarkManager {

    private const val PREF_KEY_BOOKMARKS = "saved_bookmarks"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("riloc_bookmarks", Context.MODE_PRIVATE)
    }

    fun getAll(): List<Bookmark> {
        val jsonStr = prefs.getString(PREF_KEY_BOOKMARKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Bookmark>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Bookmark(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        lat = obj.getDouble("lat"),
                        lon = obj.getDouble("lon"),
                    )
                )
            }
            list
        }.getOrElse { emptyList() }
    }

    fun save(name: String, lat: Double, lon: Double): Bookmark {
        val current = getAll().toMutableList()
        val bookmark = Bookmark(
            id = System.currentTimeMillis().toString(),
            name = name.ifBlank { "%.4f, %.4f".format(lat, lon) },
            lat = lat,
            lon = lon,
        )
        current.add(bookmark)
        saveAll(current)
        return bookmark
    }

    fun delete(id: String) {
        val filtered = getAll().filterNot { it.id == id }
        saveAll(filtered)
    }

    private fun saveAll(list: List<Bookmark>) {
        val array = JSONArray()
        list.forEach { b ->
            val obj = JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("lat", b.lat)
                put("lon", b.lon)
            }
            array.put(obj)
        }
        prefs.edit().putString(PREF_KEY_BOOKMARKS, array.toString()).apply()
    }
}
