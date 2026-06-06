package io.github.leogallego.ansiblejane.platform

expect class SecureKeyStorage {
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
}
