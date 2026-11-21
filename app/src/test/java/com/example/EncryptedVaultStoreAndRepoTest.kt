package com.example

import com.example.data.AppLockRepository
import com.example.domain.model.AuthResult
import com.example.domain.model.AutoLockTimeout
import com.example.domain.model.LockMethod
import com.example.engine.AppLockEngine
import com.example.security.EncryptedVaultStore
import com.example.security.KeyProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EncryptedVaultStoreAndRepoTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class TestAesKeyProvider : KeyProvider {
        private val key: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        override fun getOrCreateKey(): SecretKey = key
    }

    @Test
    fun `vault encrypts data on disk and fails closed on tampering`() = runTest {
        val vaultFile = File(tempFolder.root, "test_vault.enc")
        val keyProvider = TestAesKeyProvider()
        val store = EncryptedVaultStore(vaultFile, keyProvider)
        val engine = AppLockEngine("com.aistudio.applock.vzkmqr")
        val repo = AppLockRepository(store, engine)

        repo.completeInitialSetup(
            method = LockMethod.PIN,
            rawCredentialChars = "4321".toCharArray(),
            biometricEnabled = false,
            lockedPackages = setOf("com.whatsapp", "com.android.chrome")
        )
        repo.setAutoLockTimeout(AutoLockTimeout.SECONDS_30)

        // Plaintext PIN must never appear in encrypted vault file
        val rawBytes = vaultFile.readBytes().toString(Charsets.UTF_8)
        assertFalse(rawBytes.contains("4321"))
        assertFalse(rawBytes.contains("com.whatsapp"))

        // Reload with same key recovers config accurately
        val reloaded = EncryptedVaultStore(vaultFile, keyProvider).load()
        assertTrue(reloaded.onboardingCompleted)
        assertEquals(AutoLockTimeout.SECONDS_30, reloaded.autoLockTimeout)
        assertEquals(setOf("com.whatsapp", "com.android.chrome"), reloaded.lockedPackages)

        // Tampering ciphertext causes fail-closed empty default without crash
        val corrupted = vaultFile.readBytes()
        corrupted[corrupted.lastIndex] = (corrupted.last().toInt() xor 0xFF).toByte()
        vaultFile.writeBytes(corrupted)
        val tamperedLoad = EncryptedVaultStore(vaultFile, keyProvider).load()
        assertFalse(tamperedLoad.onboardingCompleted)
        assertTrue(tamperedLoad.lockedPackages.isEmpty())
    }

    @Test
    fun `failed attempts log local intrusion events and enforce cooldown after 5 failures`() = runTest {
        var fakeElapsed = 5_000L
        var fakeWall = 1_700_000_000_000L
        val vaultFile = File(tempFolder.root, "cooldown_vault.enc")
        val store = EncryptedVaultStore(vaultFile, TestAesKeyProvider())
        val engine = AppLockEngine("com.aistudio.applock.vzkmqr")
        val repo = AppLockRepository(
            vaultStore = store,
            engine = engine,
            elapsedRealtimeMs = { fakeElapsed },
            wallClockMs = { fakeWall }
        )

        repo.completeInitialSetup(
            method = LockMethod.PIN,
            rawCredentialChars = "9876".toCharArray(),
            biometricEnabled = false,
            lockedPackages = setOf("com.whatsapp")
        )

        // 4 wrong attempts -> no cooldown yet
        repeat(4) { idx ->
            fakeWall += 1_000L
            val res = repo.verifyCredential(LockMethod.PIN, "0000".toCharArray(), "com.whatsapp", "WhatsApp")
            assertEquals(AuthResult.Failed(attemptCount = idx + 1, cooldownSeconds = 0), res)
        }

        // 5th wrong attempt triggers 30s cooldown
        fakeWall += 1_000L
        val fifth = repo.verifyCredential(LockMethod.PIN, "0000".toCharArray(), "com.whatsapp", "WhatsApp")
        assertEquals(AuthResult.Failed(attemptCount = 5, cooldownSeconds = 30), fifth)
        assertEquals(5, repo.config.value.intrusionEvents.size)

        // Attempting even correct PIN during cooldown returns LockedOut
        fakeElapsed += 10_000L
        val duringCooldown = repo.verifyCredential(LockMethod.PIN, "9876".toCharArray(), "com.whatsapp", "WhatsApp")
        assertEquals(AuthResult.LockedOut(remainingSeconds = 20), duringCooldown)

        // After 30s passes, correct PIN succeeds and resets counter
        fakeElapsed += 21_000L
        val afterCooldown = repo.verifyCredential(LockMethod.PIN, "9876".toCharArray(), "com.whatsapp", "WhatsApp")
        assertEquals(AuthResult.Success, afterCooldown)
        assertEquals(0, repo.config.value.failedAttempts)
    }
}
