package eu.pascariu.shoppinglist.data.device

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface DeviceIdStore {
    fun getOrCreate(): String
}

@Singleton
class EncryptedDeviceIdStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceIdStore {

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

    override fun getOrCreate(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val deviceId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    private companion object {
        const val PREFS_FILE = "shoplist_device_identity"
        const val KEY_DEVICE_ID = "device_id"
    }
}
