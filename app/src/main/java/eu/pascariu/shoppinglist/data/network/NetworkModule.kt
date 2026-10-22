package eu.pascariu.shoppinglist.data.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eu.pascariu.shoppinglist.BuildConfig
import eu.pascariu.shoppinglist.data.auth.AuthEventBus
import eu.pascariu.shoppinglist.data.auth.AuthEventBusImpl
import eu.pascariu.shoppinglist.data.auth.AuthResponseInterceptor
import eu.pascariu.shoppinglist.data.auth.AuthInterceptor
import eu.pascariu.shoppinglist.data.auth.EncryptedTokenStore
import eu.pascariu.shoppinglist.data.auth.TokenStore
import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.device.DeviceIdInterceptor
import eu.pascariu.shoppinglist.data.device.DeviceIdStore
import eu.pascariu.shoppinglist.data.device.EncryptedDeviceIdStore
import eu.pascariu.shoppinglist.data.repo.ItemsRepository
import eu.pascariu.shoppinglist.data.repo.ShopsRepository
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideApiBaseUrl(): String = BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authEventBus: AuthEventBus,
        authInterceptor: AuthInterceptor,
        tokenStore: TokenStore,
        deviceIdInterceptor: DeviceIdInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addNetworkInterceptor(authInterceptor)
            .addInterceptor(AuthResponseInterceptor(authEventBus, tokenStore))
            .addInterceptor(deviceIdInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideHealthApi(retrofit: Retrofit): HealthApi = retrofit.create(HealthApi::class.java)

    @Provides
    @Singleton
    fun provideItemsApi(retrofit: Retrofit): ItemsApi = retrofit.create(ItemsApi::class.java)

    @Provides
    @Singleton
    fun provideShopsApi(retrofit: Retrofit): ShopsApi = retrofit.create(ShopsApi::class.java)

    @Provides
    @Singleton
    fun provideAuthEventBus(impl: AuthEventBusImpl): AuthEventBus = impl

    @Provides
    @Singleton
    fun provideTokenStore(impl: EncryptedTokenStore): TokenStore = impl

    @Provides
    @Singleton
    fun provideDeviceIdStore(impl: EncryptedDeviceIdStore): DeviceIdStore = impl

    @Provides
    @Singleton
    fun provideUndoHistoryDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        File(context.filesDir, "datastore/undo_history.preferences_pb")
    }

    @Provides
    @Singleton
    fun provideRealtimeClient(
        okHttpClient: OkHttpClient,
        json: Json,
        baseUrl: String,
    ): eu.pascariu.shoppinglist.data.realtime.RealtimeClientContract =
        eu.pascariu.shoppinglist.data.realtime.RealtimeClient(
            okHttpClient = okHttpClient,
            json = json,
            baseUrl = baseUrl,
            listId = eu.pascariu.shoppinglist.data.realtime.RealtimeClient.DEFAULT_LIST_ID,
        )
}
