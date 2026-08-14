package com.mohamed.tapcounter

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the foreground. If it's a tracked "game", it starts
 * OverlayService so the tap-counter bubble appears; when you leave that app it
 * hides the bubble again.
 */
class GameDetectAccessibilityService : AccessibilityService() {

    private var lastPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == lastPackage) return
        lastPackage = pkg

        // Ignore our own app and common system launcher/UI packages
        if (pkg == packageName) return

        if (isTrackedGame(pkg)) {
            val intent = Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_SHOW)
            startForegroundService(intent)
        } else {
            val intent = Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_HIDE)
            startService(intent)
        }
    }

    private fun isTrackedGame(pkg: String): Boolean {
        val trackedManually = Prefs.getTrackedPackages(this)
        if (trackedManually.contains(pkg)) return true

        if (Prefs.isAutoAllGames(this)) {
            return try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    override fun onInterrupt() {}
}
