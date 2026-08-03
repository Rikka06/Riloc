package com.riloc.app.xposed.hooks

import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface
import java.util.concurrent.Executor

/**
 * Spoofs `GnssStatus` callbacks (following Portal & GhostMap X).
 *
 * Anti-mock detectors query satellite visibility to check if a device is indoors
 * or receiving real GNSS signals. This hook synthesizes 14 active satellite signals
 * (GPS, Beidou, GLONASS, Galileo) with carrier-to-noise ratio >35 dBHz.
 */
class GnssStatusHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[GnssStatusHooks]"

    fun init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        runCatching {
            val lmClass = Class.forName("android.location.LocationManager", false, classLoader)

            HookUtil.hookAll(module, lmClass, "registerGnssStatusCallback", tag) { chain ->
                LocationState.update()
                if (LocationState.isPlaying && LocationState.isTarget(packageName)) {
                    val executor = chain.args.firstOrNull { it is Executor } as? Executor
                    val callback = chain.args.firstOrNull { it is GnssStatus.Callback } as? GnssStatus.Callback

                    if (callback != null) {
                        val fakeStatus = createFakeGnssStatus()
                        if (fakeStatus != null) {
                            val task = Runnable {
                                callback.onStarted()
                                callback.onSatelliteStatusChanged(fakeStatus)
                            }
                            if (executor != null) executor.execute(task) else task.run()
                        }
                    }
                    true
                } else {
                    chain.proceed()
                }
            }
        }.onFailure { module.log(Log.DEBUG, tag, "GNSS status hooks skipped or failed: ${it.message}") }

        module.log(Log.INFO, tag, "GNSS status hooks installed for $packageName")
    }

    private fun createFakeGnssStatus(): GnssStatus? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            val builder = GnssStatus.Builder()
            val addMethod = builder.javaClass.methods.firstOrNull { it.name == "addSatellite" } ?: return null
            val paramTypes = addMethod.parameterTypes

            fun addSat(type: Int, svid: Int, cn0: Float, elev: Float, azim: Float) {
                val args = arrayOfNulls<Any>(paramTypes.size)
                if (paramTypes.isEmpty()) return
                args[0] = type
                args[1] = svid
                args[2] = cn0
                args[3] = elev
                args[4] = azim
                args[5] = true // hasEphemeris
                args[6] = true // hasAlmanac
                args[7] = true // usedInFix
                for (i in 8 until paramTypes.size) {
                    when (paramTypes[i]) {
                        Boolean::class.javaPrimitiveType, java.lang.Boolean.TYPE -> args[i] = true
                        Float::class.javaPrimitiveType, java.lang.Float.TYPE -> args[i] = 38.0f
                        Int::class.javaPrimitiveType, java.lang.Integer.TYPE -> args[i] = 0
                    }
                }
                addMethod.invoke(builder, *args)
            }

            for (svid in 1..6) addSat(GnssStatus.CONSTELLATION_GPS, svid, 38f + svid, 45f + svid * 5f, 120f + svid * 10f)
            for (svid in 1..6) addSat(GnssStatus.CONSTELLATION_BEIDOU, svid, 40f + svid, 60f + svid * 4f, 200f + svid * 12f)
            for (svid in 1..2) addSat(GnssStatus.CONSTELLATION_GLONASS, svid, 36f + svid, 30f + svid * 10f, 45f + svid * 15f)

            builder.build()
        }.getOrNull()
    }
}
