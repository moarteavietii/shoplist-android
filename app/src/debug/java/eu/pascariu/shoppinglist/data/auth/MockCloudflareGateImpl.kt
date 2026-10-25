package eu.pascariu.shoppinglist.data.auth

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockCloudflareGateImpl(
    private val tokenStore: TokenStore,
    private val mockEnabled: Boolean,
) : MockCloudflareGate {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!mockEnabled) {
            return chain.proceed(chain.request())
        }
        val explicitClientId = chain.request().header(CloudflareAccessInterceptor.HEADER_CLIENT_ID)
        val explicitClientSecret = chain.request().header(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET)
        val credentials = if (explicitClientId != null && explicitClientSecret != null) {
            AccessCredentials(explicitClientId, explicitClientSecret)
        } else {
            tokenStore.load()
        }
        val request = if (credentials != null) {
            chain.request().newBuilder()
                .header(CloudflareAccessInterceptor.HEADER_CLIENT_ID, credentials.clientId)
                .header(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET, credentials.clientSecret)
                .build()
        } else {
            chain.request()
        }
        val matches = credentials != null &&
            credentials.clientId == FAKE_CLIENT_ID &&
            credentials.clientSecret == FAKE_CLIENT_SECRET
        return if (matches) {
            chain.proceed(request)
        } else {
            Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body("unauthorized".toResponseBody("text/plain".toMediaType()))
                .build()
        }
    }

    companion object {
        const val FAKE_CLIENT_ID = "dev-client"
        const val FAKE_CLIENT_SECRET = "dev-secret"
    }
}
