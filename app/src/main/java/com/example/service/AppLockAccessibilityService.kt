package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.example.AppLockApplication
import com.example.engine.LockAction

/**
 * Event-driven foreground monitor using AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED.
 * Operates with canRetrieveWindowContent="false" (never reads screen text or keystrokes)
 * and zero background polling for minimal CPU/RAM/battery footprint.
 */
class AppLockAccessibilityService : AccessibilityService() {

    private var launcherPackages: Set<String> = emptySet()
    private var imePackages: Set<String> = emptySet()
    private var lastCacheRefreshMs: Long = 0L

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                val app = applicationContext as? AppLockApplication ?: return
                app.appLockEngine.onScreenOff()
            }
        }
    }

    private var receiverRegistered = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        ServiceStatusTracker.setConnected(true)
        refreshPackageCaches(force = true)

        if (!receiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            ContextCompat.registerReceiver(
                this,
                screenStateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastCacheRefreshMs > PACKAGE_CACHE_TTL_MS) {
            refreshPackageCaches(force = false)
        }

        val isLauncher = pkg in launcherPackages
        val isTransientSystemUi = pkg in TRANSIENT_SYSTEM_PACKAGES || pkg in imePackages

        val app = applicationContext as? AppLockApplication ?: return
        when (val action = app.appLockEngine.evaluateForegroundTransition(
            packageName = pkg,
            isLauncher = isLauncher,
            isTransientSystemUi = isTransientSystemUi,
            nowMs = nowMs
        )) {
            is LockAction.None -> Unit
            is LockAction.RequireUnlock -> launchLockOverlay(action.packageName)
        }
    }

    override fun onInterrupt() {
        ServiceStatusTracker.setConnected(false)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        ServiceStatusTracker.setConnected(false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        ServiceStatusTracker.setConnected(false)
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenStateReceiver) }
            receiverRegistered = false
        }
        super.onDestroy()
    }

    private fun refreshPackageCaches(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastCacheRefreshMs < PACKAGE_CACHE_TTL_MS) return
        val app = applicationContext as? AppLockApplication ?: return
        launcherPackages = app.installedAppsProvider.getLauncherPackages()
        imePackages = app.installedAppsProvider.getInputMethodPackages()
        lastCacheRefreshMs = now
    }

    private fun launchLockOverlay(targetPackage: String) {
        val intent = LockScreenActivity.createIntent(this, targetPackage).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        runCatching { startActivity(intent) }
    }

    companion object {
        private const val PACKAGE_CACHE_TTL_MS = 30_000L
        private val TRANSIENT_SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "android"
        )
    }
}
