package eu.pascariu.shoppinglist.data.realtime

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.connectivity.ConnectivityMonitor
import eu.pascariu.shoppinglist.data.local.MutationDrainer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
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
                val drainController = ConnectivityDrainController(
                    scope = this,
                    drainer = drainer,
                    syncManager = syncManager,
                )
                val connectivityJob = launch {
                    connectivityMonitor.connectivity.collect { state ->
                        drainController.onConnectivity(state)
                    }
                }
                try {
                    syncManager.start(this)
                    awaitCancellation()
                } finally {
                    connectivityJob.cancel()
                    drainController.cancelDrain()
                    syncManager.stop()
                }
            }
        }
    }
}

class ConnectivityDrainController(
    private val scope: CoroutineScope,
    private val drainer: MutationDrainer,
    private val syncManager: RealtimeSyncManager,
) {
    private var drainJob: Job? = null

    fun onConnectivity(state: BackendConnectivity) {
        when (state) {
            BackendConnectivity.REACHABLE -> launchOrSkipDrain()
            BackendConnectivity.UNREACHABLE -> cancelDrain()
        }
    }

    fun launchOrSkipDrain() {
        if (drainJob?.isActive == true) return
        drainJob = scope.launch {
            try {
                val drained = drainer.drainAll()
                if (drained) syncManager.refetchAll()
            } catch (error: CancellationException) {
                throw error
            }
        }
    }

    fun cancelDrain() {
        drainJob?.cancel()
        drainJob = null
    }
}
