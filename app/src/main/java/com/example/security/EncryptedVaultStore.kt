package com.example.security

import com.example.domain.model.AutoLockTimeout
import com.example.domain.model.HashedCredential
import com.example.domain.model.IntrusionEvent
import com.example.domain.model.LockMethod
import com.example.domain.model.VaultConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Atomically persists minimal security configuration encrypted with AES-256-GCM
 * using a key managed by Android Keystore.
 */
class EncryptedVaultStore(
    private val vaultFile: File,
    private val keyProvider: KeyProvider
) {
    private val lock = Any()

    fun load(): VaultConfig = synchronized(lock) {
        if (!vaultFile.exists()) {
            return VaultConfig()
        }
        return try {
            val payload = vaultFile.readBytes()
            if (payload.size <= IV_LENGTH_BYTES) {
                return VaultConfig()
            }
            val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
            val cipherText = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                keyProvider.getOrCreateKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, iv)
            )
            val plainBytes = cipher.doFinal(cipherText)
            val jsonStr = String(plainBytes, StandardCharsets.UTF_8)
            plainBytes.fill(0)
            decodeConfig(JSONObject(jsonStr))
        } catch (_: Exception) {
            // Fail closed without exposing corrupted data or crashing
            VaultConfig()
        }
    }

    fun save(config: VaultConfig) = synchronized(lock) {
        vaultFile.parentFile?.mkdirs()
        val jsonBytes = encodeConfig(config).toString().toByteArray(StandardCharsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider.getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(jsonBytes)
        jsonBytes.fill(0)

        val output = ByteBuffer.allocate(iv.size + cipherText.size)
            .put(iv)
            .put(cipherText)
            .array()

        val tempFile = File(vaultFile.parentFile, "${vaultFile.name}.tmp")
        tempFile.writeBytes(output)
        output.fill(0)
        if (!tempFile.renameTo(vaultFile)) {
            vaultFile.writeBytes(tempFile.readBytes())
            tempFile.delete()
        }
    }

    private fun encodeConfig(config: VaultConfig): JSONObject {
        val root = JSONObject()
        root.put("version", VAULT_SCHEMA_VERSION)
        root.put("onboardingCompleted", config.onboardingCompleted)
        root.put("masterProtectionEnabled", config.masterProtectionEnabled)
        root.put("primaryMethod", config.primaryMethod.name)
        root.put("biometricEnabled", config.biometricEnabled)
        root.put("autoLockTimeout", config.autoLockTimeout.name)
        root.put("lockOnScreenOff", config.lockOnScreenOff)
        root.put("screenCaptureProtection", config.screenCaptureProtection)
        root.put("failedAttempts", config.failedAttempts)
        root.put("cooldownUntilElapsedMs", config.cooldownUntilElapsedMs)

        config.patternCredential?.let { root.put("patternCredential", encodeCredential(it)) }
        config.pinCredential?.let { root.put("pinCredential", encodeCredential(it)) }

        val packagesArray = JSONArray()
        config.lockedPackages.sorted().forEach { packagesArray.put(it) }
        root.put("lockedPackages", packagesArray)

        val eventsArray = JSONArray()
        config.intrusionEvents.take(MAX_INTRUSION_EVENTS).forEach { event ->
            eventsArray.put(
                JSONObject().apply {
                    put("timestampMs", event.timestampMs)
                    put("targetPackage", event.targetPackage)
                    put("targetAppName", event.targetAppName)
                    put("attemptCount", event.attemptCount)
                    put("method", event.method.name)
                }
            )
        }
        root.put("intrusionEvents", eventsArray)
        return root
    }

    private fun decodeConfig(root: JSONObject): VaultConfig {
        val primaryMethod = root.optString("primaryMethod", LockMethod.PATTERN.name)
            .let { runCatching { LockMethod.valueOf(it) }.getOrDefault(LockMethod.PATTERN) }

        val timeout = root.optString("autoLockTimeout", AutoLockTimeout.MINUTE_1.name)
            .let { runCatching { AutoLockTimeout.valueOf(it) }.getOrDefault(AutoLockTimeout.MINUTE_1) }

        val lockedPackages = buildSet {
            val arr = root.optJSONArray("lockedPackages") ?: return@buildSet
            for (i in 0 until arr.length()) {
                val pkg = arr.optString(i)
                if (pkg.isNotBlank()) add(pkg)
            }
        }

        val intrusionEvents = buildList {
            val arr = root.optJSONArray("intrusionEvents") ?: return@buildList
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val method = obj.optString("method", LockMethod.PATTERN.name)
                    .let { runCatching { LockMethod.valueOf(it) }.getOrDefault(LockMethod.PATTERN) }
                add(
                    IntrusionEvent(
                        timestampMs = obj.optLong("timestampMs", 0L),
                        targetPackage = obj.optString("targetPackage", ""),
                        targetAppName = obj.optString("targetAppName", "Protected App"),
                        attemptCount = obj.optInt("attemptCount", 1),
                        method = method
                    )
                )
            }
        }

        return VaultConfig(
            onboardingCompleted = root.optBoolean("onboardingCompleted", false),
            masterProtectionEnabled = root.optBoolean("masterProtectionEnabled", true),
            primaryMethod = primaryMethod,
            patternCredential = root.optJSONObject("patternCredential")?.let(::decodeCredential),
            pinCredential = root.optJSONObject("pinCredential")?.let(::decodeCredential),
            biometricEnabled = root.optBoolean("biometricEnabled", false),
            autoLockTimeout = timeout,
            lockOnScreenOff = root.optBoolean("lockOnScreenOff", true),
            screenCaptureProtection = root.optBoolean("screenCaptureProtection", true),
            lockedPackages = lockedPackages,
            intrusionEvents = intrusionEvents,
            failedAttempts = root.optInt("failedAttempts", 0),
            cooldownUntilElapsedMs = root.optLong("cooldownUntilElapsedMs", 0L)
        )
    }

    private fun encodeCredential(cred: HashedCredential): JSONObject =
        JSONObject().apply {
            put("hash", cred.hashBase64)
            put("salt", cred.saltBase64)
            put("iterations", cred.iterations)
        }

    private fun decodeCredential(obj: JSONObject): HashedCredential? {
        val hash = obj.optString("hash", "")
        val salt = obj.optString("salt", "")
        val iterations = obj.optInt("iterations", 0)
        if (hash.isBlank() || salt.isBlank() || iterations <= 0) return null
        return HashedCredential(hashBase64 = hash, saltBase64 = salt, iterations = iterations)
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
        private const val VAULT_SCHEMA_VERSION = 1
        private const val MAX_INTRUSION_EVENTS = 50
    }
}
