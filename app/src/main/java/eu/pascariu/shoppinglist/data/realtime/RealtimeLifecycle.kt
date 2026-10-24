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
                val drainLauncher = SingleDrainLauncher(scope) {
                    try {
                        drainer.drainAll()
                    } finally {
                        syncManager.refetchAll()
                    }
                }
                val connectivityJob = launch {
                    connectivityMonitor.connectivity.collect { state ->
                        if (state == BackendConnectivity.REACHABLE) {
                            drainLauncher.launchOrSkip()
                        }
                    }
                }
                try {
                    syncManager.start(this)
                    awaitCancellation()
                } finally {
                    connectivityJob.cancel()
                    drainLauncher.cancel()
                    syncManager.stop()
                }
            }
        }
    }
}

internal class SingleDrainLauncher(
    private val scope: CoroutineScope,
    private val drainAll: suspend () -> Unit,
) {
    private var job: kotlinx.coroutines.Job? = null

    fun launchOrSkip() {
        if (job?.isActive == true) return
        job = scope.launch { drainAll() }
    }

    fun cancel() {
        job?.cancel()
    }

    fun isActive(): Boolean = job?.isActive == true
}
