package eu.pascariu.shoppinglist.data.auth

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eu.pascariu.shoppinglist.BuildConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DebugMockCloudflareModule {

    @Provides
    @Singleton
    fun provideMockCloudflareGate(tokenStore: TokenStore): MockCloudflareGate =
        MockCloudflareGateImpl(tokenStore, BuildConfig.MOCK_CLOUDFLARE)
}
