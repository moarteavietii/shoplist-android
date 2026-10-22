package eu.pascariu.shoppinglist.data.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eu.pascariu.shoppinglist.BuildConfig
import eu.pascariu.shoppinglist.data.auth.AuthEventBus
import eu.pascariu.shoppinglist.data.auth.AuthEventBusImpl
import eu.pascariu.shoppinglist.data.auth.AuthResponseInterceptor
import eu.pascariu.shoppinglist.data.auth.AuthInterceptor
import eu.pascariu.shoppinglist.data.auth.EncryptedTokenStore
import eu.pascariu.shoppinglist.data.auth.TokenStore
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
    fun provideOkHttpClient(authEventBus: AuthEventBus, authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(AuthResponseInterceptor(authEventBus))
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
    fun provideAuthEventBus(impl: AuthEventBusImpl): AuthEventBus = impl

    @Provides
    @Singleton
    fun provideTokenStore(impl: EncryptedTokenStore): TokenStore = impl
}
