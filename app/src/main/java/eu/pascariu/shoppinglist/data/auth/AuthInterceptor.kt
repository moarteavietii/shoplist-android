package eu.pascariu.shoppinglist.data.auth

import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

interface AuthInterceptor : Interceptor

class NoOpAuthInterceptor @Inject constructor() : AuthInterceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}

class CloudflareAccessInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val apiBaseUrl: String,
) : AuthInterceptor {

    private val apiOrigin: okhttp3.HttpUrl = apiBaseUrl.toHttpUrl()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(HEADER_CLIENT_ID) != null && request.header(HEADER_CLIENT_SECRET) != null) {
            return chain.proceed(request)
        }
        val credentials = tokenStore.load()
        val isApiRequest = request.url.host == apiOrigin.host &&
            request.url.port == apiOrigin.port &&
            request.url.isHttps == apiOrigin.isHttps
        if (credentials == null || !isApiRequest) {
            val stripped = request.newBuilder()
                .removeHeader(HEADER_CLIENT_ID)
                .removeHeader(HEADER_CLIENT_SECRET)
                .build()
            return chain.proceed(stripped)
        }
        val authorized = request.newBuilder()
            .header(HEADER_CLIENT_ID, credentials.clientId)
            .header(HEADER_CLIENT_SECRET, credentials.clientSecret)
            .build()
        return chain.proceed(authorized)
    }

    companion object {
        const val HEADER_CLIENT_ID = "CF-Access-Client-Id"
        const val HEADER_CLIENT_SECRET = "CF-Access-Client-Secret"
    }
}
