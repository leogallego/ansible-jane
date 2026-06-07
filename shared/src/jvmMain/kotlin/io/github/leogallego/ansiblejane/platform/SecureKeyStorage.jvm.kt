package io.github.leogallego.ansiblejane.platform

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

actual class SecureKeyStorage {

    private val provider = CryptographyProvider.Default
    private val keystoreFile = File(System.getProperty("user.home"), ".ansiblejane/keystore.p12")
    private val keystorePassword = "${System.getProperty("user.name")}ansiblejane".toCharArray()
    private val dataKey: ByteArray

    init {
        keystoreFile.parentFile?.mkdirs()
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
        val keyStore = KeyStore.getInstance("PKCS12")

        if (keystoreFile.exists()) {
            FileInputStream(keystoreFile).use { fis ->
                keyStore.load(fis, keystorePassword)
            }
            val entry = keyStore.getEntry(DATA_KEY_ALIAS, KeyStore.PasswordProtection(keystorePassword))
            if (entry is KeyStore.SecretKeyEntry) {
                return entry.secretKey.encoded
            }
        } else {
            keyStore.load(null, keystorePassword)
        }

        val newKey = runBlocking {
            provider.get(AES.GCM)
                .keyGenerator(AES.Key.Size.B256)
                .generateKey()
                .encodeToByteArray(AES.Key.Format.RAW)
        }

        val secretKey = SecretKeySpec(newKey, "AES")
        keyStore.setEntry(
            DATA_KEY_ALIAS,
            KeyStore.SecretKeyEntry(secretKey),
            KeyStore.PasswordProtection(keystorePassword)
        )

        FileOutputStream(keystoreFile).use { fos ->
            keyStore.store(fos, keystorePassword)
        }

        return newKey
    }

    companion object {
        private const val DATA_KEY_ALIAS = "aap_data_key"
    }
}
