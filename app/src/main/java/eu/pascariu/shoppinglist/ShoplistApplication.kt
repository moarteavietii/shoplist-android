package eu.pascariu.shoppinglist

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import eu.pascariu.shoppinglist.data.device.DeviceIdStore
import eu.pascariu.shoppinglist.data.realtime.RealtimeLifecycle
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ShoplistApplication : Application() {

    @Inject lateinit var deviceIdStore: DeviceIdStore
    @Inject lateinit var realtimeLifecycle: RealtimeLifecycle

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        appScope.launch(Dispatchers.IO) { deviceIdStore.getOrCreate() }
        realtimeLifecycle.observe(appScope)
    }
}
