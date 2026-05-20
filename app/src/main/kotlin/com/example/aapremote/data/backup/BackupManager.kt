package com.example.aapremote.data.backup

import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.model.AapInstance
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupManager {

    private val json = Json { ignoreUnknownKeys = true }

    fun exportBackup(
        password: String,
        instances: List<AapInstance>,
        llmConfig: LlmProviderConfig? = null
    ): ByteArray {
        val envelope = BackupEnvelope(
            createdAt = System.currentTimeMillis(),
            instances = instances.map { it.toBackupInstance() },
            llmConfig = llmConfig
        )
        val plaintext = json.encodeToString(BackupEnvelope.serializer(), envelope)
        return encrypt(plaintext.toByteArray(Charsets.UTF_8), password)
    }

    fun importBackup(data: ByteArray, password: String): BackupEnvelope {
        val plaintext = try {
            decrypt(data, password)
        } catch (e: javax.crypto.AEADBadTagException) {
            throw BackupDecryptionException("Incorrect password")
        } catch (e: Exception) {
            throw BackupDecryptionException("Failed to decrypt backup: ${e.message}")
        }
        return try {
            json.decodeFromString(BackupEnvelope.serializer(), String(plaintext, Charsets.UTF_8))
        } catch (e: Exception) {
            throw BackupDecryptionException("Invalid backup format: ${e.message}")
        }
    }

    private fun encrypt(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(data)

        return salt + iv + ciphertext
    }

    private fun decrypt(data: ByteArray, password: String): ByteArray {
        if (data.size < SALT_LENGTH + IV_LENGTH + TAG_LENGTH_BITS / 8) {
            throw BackupDecryptionException("Backup file is too small or corrupted")
        }
        val salt = data.copyOfRange(0, SALT_LENGTH)
        val iv = data.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val ciphertext = data.copyOfRange(SALT_LENGTH + IV_LENGTH, data.size)
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val keyBytes = factory.generateSecret(spec).encoded
            return SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun AapInstance.toBackupInstance() = BackupInstance(
        id = id,
        baseUrl = baseUrl,
        token = token,
        alias = alias,
        apiVersion = apiVersion,
        trustSelfSigned = trustSelfSigned,
        certFingerprint = certFingerprint,
        mcpServerUrls = mcpServerUrls,
        mcpEnabled = mcpEnabled
    )

    companion object {
        private const val SALT_LENGTH = 32
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
        private const val KEY_LENGTH_BITS = 256
        private const val PBKDF2_ITERATIONS = 600_000
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val CIPHER_TRANSFORM = "AES/GCM/NoPadding"
    }
}
