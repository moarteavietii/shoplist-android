package eu.pascariu.shoppinglist.data.device

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class DeviceIdInterceptor @Inject constructor(
    private val deviceIdStore: DeviceIdStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method !in MUTATION_METHODS) {
            return chain.proceed(request)
        }
        val withDeviceId = request.newBuilder()
            .header(HEADER_DEVICE_ID, deviceIdStore.getOrCreate())
            .build()
        return chain.proceed(withDeviceId)
    }

    companion object {
        const val HEADER_DEVICE_ID = "X-Device-Id"
        private val MUTATION_METHODS = setOf("POST", "PATCH", "DELETE")
    }
}
