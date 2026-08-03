package com.riloc.app.xposed.hooks

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface
import kotlin.random.Random

/**
 * Simulates motion sensors (accelerometer & step counter) when location is actively moving
 * (following hazbu/xPoint). Defeats movement/fitness verification in attendance apps.
 */
class SensorHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[SensorHooks]"

    @Volatile
    private var simulatedStepCount = 1000f

    fun init() {
        runCatching {
            val smClass = HookUtil.findClass(classLoader, "android.hardware.SensorManager") ?: return

            HookUtil.hookAll(module, smClass, "registerListener", tag) { chain ->
                LocationState.update()
                chain.proceed()
            }

            HookUtil.hookAll(module, SensorEventListener::class.java, "onSensorChanged", tag) { chain ->
                LocationState.update()
                if (LocationState.isPlaying && LocationState.simulateSensors && LocationState.isTarget(packageName)) {
                    val event = chain.args.firstOrNull() as? SensorEvent
                    if (event != null) {
                        modifySensorEvent(event)
                    }
                }
                chain.proceed()
            }
        }.onFailure { module.log(Log.DEBUG, tag, "Sensor hooks failed or skipped: ${it.message}") }

        module.log(Log.INFO, tag, "Sensor simulation hooks installed for $packageName")
    }

    private fun modifySensorEvent(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Add natural walking vibration noise around gravity (9.8 m/s²)
                val noiseX = (Random.nextFloat() - 0.5f) * 0.4f
                val noiseY = (Random.nextFloat() - 0.5f) * 0.4f
                val noiseZ = 9.80665f + (Random.nextFloat() - 0.5f) * 0.6f
                event.values[0] = noiseX
                event.values[1] = noiseY
                event.values[2] = noiseZ
            }
            Sensor.TYPE_STEP_COUNTER -> {
                if (LocationState.speed > 0f) {
                    simulatedStepCount += 0.15f
                }
                event.values[0] = simulatedStepCount
            }
        }
    }
}
