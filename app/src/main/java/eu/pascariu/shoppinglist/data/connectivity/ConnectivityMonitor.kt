package eu.pascariu.shoppinglist.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ConnectivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val _connectivity = MutableStateFlow(BackendConnectivity.UNREACHABLE)
    val connectivity: StateFlow<BackendConnectivity> = _connectivity.asStateFlow()

    private var registered = false
    private var manager: ConnectivityManager? = null
    private val availabilityTracker = NetworkAvailabilityTracker<Network>()

    @Suppress("DEPRECATION")
    fun start() {
        if (registered) return
        registered = true
        val manager = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return
        this.manager = manager
        val request = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)
        val active = manager.activeNetwork
        val caps = active?.let { manager.getNetworkCapabilities(it) }
        _connectivity.value =
            if (caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) {
                BackendConnectivity.REACHABLE
            } else {
                BackendConnectivity.UNREACHABLE
            }
    }

    fun confirmReachable() {
        _connectivity.value = BackendConnectivity.REACHABLE
    }

    fun confirmUnreachable() {
        _connectivity.value = BackendConnectivity.UNREACHABLE
    }

    private fun updateState() {
        _connectivity.value =
            if (availabilityTracker.hasAvailableNetwork() || hasValidatedActiveNetwork()) {
                BackendConnectivity.REACHABLE
            } else {
                BackendConnectivity.UNREACHABLE
            }
    }

    private fun hasValidatedActiveNetwork(): Boolean {
        val manager = this.manager ?: return false
        val active = manager.activeNetwork
        val caps = active?.let { manager.getNetworkCapabilities(it) }
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }

    private val callback = object : android.net.ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            availabilityTracker.onNetworkAvailable(network)
            updateState()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                _connectivity.value = BackendConnectivity.REACHABLE
            }
        }

        override fun onLost(network: Network) {
            availabilityTracker.onNetworkLost(network)
            updateState()
        }
    }
}
