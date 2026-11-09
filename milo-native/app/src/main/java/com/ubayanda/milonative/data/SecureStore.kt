package com.ubayanda.milonative.data

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps small secrets (sign-in tokens, a music-server password) encrypted with a key that lives in the Android Keystore
 * and never leaves the chip/OS. Falls back to the old plain storage if the phone's Keystore misbehaves, so nobody is locked out.
 * Old plain values are re-saved encrypted the next time they are read.
 */
object SecureStore {
    private const val ALIAS = "milo_secret_v1"
    private const val TAG = "enc1:"

    private fun key(): SecretKey? = try {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey()
    } catch (_: Throwable) { null }

    fun seal(plain: String): String {
        if (plain.isEmpty()) return plain
        return try {
            val k = key() ?: return plain
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, k) }
            TAG + Base64.encodeToString(c.iv + c.doFinal(plain.toByteArray()), Base64.NO_WRAP)
        } catch (_: Throwable) { plain }
    }

    /** Returns the secret, or "" when it can't be decrypted (the person then simply signs in again). */
    fun open(stored: String): String {
        if (!stored.startsWith(TAG)) return stored
        return try {
            val raw = Base64.decode(stored.removePrefix(TAG), Base64.NO_WRAP)
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key() ?: return "", GCMParameterSpec(128, raw, 0, 12)) }
            String(c.doFinal(raw, 12, raw.size - 12))
        } catch (_: Throwable) { "" }
    }

    fun put(e: SharedPreferences.Editor, name: String, value: String): SharedPreferences.Editor = e.putString(name, seal(value))

    fun get(p: SharedPreferences, name: String): String? {
        val v = p.getString(name, null) ?: return null
        val out = open(v)
        if (!v.startsWith(TAG) && v.isNotEmpty()) { val s = seal(v); if (s != v) p.edit().putString(name, s).apply() }   // upgrade old plain value
        return out
    }
}
