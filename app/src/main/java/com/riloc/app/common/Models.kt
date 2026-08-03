package com.riloc.app.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A single point on a simulated route. */
@Serializable
data class RouteWaypoint(val lat: Double, val lon: Double)

/** Persisted route configuration (UI-only, stored locally as JSON). */
@Serializable
data class RouteConfig(
    val waypoints: List<RouteWaypoint> = emptyList(),
    val speedMps: Double = 1.4,
    val loop: Boolean = false,
    val enabled: Boolean = false,
)

object RouteStore {
    private const val KEY_ROUTE = "route_config"

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): RouteConfig =
        Prefs.uiString(KEY_ROUTE, null)?.let { raw ->
            runCatching { json.decodeFromString<RouteConfig>(raw) }.getOrNull()
        } ?: RouteConfig()

    fun save(config: RouteConfig) {
        Prefs.setUiString(KEY_ROUTE, runCatching { json.encodeToString(config) }.getOrNull())
    }
}
