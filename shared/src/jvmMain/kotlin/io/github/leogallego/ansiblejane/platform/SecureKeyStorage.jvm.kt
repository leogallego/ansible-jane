package io.github.leogallego.ansiblejane.platform

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

actual class SecureKeyStorage {

    private val provider = CryptographyProvider.Default
    private val configDir = File(System.getProperty("user.home"), ".ansiblejane")
    private val keystoreFile = File(configDir, "keystore.p12")
    private val passwordFile = File(configDir, "keystore.pwd")
    private val keystorePassword: CharArray
    private val dataKey: ByteArray

    init {
        configDir.mkdirs()
        restrictPermissions(configDir)
        keystorePassword = loadOrCreatePassword()
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

    private fun loadOrCreatePassword(): CharArray {
        if (passwordFile.exists()) {
            return passwordFile.readText().toCharArray()
        }
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val password = bytes.joinToString("") { "%02x".format(it) }
        passwordFile.writeText(password)
        restrictPermissions(passwordFile)
        return password.toCharArray()
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
        restrictPermissions(keystoreFile)

        return newKey
    }

    private fun restrictPermissions(file: File) {
        try {
            val perms = if (file.isDirectory) {
                PosixFilePermissions.fromString("rwx------")
            } else {
                PosixFilePermissions.fromString("rw-------")
            }
            Files.setPosixFilePermissions(file.toPath(), perms)
        } catch (_: UnsupportedOperationException) {
            file.setReadable(false, false)
            file.setReadable(true, true)
            file.setWritable(false, false)
            file.setWritable(true, true)
            file.setExecutable(false, false)
            if (file.isDirectory) file.setExecutable(true, true)
        }
    }

    companion object {
        private const val DATA_KEY_ALIAS = "aap_data_key"
    }
}
