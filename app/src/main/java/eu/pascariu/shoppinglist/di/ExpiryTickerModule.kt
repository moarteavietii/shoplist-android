package eu.pascariu.shoppinglist.di

import eu.pascariu.shoppinglist.data.config.FeatureConfig
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ExpiryTickerModule {

    @Provides
    @Singleton
    @ExpiryTicker
    fun provideExpiryTicker(@ApplicationScope scope: CoroutineScope): StateFlow<Long> {
        val ticker = MutableStateFlow(System.currentTimeMillis())
        scope.launch {
            while (true) {
                delay(FeatureConfig.EXPIRY_PRUNE_INTERVAL.toMillis())
                ticker.value = System.currentTimeMillis()
            }
        }
        return ticker.asStateFlow()
    }
}
