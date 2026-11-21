package com.example.domain.model

enum class LockMethod(val label: String) {
    PATTERN("Pattern Lock"),
    PIN("PIN Code")
}

enum class AutoLockTimeout(
    val durationMs: Long,
    val title: String,
    val summary: String
) {
    IMMEDIATE(0L, "Immediately", "Lock as soon as you leave an app"),
    SECONDS_15(15_000L, "15 seconds", "Lock apps after 15 seconds"),
    SECONDS_30(30_000L, "30 seconds", "Lock apps after 30 seconds"),
    MINUTE_1(60_000L, "1 minute", "Lock apps after 1 minute"),
    MINUTES_5(300_000L, "5 minutes", "Lock apps after 5 minutes"),
    SCREEN_OFF(-1L, "When screen locks", "Stay unlocked until screen turns off")
}

data class HashedCredential(
    val hashBase64: String,
    val saltBase64: String,
    val iterations: Int
)

data class IntrusionEvent(
    val timestampMs: Long,
    val targetPackage: String,
    val targetAppName: String,
    val attemptCount: Int,
    val method: LockMethod
)

data class VaultConfig(
    val onboardingCompleted: Boolean = false,
    val masterProtectionEnabled: Boolean = true,
    val primaryMethod: LockMethod = LockMethod.PATTERN,
    val patternCredential: HashedCredential? = null,
    val pinCredential: HashedCredential? = null,
    val biometricEnabled: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.MINUTE_1,
    val lockOnScreenOff: Boolean = true,
    val screenCaptureProtection: Boolean = true,
    val lockedPackages: Set<String> = emptySet(),
    val intrusionEvents: List<IntrusionEvent> = emptyList(),
    val failedAttempts: Int = 0,
    val cooldownUntilElapsedMs: Long = 0L
) {
    fun hasCredentialFor(method: LockMethod): Boolean = when (method) {
        LockMethod.PATTERN -> patternCredential != null
        LockMethod.PIN -> pinCredential != null
    }

    fun isConfigured(): Boolean =
        onboardingCompleted && (patternCredential != null || pinCredential != null)
}

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val isLocked: Boolean
)

data class ProtectionHealth(
    val accessibilityEnabled: Boolean,
    val serviceConnected: Boolean,
    val biometricHardwareSupported: Boolean,
    val biometricEnrolled: Boolean,
    val ignoringBatteryOptimizations: Boolean,
    val lockedAppCount: Int,
    val masterProtectionEnabled: Boolean
) {
    val isFullyProtected: Boolean
        get() = masterProtectionEnabled && accessibilityEnabled && lockedAppCount > 0
}

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failed(val attemptCount: Int, val cooldownSeconds: Int) : AuthResult
    data class LockedOut(val remainingSeconds: Int) : AuthResult
}
