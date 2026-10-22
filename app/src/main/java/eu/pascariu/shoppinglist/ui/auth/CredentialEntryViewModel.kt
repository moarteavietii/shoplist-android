package eu.pascariu.shoppinglist.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.pascariu.shoppinglist.data.auth.AccessCredentials
import eu.pascariu.shoppinglist.data.auth.TokenStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CredentialEntryUiState(
    val clientId: String = "",
    val clientSecret: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class CredentialEntryViewModel @Inject constructor(
    private val tokenStore: TokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialEntryUiState())
    val uiState: StateFlow<CredentialEntryUiState> = _uiState.asStateFlow()

    fun onClientIdChange(value: String) {
        _uiState.update { it.copy(clientId = value, error = null) }
    }

    fun onClientSecretChange(value: String) {
        _uiState.update { it.copy(clientSecret = value, error = null) }
    }

    fun onQrPayloadScanned(payload: String) {
        val credentials = parseQrPayload(payload)
        if (credentials == null) {
            _uiState.update { it.copy(error = "Unrecognized QR code format") }
        } else {
            saveCredentials(credentials)
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.clientId.isBlank() || state.clientSecret.isBlank()) {
            _uiState.update { it.copy(error = "Both client id and secret are required") }
            return
        }
        saveCredentials(AccessCredentials(state.clientId.trim(), state.clientSecret.trim()))
    }

    private fun saveCredentials(credentials: AccessCredentials) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            tokenStore.save(credentials)
            _uiState.update { it.copy(isSaving = false, saved = true) }
        }
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
}
