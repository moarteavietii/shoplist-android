package eu.pascariu.shoppinglist.data.realtime

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.connectivity.ConnectivityMonitor
import eu.pascariu.shoppinglist.data.local.MutationDrainer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

@Singleton
class RealtimeLifecycle @Inject constructor(
    private val syncManager: RealtimeSyncManager,
    private val drainer: MutationDrainer,
    private val connectivityMonitor: ConnectivityMonitor,
) {

    fun observe(scope: CoroutineScope) {
        scope.launch {
            val owner = ProcessLifecycleOwner.get()
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                connectivityMonitor.start()
                var drainJob: Job? = null
                val connectivityJob = launch {
                    connectivityMonitor.connectivity.collect { state ->
                        if (state == BackendConnectivity.REACHABLE) {
                            drainJob?.cancel()
                            drainJob = launch {
                                try {
                                    drainer.drainAll()
                                } finally {
                                    syncManager.refetchAll()
                                }
                            }
                        }
                    }
                }
                try {
                    syncManager.start(this)
                    awaitCancellation()
                } finally {
                    connectivityJob.cancel()
                    drainJob?.cancel()
                    syncManager.stop()
                }
            }
        }
    }
}
