package eu.pascariu.shoppinglist.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.pascariu.shoppinglist.data.auth.AccessCredentials
import eu.pascariu.shoppinglist.data.auth.AuthEvent
import eu.pascariu.shoppinglist.data.auth.AuthEventBus
import eu.pascariu.shoppinglist.data.auth.TokenStore
import eu.pascariu.shoppinglist.data.network.HealthApi
import eu.pascariu.shoppinglist.di.IoDispatcher
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

enum class CredentialErrorKind {
    InvalidCredentials,
    Unreachable,
    Validation,
    QrFormat,
}

data class CredentialEntryUiState(
    val clientId: String = "",
    val clientSecret: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val errorKind: CredentialErrorKind? = null,
    val rejectionMessage: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class CredentialEntryViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val healthApi: HealthApi,
    authEventBus: AuthEventBus,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialEntryUiState())
    val uiState: StateFlow<CredentialEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authEventBus.events.collect { event ->
                if (event is AuthEvent.CredentialsRejected) {
                    _uiState.update { state ->
                        when {
                            state.isSaving -> state.copy(saved = false)
                            state.error == INVALID_CREDENTIALS_MESSAGE -> state
                            else -> state.copy(saved = false, rejectionMessage = REJECTION_MESSAGE)
                        }
                    }
                }
            }
        }
    }

    fun onClientIdChange(value: String) {
        _uiState.update { it.copy(clientId = value, error = null, errorKind = null, rejectionMessage = null) }
    }

    fun onClientSecretChange(value: String) {
        _uiState.update { it.copy(clientSecret = value, error = null, errorKind = null, rejectionMessage = null) }
    }

    fun onQrPayloadScanned(payload: String) {
        val credentials = parseQrPayload(payload)
        if (credentials == null) {
            _uiState.update { it.copy(error = "Unrecognized QR code format", errorKind = CredentialErrorKind.QrFormat) }
        } else {
            _uiState.update {
                it.copy(
                    clientId = credentials.clientId,
                    clientSecret = credentials.clientSecret,
                    error = null,
                    errorKind = null,
                    rejectionMessage = null,
                )
            }
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.clientId.isBlank() || state.clientSecret.isBlank()) {
            _uiState.update { it.copy(error = "Both client id and secret are required", errorKind = CredentialErrorKind.Validation) }
            return
        }
        verifyAndSave(AccessCredentials(state.clientId.trim(), state.clientSecret.trim()))
    }

    fun onSavedHandled() {
        _uiState.update { it.copy(saved = false) }
    }

    fun onShownAfterRejection() {
        _uiState.update { it.copy(rejectionMessage = REJECTION_MESSAGE) }
    }

    private fun verifyAndSave(credentials: AccessCredentials) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, errorKind = null) }
            withContext(ioDispatcher) {
                tokenStore.save(credentials)
            }
            when (probeCredentials()) {
                ProbeOutcome.Verified -> _uiState.update {
                    it.copy(isSaving = false, saved = true)
                }
                ProbeOutcome.Rejected -> {
                    withContext(ioDispatcher) {
                        tokenStore.clear()
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = INVALID_CREDENTIALS_MESSAGE,
                            errorKind = CredentialErrorKind.InvalidCredentials,
                            rejectionMessage = null,
                        )
                    }
                }
                ProbeOutcome.Unreachable -> {
                    withContext(ioDispatcher) {
                        tokenStore.clear()
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = UNREACHABLE_MESSAGE,
                            errorKind = CredentialErrorKind.Unreachable,
                            rejectionMessage = null,
                        )
                    }
                }
            }
        }
    }

    private suspend fun probeCredentials(): ProbeOutcome =
        withContext(ioDispatcher) {
            try {
                healthApi.getHealth()
                ProbeOutcome.Verified
            } catch (error: HttpException) {
                if (error.code() == 401 || error.code() == 403) {
                    ProbeOutcome.Rejected
                } else {
                    ProbeOutcome.Unreachable
                }
            } catch (error: IOException) {
                ProbeOutcome.Unreachable
            }
        }

    private enum class ProbeOutcome {
        Verified,
        Rejected,
        Unreachable,
    }

    private fun parseQrPayload(payload: String): AccessCredentials? {
        val params = payload.substringAfter("?", "").split('&')
            .mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                pair.take(idx) to java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
            }
            .toMap()
        val clientId = params["id"] ?: return null
        val clientSecret = params["secret"] ?: return null
        if (clientId.isBlank() || clientSecret.isBlank()) return null
        return AccessCredentials(clientId, clientSecret)
    }

    companion object {
        const val INVALID_CREDENTIALS_MESSAGE = "Invalid credentials"
        const val UNREACHABLE_MESSAGE = "Can't reach the server"
        const val REJECTION_MESSAGE = "Your credentials were rejected or the session expired. Please sign in again."
    }
}
