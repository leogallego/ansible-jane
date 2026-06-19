package io.github.leogallego.ansiblejane.data.backup

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.model.AapInstance
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.serialization.json.Json

class BackupManager {

    private val json = Json { ignoreUnknownKeys = true }
    private val provider = CryptographyProvider.Default

    suspend fun exportBackup(
        password: String,
        instances: List<AapInstance>,
        llmConfig: LlmProviderConfig? = null,
        llmConfigs: Map<String, LlmProviderConfig>? = null
    ): ByteArray {
        val activeProvider = if (llmConfig != null && llmConfig is LlmProviderConfig.OpenAiCompatible) {
            io.github.leogallego.ansiblejane.assistant.data.KnownProvider.fromUrl(llmConfig.url).name
        } else null
        val envelope = BackupEnvelope(
            createdAt = Clock.System.now().toEpochMilliseconds(),
            instances = instances.map { it.toBackupInstance() },
            llmConfig = llmConfig,
            llmConfigs = llmConfigs,
            activeProvider = activeProvider
        )
        val plaintext = json.encodeToString(BackupEnvelope.serializer(), envelope)
        return encrypt(plaintext.encodeToByteArray(), password)
    }

    suspend fun importBackup(data: ByteArray, password: String): BackupEnvelope {
        val plaintext = try {
            decrypt(data, password)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw BackupDecryptionException("Incorrect password or corrupted backup")
        }
        return try {
            json.decodeFromString(BackupEnvelope.serializer(), plaintext.decodeToString())
        } catch (e: Exception) {
            throw BackupDecryptionException("Invalid backup format: ${e.message}")
        }
    }

    private suspend fun encrypt(data: ByteArray, password: String): ByteArray {
        val salt = CryptographyRandom.nextBytes(SALT_LENGTH)

        val derivedKeyBytes = provider.get(PBKDF2).secretDerivation(
            digest = SHA256,
            iterations = PBKDF2_ITERATIONS,
            outputSize = KEY_LENGTH_BITS.bits,
            salt = salt
        ).deriveSecretToByteArray(password.encodeToByteArray())

        val aesKey = provider.get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, derivedKeyBytes)

        val ciphertext = aesKey.cipher().encrypt(data)

        return salt + ciphertext
    }

    private suspend fun decrypt(data: ByteArray, password: String): ByteArray {
        if (data.size < SALT_LENGTH + MIN_CIPHERTEXT_LENGTH) {
            throw BackupDecryptionException("Backup file is too small or corrupted")
        }
        val salt = data.copyOfRange(0, SALT_LENGTH)
        val ciphertext = data.copyOfRange(SALT_LENGTH, data.size)

        val derivedKeyBytes = provider.get(PBKDF2).secretDerivation(
            digest = SHA256,
            iterations = PBKDF2_ITERATIONS,
            outputSize = KEY_LENGTH_BITS.bits,
            salt = salt
        ).deriveSecretToByteArray(password.encodeToByteArray())

        val aesKey = provider.get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, derivedKeyBytes)

        return aesKey.cipher().decrypt(ciphertext)
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
        private const val KEY_LENGTH_BITS = 256
        private const val PBKDF2_ITERATIONS = 600_000
        private const val MIN_CIPHERTEXT_LENGTH = 28
    }
}
