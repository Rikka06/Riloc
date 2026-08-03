package com.riloc.app.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Drives continuous fake-location movement for the joystick mode.
 * A 100 ms tick loop converts the joystick deflection vector into a movement
 * step and pushes the new coordinates through [LocationHub], which persists
 * them to the prefs the Xposed module reads.
 */
class MoveEngine(private val scope: CoroutineScope) {

    enum class Mode { IDLE, JOYSTICK }

    private val tickMillis = 100L
    private var tickJob: Job? = null

    private val _modeState = MutableStateFlow(Mode.IDLE)
    val modeState: StateFlow<Mode> = _modeState.asStateFlow()

    @Volatile
    var mode: Mode = Mode.IDLE
        private set

    private fun setMode(newMode: Mode) {
        mode = newMode
        _modeState.value = newMode
    }

    // joystick state (screen-relative, -1..1; +x = east, -y = north)
    @Volatile private var joyX = 0f
    @Volatile private var joyY = 0f
    @Volatile private var joySpeedMps = 4f

    val isActive: Boolean get() = mode != Mode.IDLE

    /** Updates the joystick deflection vector (both in -1..1). */
    fun setJoystickVector(x: Float, y: Float) {
        joyX = x.coerceIn(-1f, 1f)
        joyY = y.coerceIn(-1f, 1f)
    }

    fun startJoystick(speedMps: Float) {
        joySpeedMps = speedMps
        setMode(Mode.JOYSTICK)
        ensureTicking()
    }

    fun stop() {
        setMode(Mode.IDLE)
        joyX = 0f
        joyY = 0f
        tickJob?.cancel()
        tickJob = null
    }

    private fun ensureTicking() {
        if (tickJob != null) return
        tickJob = scope.launch {
            while (isActive && mode != Mode.IDLE) {
                val startedAt = System.nanoTime()
                tickJoystick()
                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
                delay((tickMillis - elapsedMs).coerceAtLeast(10L))
            }
            tickJob = null
        }
    }

    private fun tickJoystick() {
        val magnitude = hypot(joyX.toDouble(), joyY.toDouble())
        if (magnitude < 0.08) return
        // Bearing from screen vector: drag right → east, drag up → north.
        val bearingDeg = Math.toDegrees(atan2(joyX.toDouble(), -joyY.toDouble()))
        val distanceM = joySpeedMps.toDouble() * magnitude * (tickMillis / 1000.0)
        val current = LocationHub.current.value
        val (lat, lon) = GeoMath.destination(current.lat, current.lon, bearingDeg, distanceM)
        LocationHub.update(lat, lon)
    }
}
