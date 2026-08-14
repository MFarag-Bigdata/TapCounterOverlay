package com.mohamed.tapcounter

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

/**
 * Draws a small draggable floating bubble on top of the current app (the game).
 * Tapping the bubble increments the counter. It cannot see taps elsewhere on
 * screen -- Android does not allow any app to observe touches delivered to a
 * different app's window, so this bubble IS the counter button.
 */
class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "com.mohamed.tapcounter.SHOW"
        const val ACTION_HIDE = "com.mohamed.tapcounter.HIDE"
        const val ACTION_RESET = "com.mohamed.tapcounter.RESET"
        private const val CHANNEL_ID = "tap_counter_channel"
        private const val NOTIF_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var tapCount = 0
    private var sessionStartMs = 0L

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showBubble()
            ACTION_HIDE -> hideBubble()
            ACTION_RESET -> {
                tapCount = 0
                updateCountText()
            }
        }
        return START_STICKY
    }

    private fun showBubble() {
        if (bubbleView != null) return // already showing

        tapCount = 0
        sessionStartMs = System.currentTimeMillis()

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_counter, null)
        bubbleView = view

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 300

        windowManager.addView(view, params)

        // Drag-to-move + tap-to-count, distinguished by movement distance
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) {
                        moved = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        tapCount++
                        updateCountText()
                    }
                    true
                }
                else -> false
            }
        }

        updateCountText()
    }

    private fun updateCountText() {
        val view = bubbleView ?: return
        view.findViewById<TextView>(R.id.tvCount).text = tapCount.toString()

        val elapsedSec = ((System.currentTimeMillis() - sessionStartMs) / 1000.0).coerceAtLeast(0.5)
        val rate = tapCount / elapsedSec
        view.findViewById<TextView>(R.id.tvRate).text = String.format("%.1f tps", rate)
    }

    private fun hideBubble() {
        bubbleView?.let {
            windowManager.removeView(it)
            bubbleView = null
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Tap Counter", NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tap Counter Overlay active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        hideBubble()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
