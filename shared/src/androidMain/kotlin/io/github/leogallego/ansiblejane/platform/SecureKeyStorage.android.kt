package io.github.leogallego.ansiblejane.platform

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager

actual class SecureKeyStorage(context: Context) {

    private val aead: Aead

    init {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "aap_keyset", "aap_keyset_prefs")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://aap_master_key")
            .build()
            .keysetHandle
        aead = keysetHandle.getPrimitive(
            com.google.crypto.tink.RegistryConfiguration.get(),
            Aead::class.java
        )
    }

    actual fun encrypt(data: ByteArray): ByteArray = aead.encrypt(data, null)

    actual fun decrypt(data: ByteArray): ByteArray = aead.decrypt(data, null)
}
