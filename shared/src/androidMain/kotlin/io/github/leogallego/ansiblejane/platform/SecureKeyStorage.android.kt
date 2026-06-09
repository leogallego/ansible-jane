package io.github.leogallego.ansiblejane.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.coroutines.runBlocking
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual class SecureKeyStorage(context: Context) {

    private val provider = CryptographyProvider.Default
    private val prefs = context.getSharedPreferences("aap_crypto_prefs", Context.MODE_PRIVATE)
    private val dataKey: ByteArray

    init {
        dataKey = loadOrCreateDataKey()
    }

    actual fun encrypt(data: ByteArray): ByteArray = runBlocking {
        val aesKey = provider.get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, dataKey)
        aesKey.cipher().encrypt(data)
    }

    actual fun decrypt(data: ByteArray): ByteArray = runBlocking {
        val aesKey = provider.get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, dataKey)
        aesKey.cipher().decrypt(data)
    }

    private fun loadOrCreateDataKey(): ByteArray {
        val keystoreMasterKey = getOrCreateKeystoreMasterKey()
        val encryptedKeyB64 = prefs.getString(KEY_ENCRYPTED_DATA_KEY, null)
        val ivB64 = prefs.getString(KEY_DATA_KEY_IV, null)

        if (encryptedKeyB64 != null && ivB64 != null) {
            val encryptedKey = android.util.Base64.decode(encryptedKeyB64, android.util.Base64.NO_WRAP)
            val iv = android.util.Base64.decode(ivB64, android.util.Base64.NO_WRAP)
            return unwrapDataKey(keystoreMasterKey, encryptedKey, iv)
        }

        val newKey = runBlocking {
            provider.get(AES.GCM)
                .keyGenerator(AES.Key.Size.B256)
                .generateKey()
                .encodeToByteArray(AES.Key.Format.RAW)
        }

        val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreMasterKey)
        val encryptedKey = cipher.doFinal(newKey)
        val iv = cipher.iv

        prefs.edit()
            .putString(KEY_ENCRYPTED_DATA_KEY, android.util.Base64.encodeToString(encryptedKey, android.util.Base64.NO_WRAP))
            .putString(KEY_DATA_KEY_IV, android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
            .apply()

        return newKey
    }

    private fun unwrapDataKey(masterKey: SecretKey, encryptedKey: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(encryptedKey)
    }

    private fun getOrCreateKeystoreMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        keyStore.getEntry(MASTER_KEY_ALIAS, null)?.let { entry ->
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGen.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "aap_master_key_v2"
        private const val CIPHER_TRANSFORM = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BITS = 128
        private const val KEY_ENCRYPTED_DATA_KEY = "encrypted_data_key"
        private const val KEY_DATA_KEY_IV = "data_key_iv"
    }
}
