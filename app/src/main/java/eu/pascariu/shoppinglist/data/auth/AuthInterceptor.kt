package eu.pascariu.shoppinglist.data.auth

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

interface AuthInterceptor : Interceptor

class NoOpAuthInterceptor @Inject constructor() : AuthInterceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}

class CloudflareAccessInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : AuthInterceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = tokenStore.load()
        val request = if (credentials == null) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header(HEADER_CLIENT_ID, credentials.clientId)
                .header(HEADER_CLIENT_SECRET, credentials.clientSecret)
                .build()
        }
        return chain.proceed(request)
    }

    companion object {
        const val HEADER_CLIENT_ID = "CF-Access-Client-Id"
        const val HEADER_CLIENT_SECRET = "CF-Access-Client-Secret"
    }
}
