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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

enum class CredentialErrorKind {
    InvalidCredentials,
    ServerError,
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

@Serializable
private data class QrTokenPayload(
    val clientId: String,
    val clientSecret: String,
)

private val qrJson = Json { ignoreUnknownKeys = true }

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
        if (state.isSaving) return
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
            // Probed with explicit credentials before saving: the auth interceptor
            // only injects CF headers from the store, so the store is written
            // only after the probe verifies the credentials.
            when (probeCredentials(credentials)) {
                ProbeOutcome.Verified -> {
                    withContext(ioDispatcher) {
                        tokenStore.save(credentials)
                    }
                    _uiState.update {
                        it.copy(isSaving = false, saved = true)
                    }
                }
                ProbeOutcome.Rejected -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = INVALID_CREDENTIALS_MESSAGE,
                        errorKind = CredentialErrorKind.InvalidCredentials,
                        rejectionMessage = null,
                    )
                }
                ProbeOutcome.Unreachable -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = UNREACHABLE_MESSAGE,
                        errorKind = CredentialErrorKind.Unreachable,
                        rejectionMessage = null,
                    )
                }
                ProbeOutcome.ServerError -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = SERVER_ERROR_MESSAGE,
                        errorKind = CredentialErrorKind.ServerError,
                        rejectionMessage = null,
                    )
                }
            }
        }
    }

    private suspend fun probeCredentials(credentials: AccessCredentials): ProbeOutcome =
        try {
            healthApi.probeShops(credentials.clientId, credentials.clientSecret)
            ProbeOutcome.Verified
        } catch (error: HttpException) {
            if (error.code() == 401 || error.code() == 403) {
                ProbeOutcome.Rejected
            } else {
                ProbeOutcome.ServerError
            }
        } catch (error: IOException) {
            ProbeOutcome.Unreachable
        } catch (error: SerializationException) {
            // The gate answered with its HTML login page (redirect followed)
            // instead of JSON: the probe credentials were not accepted.
            ProbeOutcome.Rejected
        }

    private enum class ProbeOutcome {
        Verified,
        Rejected,
        ServerError,
        Unreachable,
    }

    private fun parseQrPayload(payload: String): AccessCredentials? =
        try {
            val credentials = qrJson.decodeFromString<QrTokenPayload>(payload)
            if (credentials.clientId.isBlank() || credentials.clientSecret.isBlank()) null
            else AccessCredentials(credentials.clientId, credentials.clientSecret)
        } catch (error: SerializationException) {
            null
        } catch (error: IllegalArgumentException) {
            null
        }

    companion object {
        const val INVALID_CREDENTIALS_MESSAGE = "Invalid credentials"
        const val SERVER_ERROR_MESSAGE = "Server error. Please try again later."
        const val UNREACHABLE_MESSAGE = "Can't reach the server"
        const val REJECTION_MESSAGE = "Your credentials were rejected or the session expired. Please sign in again."
    }
}
