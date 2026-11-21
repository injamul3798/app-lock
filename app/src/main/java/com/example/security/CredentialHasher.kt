package com.example.security

import com.example.domain.model.HashedCredential
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic credential hashing & verification using salted PBKDF2-HMAC-SHA256.
 * Never persists raw PINs or patterns and zeroes transient buffers after use.
 */
object CredentialHasher {
    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_LENGTH_BITS = 256
    private const val MIN_PATTERN_NODES = 4
    private const val MIN_PIN_LENGTH = 4
    private const val MAX_PIN_LENGTH = 6

    private val secureRandom = SecureRandom()

    fun isValidPattern(nodes: List<Int>): Boolean {
        if (nodes.size < MIN_PATTERN_NODES) return false
        if (nodes.any { it !in 0..8 }) return false
        return nodes.distinct().size == nodes.size
    }

    fun formatPattern(nodes: List<Int>): CharArray =
        nodes.joinToString(separator = "-").toCharArray()

    fun isValidPin(pin: String): Boolean =
        pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }

    fun hashCredential(rawChars: CharArray): HashedCredential {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val derived = deriveKey(rawChars, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            HashedCredential(
                hashBase64 = bytesToHex(derived),
                saltBase64 = bytesToHex(salt),
                iterations = ITERATIONS
            )
        } finally {
            derived.fill(0)
            rawChars.fill('\u0000')
        }
    }

    fun verifyCredential(rawChars: CharArray, stored: HashedCredential): Boolean {
        val salt = hexToBytes(stored.saltBase64)
        val expectedHash = hexToBytes(stored.hashBase64)
        val candidate = deriveKey(rawChars, salt, stored.iterations, expectedHash.size * 8)
        return try {
            MessageDigest.isEqual(candidate, expectedHash)
        } finally {
            candidate.fill(0)
            expectedHash.fill(0)
            rawChars.fill('\u0000')
        }
    }

    private fun deriveKey(
        chars: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBits: Int
    ): ByteArray {
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(chars, salt, iterations, keyLengthBits)
            try {
                factory.generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        } catch (_: NoSuchAlgorithmException) {
            pbkdf2HmacSha256Fallback(chars, salt, iterations, keyLengthBits / 8)
        }
    }

    private fun pbkdf2HmacSha256Fallback(
        chars: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyLenBytes: Int
    ): ByteArray {
        val passwordByteBuf = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars))
        val passwordBytes = ByteArray(passwordByteBuf.remaining())
        passwordByteBuf.get(passwordBytes)
        if (passwordByteBuf.hasArray()) {
            passwordByteBuf.array().fill(0)
        }

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(passwordBytes, "HmacSHA256"))
        passwordBytes.fill(0)

        val hashLen = mac.macLength
        val blockCount = (keyLenBytes + hashLen - 1) / hashLen
        val output = ByteArray(blockCount * hashLen)
        val intBuf = ByteBuffer.allocate(4)

        for (block in 1..blockCount) {
            intBuf.clear()
            intBuf.putInt(block)
            mac.reset()
            mac.update(salt)
            mac.update(intBuf.array())
            var u = mac.doFinal()
            val t = u.copyOf()
            for (i in 2..iterations) {
                mac.reset()
                u = mac.doFinal(u)
                for (j in t.indices) {
                    t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
                }
            }
            System.arraycopy(t, 0, output, (block - 1) * hashLen, hashLen)
        }
        return output.copyOf(keyLenBytes)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val out = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val hi = Character.digit(hex[i], 16)
            val lo = Character.digit(hex[i + 1], 16)
            out[i / 2] = ((hi shl 4) + lo).toByte()
            i += 2
        }
        return out
    }
}
