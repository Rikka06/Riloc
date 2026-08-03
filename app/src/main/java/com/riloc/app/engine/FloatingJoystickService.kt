package com.riloc.app.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.riloc.app.common.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * System floating window service providing a screen-level joystick widget
 * over target apps (following LocationJoystick_V4).
 */
class FloatingJoystickService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private var overlayView: FrameLayout? = null
    private var moveEngine: MoveEngine? = null

    companion object {
        const val CHANNEL_ID = "riloc_joystick_channel"
        const val NOTIF_ID = 9001

        fun start(context: Context) {
            val intent = Intent(context, FloatingJoystickService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingJoystickService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        moveEngine = MoveEngine(serviceScope)
        startForegroundNotification()
        createFloatingWidget()
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Riloc 悬浮摇杆",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Riloc 悬浮摇杆")
            .setContentText("实时摇杆控制已在屏幕上悬浮显示")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIF_ID, notification)
    }

    private fun createFloatingWidget() {
        val windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val root = FrameLayout(this)
        val bgDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#E6121318"))
            cornerRadius = 48f
            setStroke(2, Color.parseColor("#33FFFFFF"))
        }
        root.background = bgDrawable

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Header / Drag bar
        val header = TextView(this).apply {
            text = "Riloc 摇杆"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, 0, 0, 16)
        }
        container.addView(header)

        // Touch Joystick Base (Outer Ring)
        val joystickSizePx = 280
        val handleSizePx = 90
        val maxRadius = (joystickSizePx - handleSizePx) / 2f

        val baseView = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33FFFFFF"))
                setStroke(3, Color.parseColor("#666200EE"))
            }
        }
        val baseLp = LinearLayout.LayoutParams(joystickSizePx, joystickSizePx)
        baseView.layoutParams = baseLp

        val handleView = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FFBB86FC"))
            }
        }
        val handleLp = FrameLayout.LayoutParams(handleSizePx, handleSizePx).apply {
            gravity = Gravity.CENTER
        }
        handleView.layoutParams = handleLp

        baseView.addView(handleView)
        container.addView(baseView)

        // Speed control label
        val speedTv = TextView(this).apply {
            text = "速度: 4.0 m/s"
            setTextColor(Color.LTGRAY)
            textSize = 11f
            setPadding(0, 12, 0, 8)
        }
        container.addView(speedTv)

        // Close button
        val closeTv = TextView(this).apply {
            text = "关闭悬浮"
            setTextColor(Color.parseColor("#FF8A80"))
            textSize = 11f
            setPadding(16, 8, 16, 8)
            setOnClickListener {
                stopSelf()
            }
        }
        container.addView(closeTv)

        root.addView(container)

        // Dragging whole window by header
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams.x
                    initialY = windowParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    windowParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    windowParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(root, windowParams)
                    true
                }
                else -> false
            }
        }

        // Joystick deflection handler
        val speedMps = Prefs.speed().let { if (it > 0f) it else 4f }
        moveEngine?.startJoystick(speedMps)

        baseView.setOnTouchListener { _, event ->
            val centerX = baseView.width / 2f
            val centerY = baseView.height / 2f
            val dx = event.x - centerX
            val dy = event.y - centerY
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val clampedDist = dist.coerceAtMost(maxRadius)
                    val angle = atan2(dy.toDouble(), dx.toDouble())
                    val handleX = clampedDist * kotlin.math.cos(angle).toFloat()
                    val handleY = clampedDist * kotlin.math.sin(angle).toFloat()

                    handleView.translationX = handleX
                    handleView.translationY = handleY

                    val normX = handleX / maxRadius
                    val normY = handleY / maxRadius
                    moveEngine?.setJoystickVector(normX, normY)
                    Prefs.setPlaying(true)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handleView.translationX = 0f
                    handleView.translationY = 0f
                    moveEngine?.setJoystickVector(0f, 0f)
                    true
                }
                else -> false
            }
        }

        overlayView = root
        windowManager.addView(root, windowParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        moveEngine?.stop()
        serviceScope.cancel()
        overlayView?.let {
            runCatching { windowManager.removeView(it) }
        }
        overlayView = null
    }
}
