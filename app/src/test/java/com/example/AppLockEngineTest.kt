package com.example

import com.example.domain.model.AutoLockTimeout
import com.example.engine.AppLockEngine
import com.example.engine.LockAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockEngineTest {

    private var fakeNowMs = 10_000L
    private val ownPkg = "com.aistudio.applock.vzkmqr"
    private val protectedPkg = "com.whatsapp"
    private val otherPkg = "com.android.chrome"
    private val launcherPkg = "com.google.android.apps.nexuslauncher"

    private fun createEngine(timeout: AutoLockTimeout = AutoLockTimeout.IMMEDIATE): AppLockEngine {
        return AppLockEngine(ownPackageName = ownPkg) { fakeNowMs }.apply {
            updatePolicy(
                masterEnabled = true,
                lockedPackages = setOf(protectedPkg),
                autoLockTimeout = timeout,
                lockOnScreenOff = true
            )
        }
    }

    @Test
    fun `opening locked app requires unlock and ignores duplicate burst within cooldown`() {
        val engine = createEngine()

        val first = engine.evaluateForegroundTransition(protectedPkg, isLauncher = false, isTransientSystemUi = false)
        assertEquals(LockAction.RequireUnlock(protectedPkg), first)

        fakeNowMs += 300L
        val duplicate = engine.evaluateForegroundTransition(protectedPkg, isLauncher = false, isTransientSystemUi = false)
        assertEquals(LockAction.None, duplicate)
    }

    @Test
    fun `transient system UI and keyboard do not trigger immediate relock`() {
        val engine = createEngine(AutoLockTimeout.IMMEDIATE)
        engine.onUnlockSucceeded(protectedPkg, fakeNowMs)

        // System UI notification shade / IME keyboard opens over protected app
        val shade = engine.evaluateForegroundTransition("com.android.systemui", isLauncher = false, isTransientSystemUi = true)
        assertEquals(LockAction.None, shade)

        fakeNowMs += 2_000L
        val returnToApp = engine.evaluateForegroundTransition(protectedPkg, isLauncher = false, isTransientSystemUi = false)
        assertEquals(LockAction.None, returnToApp)
        assertTrue(engine.isCurrentlyUnlocked(protectedPkg, fakeNowMs))
    }

    @Test
    fun `immediate timeout relocks when switching to launcher or another app`() {
        val engine = createEngine(AutoLockTimeout.IMMEDIATE)
        engine.onUnlockSucceeded(protectedPkg, fakeNowMs)

        // User goes home
        engine.evaluateForegroundTransition(launcherPkg, isLauncher = true, isTransientSystemUi = false)
        assertFalse(engine.isCurrentlyUnlocked(protectedPkg, fakeNowMs))

        // Re-opening protected app requires unlock again
        fakeNowMs += 500L
        val relaunch = engine.evaluateForegroundTransition(protectedPkg, isLauncher = false, isTransientSystemUi = false)
        assertEquals(LockAction.RequireUnlock(protectedPkg), relaunch)
    }

    @Test
    fun `timed grace period keeps app unlocked until timeout expires`() {
        val engine = createEngine(AutoLockTimeout.SECONDS_15)
        engine.onUnlockSucceeded(protectedPkg, fakeNowMs)

        // Switch to Chrome
        engine.evaluateForegroundTransition(otherPkg, isLauncher = false, isTransientSystemUi = false)

        // Return after 10s -> still unlocked
        fakeNowMs += 10_000L
        assertEquals(
            LockAction.None,
            engine.evaluateForegroundTransition(protectedPkg, isLauncher = false, isTransientSystemUi = false)
        )

        // Leave again and return after 16s -> requires unlock
        engine.evaluateForegroundTransition(launcherPkg, isLauncher = true, isTransientSystemUi = false)
        fakeNowMs += 16_000L
        assertEquals(
            LockAction.RequireUnlock(protectedPkg),
            engine.evaluateForegroundTransition(protectedPkg, isLauncher = false, isTransientSystemUi = false)
        )
    }

    @Test
    fun `screen off immediately clears all unlocked sessions`() {
        val engine = createEngine(AutoLockTimeout.MINUTES_5)
        engine.onUnlockSucceeded(protectedPkg, fakeNowMs)
        assertTrue(engine.isCurrentlyUnlocked(protectedPkg, fakeNowMs))

        engine.onScreenOff()
        assertFalse(engine.isCurrentlyUnlocked(protectedPkg, fakeNowMs))

        fakeNowMs += 2_000L
        assertEquals(
            LockAction.RequireUnlock(protectedPkg),
            engine.evaluateForegroundTransition(protectedPkg, isLauncher = false, isTransientSystemUi = false)
        )
    }
}
