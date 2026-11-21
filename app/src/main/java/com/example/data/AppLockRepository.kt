package com.example.data

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import com.example.domain.model.AuthResult
import com.example.domain.model.AutoLockTimeout
import com.example.domain.model.IntrusionEvent
import com.example.domain.model.LockMethod
import com.example.domain.model.ProtectionHealth
import com.example.domain.model.VaultConfig
import com.example.engine.AppLockEngine
import com.example.security.BiometricHelper
import com.example.security.CredentialHasher
import com.example.security.EncryptedVaultStore
import com.example.service.ServiceStatusTracker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppLockRepository(
    private val vaultStore: EncryptedVaultStore,
    private val engine: AppLockEngine,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val elapsedRealtimeMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val wallClockMs: () -> Long = { System.currentTimeMillis() }
) {
    private val _config = MutableStateFlow(VaultConfig())
    val config: StateFlow<VaultConfig> = _config.asStateFlow()

    init {
        val loaded = vaultStore.load()
        _config.value = loaded
        syncEngine(loaded)
    }

    suspend fun completeInitialSetup(
        method: LockMethod,
        rawCredentialChars: CharArray,
        biometricEnabled: Boolean,
        lockedPackages: Set<String>
    ) = withContext(ioDispatcher) {
        val hashed = CredentialHasher.hashCredential(rawCredentialChars)
        val current = _config.value
        val updated = current.copy(
            onboardingCompleted = true,
            masterProtectionEnabled = true,
            primaryMethod = method,
            patternCredential = if (method == LockMethod.PATTERN) hashed else current.patternCredential,
            pinCredential = if (method == LockMethod.PIN) hashed else current.pinCredential,
            biometricEnabled = biometricEnabled,
            lockedPackages = lockedPackages,
            failedAttempts = 0,
            cooldownUntilElapsedMs = 0L
        )
        persist(updated)
    }

    suspend fun updateCredential(
        method: LockMethod,
        rawCredentialChars: CharArray,
        makePrimary: Boolean = false
    ) = withContext(ioDispatcher) {
        val hashed = CredentialHasher.hashCredential(rawCredentialChars)
        val current = _config.value
        val updated = current.copy(
            primaryMethod = if (makePrimary) method else current.primaryMethod,
            patternCredential = if (method == LockMethod.PATTERN) hashed else current.patternCredential,
            pinCredential = if (method == LockMethod.PIN) hashed else current.pinCredential,
            failedAttempts = 0,
            cooldownUntilElapsedMs = 0L
        )
        persist(updated)
    }

    suspend fun verifyCredential(
        method: LockMethod,
        rawCredentialChars: CharArray,
        targetPackage: String,
        targetAppName: String
    ): AuthResult = withContext(ioDispatcher) {
        val current = _config.value
        val nowElapsed = elapsedRealtimeMs()

        val remainingCooldown = computeRemainingCooldownSeconds(current, nowElapsed)
        if (remainingCooldown > 0) {
            rawCredentialChars.fill('\u0000')
            return@withContext AuthResult.LockedOut(remainingCooldown)
        }

        val stored = when (method) {
            LockMethod.PATTERN -> current.patternCredential
            LockMethod.PIN -> current.pinCredential
        }

        if (stored == null) {
            rawCredentialChars.fill('\u0000')
            return@withContext AuthResult.Failed(attemptCount = current.failedAttempts + 1, cooldownSeconds = 0)
        }

        val isMatch = CredentialHasher.verifyCredential(rawCredentialChars, stored)
        if (isMatch) {
            if (current.failedAttempts != 0 || current.cooldownUntilElapsedMs != 0L) {
                persist(current.copy(failedAttempts = 0, cooldownUntilElapsedMs = 0L))
            }
            return@withContext AuthResult.Success
        }

        val newAttempts = current.failedAttempts + 1
        val cooldownSec = cooldownSecondsForAttempts(newAttempts)
        val cooldownUntil = if (cooldownSec > 0) nowElapsed + (cooldownSec * 1_000L) else 0L

        val event = IntrusionEvent(
            timestampMs = wallClockMs(),
            targetPackage = targetPackage,
            targetAppName = targetAppName,
            attemptCount = newAttempts,
            method = method
        )

        val updated = current.copy(
            failedAttempts = newAttempts,
            cooldownUntilElapsedMs = cooldownUntil,
            intrusionEvents = (listOf(event) + current.intrusionEvents).take(MAX_INTRUSION_LOG)
        )
        persist(updated)

        if (cooldownSec > 0) {
            AuthResult.Failed(attemptCount = newAttempts, cooldownSeconds = cooldownSec)
        } else {
            AuthResult.Failed(attemptCount = newAttempts, cooldownSeconds = 0)
        }
    }

    suspend fun onBiometricUnlockSucceeded() = withContext(ioDispatcher) {
        val current = _config.value
        if (current.failedAttempts != 0 || current.cooldownUntilElapsedMs != 0L) {
            persist(current.copy(failedAttempts = 0, cooldownUntilElapsedMs = 0L))
        }
    }

    fun getRemainingCooldownSeconds(): Int =
        computeRemainingCooldownSeconds(_config.value, elapsedRealtimeMs())

    suspend fun setPrimaryMethod(method: LockMethod) = withContext(ioDispatcher) {
        val current = _config.value
        if (current.hasCredentialFor(method)) {
            persist(current.copy(primaryMethod = method))
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        persist(_config.value.copy(biometricEnabled = enabled))
    }

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) = withContext(ioDispatcher) {
        persist(_config.value.copy(autoLockTimeout = timeout))
    }

    suspend fun setLockOnScreenOff(enabled: Boolean) = withContext(ioDispatcher) {
        persist(_config.value.copy(lockOnScreenOff = enabled))
    }

    suspend fun setScreenCaptureProtection(enabled: Boolean) = withContext(ioDispatcher) {
        persist(_config.value.copy(screenCaptureProtection = enabled))
    }

    suspend fun setMasterProtectionEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        persist(_config.value.copy(masterProtectionEnabled = enabled))
    }

    suspend fun setAppLocked(packageName: String, locked: Boolean) = withContext(ioDispatcher) {
        val current = _config.value
        val updatedSet = if (locked) {
            current.lockedPackages + packageName
        } else {
            current.lockedPackages - packageName
        }
        persist(current.copy(lockedPackages = updatedSet))
    }

    suspend fun setLockedPackages(packages: Set<String>) = withContext(ioDispatcher) {
        persist(_config.value.copy(lockedPackages = packages))
    }

    suspend fun clearIntrusionEvents() = withContext(ioDispatcher) {
        persist(_config.value.copy(intrusionEvents = emptyList(), failedAttempts = 0, cooldownUntilElapsedMs = 0L))
    }

    fun checkProtectionHealth(context: Context): ProtectionHealth {
        val cfg = _config.value
        val accessibilityEnabled = ServiceStatusTracker.isAccessibilityServiceEnabled(context)
        val serviceConnected = ServiceStatusTracker.isServiceConnected.value
        val biometric = BiometricHelper.checkCapability(context)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ignoringBatteryOpt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        return ProtectionHealth(
            accessibilityEnabled = accessibilityEnabled,
            serviceConnected = serviceConnected,
            biometricHardwareSupported = biometric.hardwareSupported,
            biometricEnrolled = biometric.enrolled,
            ignoringBatteryOptimizations = ignoringBatteryOpt,
            lockedAppCount = cfg.lockedPackages.size,
            masterProtectionEnabled = cfg.masterProtectionEnabled
        )
    }

    private fun persist(updated: VaultConfig) {
        vaultStore.save(updated)
        _config.value = updated
        syncEngine(updated)
    }

    private fun syncEngine(cfg: VaultConfig) {
        engine.updatePolicy(
            masterEnabled = cfg.onboardingCompleted && cfg.masterProtectionEnabled,
            lockedPackages = cfg.lockedPackages,
            autoLockTimeout = cfg.autoLockTimeout,
            lockOnScreenOff = cfg.lockOnScreenOff
        )
    }

    private fun computeRemainingCooldownSeconds(cfg: VaultConfig, nowElapsed: Long): Int {
        if (cfg.cooldownUntilElapsedMs <= 0L) return 0
        val diffMs = cfg.cooldownUntilElapsedMs - nowElapsed
        // Guard against stale elapsedRealtime values after a device reboot
        if (diffMs <= 0L || diffMs > MAX_COOLDOWN_MS) return 0
        return ((diffMs + 999L) / 1_000L).toInt()
    }

    private fun cooldownSecondsForAttempts(attempts: Int): Int = when {
        attempts >= 10 -> 300
        attempts >= 8 -> 60
        attempts >= 5 -> 30
        else -> 0
    }

    companion object {
        private const val MAX_COOLDOWN_MS = 300_000L
        private const val MAX_INTRUSION_LOG = 50
    }
}
