package eu.pascariu.shoppinglist.data.connectivity

class NetworkAvailabilityTracker<T : Any> {

    private val availableNetworks = HashSet<T>()

    @Synchronized
    fun onNetworkAvailable(network: T) {
        availableNetworks.add(network)
    }

    @Synchronized
    fun onNetworkLost(network: T) {
        availableNetworks.remove(network)
    }

    @Synchronized
    fun hasAvailableNetwork(): Boolean = availableNetworks.isNotEmpty()
}
