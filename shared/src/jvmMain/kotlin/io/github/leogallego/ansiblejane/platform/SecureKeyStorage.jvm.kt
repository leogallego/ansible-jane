package io.github.leogallego.ansiblejane.platform

actual class SecureKeyStorage {
    actual fun encrypt(data: ByteArray): ByteArray =
        throw NotImplementedError("Desktop SecureKeyStorage: Phase 7/US6")

    actual fun decrypt(data: ByteArray): ByteArray =
        throw NotImplementedError("Desktop SecureKeyStorage: Phase 7/US6")
}
