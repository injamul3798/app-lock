package com.example.engine

import com.example.domain.model.AutoLockTimeout

sealed interface LockAction {
    data object None : LockAction
    data class RequireUnlock(val packageName: String) : LockAction
}

/**
 * Deterministic, testable app-locking state machine.
 * Tracks unlocked app sessions, grace timeouts, screen-off relocking,
 * launcher transitions, and duplicate-launch debouncing without continuous polling.
 */
class AppLockEngine(
    private val ownPackageName: String,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private data class UnlockedSession(
        val unlockedAtMs: Long,
        var backgroundedAtMs: Long? = null
    )

    private val lock = Any()

    private var masterEnabled: Boolean = true
    private var lockedPackages: Set<String> = emptySet()
    private var autoLockTimeout: AutoLockTimeout = AutoLockTimeout.MINUTE_1
    private var lockOnScreenOff: Boolean = true

    private val unlockedSessions = mutableMapOf<String, UnlockedSession>()
    private var activeForegroundPackage: String? = null
    private var lastLockLaunchPkg: String? = null
    private var lastLockLaunchAtMs: Long = 0L

    fun updatePolicy(
        masterEnabled: Boolean,
        lockedPackages: Set<String>,
        autoLockTimeout: AutoLockTimeout,
        lockOnScreenOff: Boolean
    ) = synchronized(lock) {
        this.masterEnabled = masterEnabled
        this.lockedPackages = lockedPackages
        this.autoLockTimeout = autoLockTimeout
        this.lockOnScreenOff = lockOnScreenOff
        if (!masterEnabled) {
            unlockedSessions.clear()
            lastLockLaunchPkg = null
        } else {
            unlockedSessions.keys.retainAll(lockedPackages)
        }
    }

    fun evaluateForegroundTransition(
        packageName: String,
        isLauncher: Boolean,
        isTransientSystemUi: Boolean,
        nowMs: Long = clockMs()
    ): LockAction = synchronized(lock) {
        if (!masterEnabled || packageName.isBlank()) {
            return LockAction.None
        }

        // Ignore our own app & overlay and transient overlays (IME keyboard, permission dialogs, system UI)
        if (packageName == ownPackageName || isTransientSystemUi) {
            return LockAction.None
        }

        val previousPkg = activeForegroundPackage
        if (previousPkg != null && previousPkg != packageName && previousPkg in lockedPackages) {
            handleProtectedAppBackgrounded(previousPkg, nowMs)
        }

        activeForegroundPackage = packageName

        if (isLauncher || packageName !in lockedPackages) {
            return LockAction.None
        }

        // Entering a protected app
        val session = unlockedSessions[packageName]
        if (session != null) {
            val bgAt = session.backgroundedAtMs
            val isStillValid = when (autoLockTimeout) {
                AutoLockTimeout.IMMEDIATE -> {
                    // Only valid if we never actually left this protected app
                    bgAt == null
                }
                AutoLockTimeout.SCREEN_OFF -> {
                    true
                }
                else -> {
                    bgAt == null || (nowMs - bgAt) < autoLockTimeout.durationMs
                }
            }
            if (isStillValid) {
                session.backgroundedAtMs = null
                return LockAction.None
            } else {
                unlockedSessions.remove(packageName)
            }
        }

        // Debounce duplicate launches while LockScreenActivity is already animating into foreground
        if (lastLockLaunchPkg == packageName && (nowMs - lastLockLaunchAtMs) < LAUNCH_COOLDOWN_MS) {
            return LockAction.None
        }

        lastLockLaunchPkg = packageName
        lastLockLaunchAtMs = nowMs
        return LockAction.RequireUnlock(packageName)
    }

    fun onUnlockSucceeded(packageName: String, nowMs: Long = clockMs()) = synchronized(lock) {
        if (packageName in lockedPackages) {
            unlockedSessions[packageName] = UnlockedSession(unlockedAtMs = nowMs, backgroundedAtMs = null)
        }
        if (lastLockLaunchPkg == packageName) {
            lastLockLaunchPkg = null
            lastLockLaunchAtMs = 0L
        }
        activeForegroundPackage = packageName
    }

    fun onUnlockCancelled(packageName: String) = synchronized(lock) {
        unlockedSessions.remove(packageName)
        if (lastLockLaunchPkg == packageName) {
            lastLockLaunchPkg = null
            lastLockLaunchAtMs = 0L
        }
        activeForegroundPackage = null
    }

    fun onScreenOff() = synchronized(lock) {
        if (lockOnScreenOff || autoLockTimeout == AutoLockTimeout.SCREEN_OFF) {
            unlockedSessions.clear()
        }
        activeForegroundPackage = null
        lastLockLaunchPkg = null
        lastLockLaunchAtMs = 0L
    }

    fun relockAll() = synchronized(lock) {
        unlockedSessions.clear()
        lastLockLaunchPkg = null
        lastLockLaunchAtMs = 0L
    }

    fun isCurrentlyUnlocked(packageName: String, nowMs: Long = clockMs()): Boolean = synchronized(lock) {
        val session = unlockedSessions[packageName] ?: return false
        val bgAt = session.backgroundedAtMs ?: return true
        return when (autoLockTimeout) {
            AutoLockTimeout.IMMEDIATE -> false
            AutoLockTimeout.SCREEN_OFF -> true
            else -> (nowMs - bgAt) < autoLockTimeout.durationMs
        }
    }

    private fun handleProtectedAppBackgrounded(previousPkg: String, nowMs: Long) {
        if (autoLockTimeout == AutoLockTimeout.IMMEDIATE) {
            unlockedSessions.remove(previousPkg)
        } else {
            unlockedSessions[previousPkg]?.let { session ->
                if (session.backgroundedAtMs == null) {
                    session.backgroundedAtMs = nowMs
                }
            }
        }
    }

    companion object {
        private const val LAUNCH_COOLDOWN_MS = 1_200L
    }
}
