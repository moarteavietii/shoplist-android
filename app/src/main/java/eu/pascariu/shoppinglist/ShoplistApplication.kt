package eu.pascariu.shoppinglist

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import eu.pascariu.shoppinglist.data.device.DeviceIdStore
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ShoplistApplication : Application() {

    @Inject lateinit var deviceIdStore: DeviceIdStore

    private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        warmupScope.launch { deviceIdStore.getOrCreate() }
    }
}
