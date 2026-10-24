package eu.pascariu.shoppinglist.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.connectivity.ConnectivityMonitor
import eu.pascariu.shoppinglist.data.local.ItemDao
import eu.pascariu.shoppinglist.data.local.PendingMutationDao
import eu.pascariu.shoppinglist.data.local.ShopDao
import eu.pascariu.shoppinglist.data.local.ShoplistDatabase
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShoplistDatabase =
        Room.databaseBuilder(context, ShoplistDatabase::class.java, "shoplist.db")
            .build()

    @Provides
    fun provideItemDao(database: ShoplistDatabase): ItemDao = database.itemDao()

    @Provides
    fun provideShopDao(database: ShoplistDatabase): ShopDao = database.shopDao()

    @Provides
    fun providePendingMutationDao(database: ShoplistDatabase): PendingMutationDao =
        database.pendingMutationDao()

    @Provides
    @Singleton
    fun provideConnectivityState(
        monitor: ConnectivityMonitor,
    ): @JvmSuppressWildcards StateFlow<BackendConnectivity> = monitor.connectivity
}
