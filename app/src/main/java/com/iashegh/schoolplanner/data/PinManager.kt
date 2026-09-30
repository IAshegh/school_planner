package com.iashegh.schoolplanner.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted, iterated hash of the parent PIN (PBKDF2). The PIN itself is never stored. */
object PinManager {
    private const val ITERATIONS = 20_000
    private const val KEY_BITS = 256

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun verify(pin: String, salt: String?, expected: String?): Boolean {
        if (salt == null || expected == null) return false
        val actual = hash(pin, salt)
        return MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())
    }
}
