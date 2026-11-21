package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AppLockApplication

/**
 * Ensures the engine relocks all active sessions cleanly after device restart or app upgrade.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as? AppLockApplication ?: return
            app.appLockEngine.relockAll()
        }
    }
}
