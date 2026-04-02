package com.example.aapremote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.aapremote.network.ApiVersion
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets

private val Context.credentialsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "credentials"
)

class TokenManager(private val context: Context) {

    private val aead: Aead

    var cachedToken: String? = null
        private set
    var cachedBaseUrl: String? = null
        private set
    var cachedApiVersion: ApiVersion = ApiVersion.V2
        private set
    var cachedTrustSelfSigned: Boolean = false
        private set

    init {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "aap_keyset", "aap_keyset_prefs")
            .withKeyTemplate(PredefinedAeadParameters.AES256_GCM)
            .withMasterKeyUri("android-keystore://aap_master_key")
            .build()
            .keysetHandle
        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    private companion object {
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_API_VERSION = stringPreferencesKey("api_version")
        val KEY_TRUST_SELF_SIGNED = booleanPreferencesKey("trust_self_signed")
        val KEY_CERT_FINGERPRINT = stringPreferencesKey("cert_fingerprint")
    }

    private fun encrypt(value: String): String {
        val ciphertext = aead.encrypt(
            value.toByteArray(StandardCharsets.UTF_8),
            null
        )
        return android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val ciphertext = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        val plaintext = aead.decrypt(ciphertext, null)
        return String(plaintext, StandardCharsets.UTF_8)
    }

    suspend fun saveCredentials(
        baseUrl: String,
        token: String,
        apiVersion: ApiVersion,
        trustSelfSigned: Boolean = false,
        certFingerprint: String? = null
    ) {
        context.credentialsDataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = encrypt(baseUrl)
            prefs[KEY_TOKEN] = encrypt(token)
            prefs[KEY_API_VERSION] = apiVersion.name
            prefs[KEY_TRUST_SELF_SIGNED] = trustSelfSigned
            if (certFingerprint != null) {
                prefs[KEY_CERT_FINGERPRINT] = certFingerprint
            }
        }
        cachedBaseUrl = baseUrl
        cachedToken = token
        cachedApiVersion = apiVersion
        cachedTrustSelfSigned = trustSelfSigned
    }

    suspend fun loadCredentials(): Boolean {
        val prefs = context.credentialsDataStore.data.first()
        val encryptedUrl = prefs[KEY_BASE_URL] ?: return false
        val encryptedToken = prefs[KEY_TOKEN] ?: return false

        return try {
            cachedBaseUrl = decrypt(encryptedUrl)
            cachedToken = decrypt(encryptedToken)
            cachedApiVersion = prefs[KEY_API_VERSION]?.let {
                try { ApiVersion.valueOf(it) } catch (_: Exception) { ApiVersion.V2 }
            } ?: ApiVersion.V2
            cachedTrustSelfSigned = prefs[KEY_TRUST_SELF_SIGNED] ?: false
            true
        } catch (_: Exception) {
            clearCredentials()
            false
        }
    }

    suspend fun clearCredentials() {
        context.credentialsDataStore.edit { it.clear() }
        cachedToken = null
        cachedBaseUrl = null
        cachedApiVersion = ApiVersion.V2
        cachedTrustSelfSigned = false
    }

    val isLoggedIn: Flow<Boolean> = context.credentialsDataStore.data.map { prefs ->
        prefs[KEY_TOKEN] != null
    }
}
