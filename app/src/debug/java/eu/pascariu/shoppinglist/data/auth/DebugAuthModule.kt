package eu.pascariu.shoppinglist.data.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugAuthModule {

    @Binds
    abstract fun bindAuthInterceptor(impl: NoOpAuthInterceptor): AuthInterceptor
}
