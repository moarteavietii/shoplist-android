package eu.pascariu.shoppinglist.data.auth

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

interface MockCloudflareGate : Interceptor

class PassThroughMockCloudflareGate @Inject constructor() : MockCloudflareGate {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
