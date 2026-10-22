package eu.pascariu.shoppinglist.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

@Serializable
data class AccessCredentials(
    val clientId: String,
    val clientSecret: String,
)

interface TokenStore {
    fun load(): AccessCredentials?
    fun save(credentials: AccessCredentials)
    fun clear()
}

@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : TokenStore {

    private val prefs: SharedPreferences by lazy { createPrefs(retryOnCorruption = true) }

    private fun createPrefs(retryOnCorruption: Boolean): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (error: GeneralSecurityException) {
            recoverFromCorruptedPrefs(retryOnCorruption, error)
        } catch (error: IOException) {
            recoverFromCorruptedPrefs(retryOnCorruption, error)
        }
    }

    private fun recoverFromCorruptedPrefs(
        retryOnCorruption: Boolean,
        error: Exception,
    ): SharedPreferences {
        if (!retryOnCorruption) throw error
        context.deleteSharedPreferences(PREFS_FILE)
        return createPrefs(retryOnCorruption = false)
    }

    override fun load(): AccessCredentials? {
        val clientId = prefs.getString(KEY_CLIENT_ID, null) ?: return null
        val clientSecret = prefs.getString(KEY_CLIENT_SECRET, null) ?: return null
        return AccessCredentials(clientId = clientId, clientSecret = clientSecret)
    }

    override fun save(credentials: AccessCredentials) {
        prefs.edit()
            .putString(KEY_CLIENT_ID, credentials.clientId)
            .putString(KEY_CLIENT_SECRET, credentials.clientSecret)
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_FILE = "shoplist_cloudflare_tokens"
        const val KEY_CLIENT_ID = "cf_access_client_id"
        const val KEY_CLIENT_SECRET = "cf_access_client_secret"
    }
}
