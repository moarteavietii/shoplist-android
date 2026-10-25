package eu.pascariu.shoppinglist.data.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ReleaseAuthModule {

    @Binds
    abstract fun bindAuthInterceptor(impl: CloudflareAccessInterceptor): AuthInterceptor

    @Binds
    abstract fun bindMockCloudflareGate(impl: PassThroughMockCloudflareGate): MockCloudflareGate
}
