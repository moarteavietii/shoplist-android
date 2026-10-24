package eu.pascariu.shoppinglist.data.connectivity

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Response

@Singleton
class BackendReachabilityListener @Inject constructor(
    private val reachability: BackendReachability,
) : EventListener() {

    override fun responseHeadersEnd(call: Call, response: Response) {
        reachability.confirmReachable()
    }

    override fun callFailed(call: Call, ioe: IOException) {
        if (!call.isCanceled()) reachability.confirmUnreachable()
    }
}
