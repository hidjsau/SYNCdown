package com.clipsync.app

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    private var cachedPin = ""
    private var cachedKey: SecretKeyFactoryKey? = null

    private class SecretKeyFactoryKey(val key: SecretKeySpec)

    private fun key(pin: String): SecretKeySpec {
        val c = cachedKey
        if (c != null && cachedPin == pin) return c.key
        val spec = PBEKeySpec(pin.toCharArray(), "clipsync-v1".toByteArray(), 10000, 256)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val k = SecretKeySpec(raw, "AES")
        cachedPin = pin
        cachedKey = SecretKeyFactoryKey(k)
        return k
    }

    fun enc(pin: String, text: String): ByteArray {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key(pin), GCMParameterSpec(128, iv))
        return iv + c.doFinal(text.toByteArray(Charsets.UTF_8))
    }

    fun dec(pin: String, data: ByteArray): String? {
        return try {
            if (data.size < 13) return null
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(pin), GCMParameterSpec(128, data.copyOfRange(0, 12)))
            String(c.doFinal(data, 12, data.size - 12), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
