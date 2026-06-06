package io.github.leogallego.ansiblejane.data.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.BinarySize.Companion.bits

class CryptoManager {

    private val provider = CryptographyProvider.Default

    suspend fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        val aesKey = provider.get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, key)
        return aesKey.cipher().encrypt(plaintext)
    }

    suspend fun decrypt(ciphertext: ByteArray, key: ByteArray): ByteArray {
        val aesKey = provider.get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, key)
        return aesKey.cipher().decrypt(ciphertext)
    }

    suspend fun generateKey(): ByteArray {
        return provider.get(AES.GCM)
            .keyGenerator(AES.Key.Size.B256)
            .generateKey()
            .encodeToByteArray(AES.Key.Format.RAW)
    }
}
