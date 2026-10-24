package eu.pascariu.shoppinglist.data.connectivity

interface BackendReachability {
    fun confirmReachable()
    fun confirmUnreachable()
}
