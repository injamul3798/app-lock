package com.example

import com.example.security.CredentialHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialHasherTest {

    @Test
    fun `hash and verify pattern succeeds with matching input and rejects mismatch`() {
        val pattern = listOf(0, 3, 6, 7, 8)
        assertTrue(CredentialHasher.isValidPattern(pattern))

        val stored = CredentialHasher.hashCredential(CredentialHasher.formatPattern(pattern))

        assertTrue(CredentialHasher.verifyCredential(CredentialHasher.formatPattern(pattern), stored))
        assertFalse(CredentialHasher.verifyCredential(CredentialHasher.formatPattern(listOf(0, 3, 6, 7, 5)), stored))
    }

    @Test
    fun `identical credentials produce unique random salts and hashes`() {
        val first = CredentialHasher.hashCredential("1234".toCharArray())
        val second = CredentialHasher.hashCredential("1234".toCharArray())

        assertNotEquals(first.saltBase64, second.saltBase64)
        assertNotEquals(first.hashBase64, second.hashBase64)
    }

    @Test
    fun `hashCredential and verifyCredential zero out input char arrays`() {
        val setupChars = "567890".toCharArray()
        val stored = CredentialHasher.hashCredential(setupChars)
        assertTrue(setupChars.all { it == '\u0000' })

        val verifyChars = "567890".toCharArray()
        assertTrue(CredentialHasher.verifyCredential(verifyChars, stored))
        assertTrue(verifyChars.all { it == '\u0000' })
    }

    @Test
    fun `pattern and pin validation enforce minimum constraints`() {
        assertFalse(CredentialHasher.isValidPattern(listOf(0, 1, 2)))
        assertFalse(CredentialHasher.isValidPattern(listOf(0, 1, 2, 2)))
        assertFalse(CredentialHasher.isValidPattern(listOf(0, 1, 2, 9)))
        assertTrue(CredentialHasher.isValidPattern(listOf(0, 4, 8, 5)))

        assertFalse(CredentialHasher.isValidPin("123"))
        assertFalse(CredentialHasher.isValidPin("1234567"))
        assertFalse(CredentialHasher.isValidPin("12a4"))
        assertEquals(true, CredentialHasher.isValidPin("2580"))
        assertEquals(true, CredentialHasher.isValidPin("258019"))
    }
}
