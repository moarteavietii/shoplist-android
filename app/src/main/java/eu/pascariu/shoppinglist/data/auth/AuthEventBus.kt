package eu.pascariu.shoppinglist.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthEvent {
    data object CredentialsRejected : AuthEvent
}

interface AuthEventBus {
    val events: SharedFlow<AuthEvent>
    fun emit(event: AuthEvent)
}

@Singleton
class AuthEventBusImpl @Inject constructor() : AuthEventBus {

    private val mutableEvents = MutableSharedFlow<AuthEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )

    override val events: SharedFlow<AuthEvent> = mutableEvents

    override fun emit(event: AuthEvent) {
        mutableEvents.tryEmit(event)
    }
}

class AuthResponseInterceptor(
    private val authEventBus: AuthEventBus,
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isExplicitCredentialProbe =
            request.header(CloudflareAccessInterceptor.HEADER_CLIENT_ID) != null &&
                request.header(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET) != null
        val response = chain.proceed(request)
        if (!isExplicitCredentialProbe && (response.code == 401 || response.code == 403)) {
            tokenStore.clear()
            authEventBus.emit(AuthEvent.CredentialsRejected)
        }
        return response
    }
}
