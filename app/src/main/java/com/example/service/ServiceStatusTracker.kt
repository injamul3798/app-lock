package com.example.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceStatusTracker {
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun setConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, AppLockAccessibilityService::class.java)
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledList = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (enabledList != null) {
            val found = enabledList.any { info ->
                val svcInfo = info.resolveInfo?.serviceInfo
                svcInfo != null &&
                    svcInfo.packageName == expectedComponent.packageName &&
                    svcInfo.name == expectedComponent.className
            }
            if (found) return true
        }

        val enabledServices = runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        }.getOrNull().orEmpty()

        val flat = expectedComponent.flattenToString()
        val shortFlat = expectedComponent.flattenToShortString()
        return enabledServices.split(':').any { entry ->
            entry.equals(flat, ignoreCase = true) || entry.equals(shortFlat, ignoreCase = true)
        }
    }
}
