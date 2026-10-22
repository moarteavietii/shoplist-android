package eu.pascariu.shoppinglist.data.realtime

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class RealtimeLifecycle @Inject constructor(
    private val syncManager: RealtimeSyncManager,
) {

    fun observe(scope: CoroutineScope) {
        scope.launch {
            val owner = ProcessLifecycleOwner.get()
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncManager.start(this)
            }
        }
    }
}
