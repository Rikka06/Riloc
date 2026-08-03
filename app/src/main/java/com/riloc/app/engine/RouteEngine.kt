package com.riloc.app.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Multi-point route navigation & path playback engine (from GlobalTraveling & xPoint).
 */
class RouteEngine(private val scope: CoroutineScope) {

    enum class RouteState { IDLE, PLAYING, PAUSED }
    enum class LoopMode { ONCE, REPEAT }

    private val tickMillis = 100L
    private var routeJob: Job? = null

    private val _state = MutableStateFlow(RouteState.IDLE)
    val state: StateFlow<RouteState> = _state.asStateFlow()

    private val waypoints = mutableListOf<LatLng>()
    private var currentSegmentIndex = 0
    private var currentSegmentProgressMeters = 0.0

    @Volatile var speedMps: Float = 4.0f
    @Volatile var speedMultiplier: Float = 1.0f
    @Volatile var loopMode: LoopMode = LoopMode.REPEAT

    val isRunning: Boolean get() = _state.value == RouteState.PLAYING

    fun setWaypoints(points: List<LatLng>) {
        waypoints.clear()
        waypoints.addAll(points)
        currentSegmentIndex = 0
        currentSegmentProgressMeters = 0.0
    }

    fun addWaypoint(point: LatLng) {
        waypoints.add(point)
    }

    fun clearRoute() {
        stop()
        waypoints.clear()
    }

    fun getWaypoints(): List<LatLng> = ArrayList(waypoints)

    fun start(speed: Float = speedMps, multiplier: Float = speedMultiplier, mode: LoopMode = loopMode) {
        if (waypoints.size < 2) return
        speedMps = speed
        speedMultiplier = multiplier
        loopMode = mode
        _state.value = RouteState.PLAYING
        ensureTicking()
    }

    fun pause() {
        _state.value = RouteState.PAUSED
    }

    fun resume() {
        if (waypoints.size < 2) return
        _state.value = RouteState.PLAYING
        ensureTicking()
    }

    fun stop() {
        _state.value = RouteState.IDLE
        currentSegmentIndex = 0
        currentSegmentProgressMeters = 0.0
        routeJob?.cancel()
        routeJob = null
    }

    private fun ensureTicking() {
        if (routeJob != null) return
        routeJob = scope.launch {
            while (_state.value == RouteState.PLAYING) {
                val startedAt = System.nanoTime()
                tickStep()
                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
                delay((tickMillis - elapsedMs).coerceAtLeast(10L))
            }
            routeJob = null
        }
    }

    private fun tickStep() {
        if (waypoints.size < 2 || currentSegmentIndex >= waypoints.size - 1) {
            if (loopMode == LoopMode.REPEAT && waypoints.size >= 2) {
                currentSegmentIndex = 0
                currentSegmentProgressMeters = 0.0
            } else {
                stop()
                return
            }
        }

        val p1 = waypoints[currentSegmentIndex]
        val p2 = waypoints[currentSegmentIndex + 1]
        val segmentDistance = GeoMath.distanceMeters(p1.lat, p1.lon, p2.lat, p2.lon)

        val jitterFactor = 1.0 + (kotlin.random.Random.nextDouble(-0.15, 0.15))
        val stepDistance = (speedMps * speedMultiplier * jitterFactor) * (tickMillis / 1000.0)

        currentSegmentProgressMeters += stepDistance



        if (currentSegmentProgressMeters >= segmentDistance) {
            currentSegmentProgressMeters = 0.0
            currentSegmentIndex++
            if (currentSegmentIndex >= waypoints.size - 1) {
                if (loopMode == LoopMode.REPEAT) {
                    currentSegmentIndex = 0
                } else {
                    LocationHub.update(p2.lat, p2.lon)
                    stop()
                    return
                }
            }
            val nextP = waypoints[currentSegmentIndex]
            LocationHub.update(nextP.lat, nextP.lon)
        } else {
            val t = (currentSegmentProgressMeters / segmentDistance).coerceIn(0.0, 1.0)
            val (lat, lon) = GeoMath.interpolate(p1.lat, p1.lon, p2.lat, p2.lon, t)
            LocationHub.update(lat, lon)
        }
    }
}
