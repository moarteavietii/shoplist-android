package eu.pascariu.shoppinglist.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

@Composable
fun CredentialEntryScreen(
    onCredentialsSaved: () -> Unit,
    viewModel: CredentialEntryViewModel = hiltViewModel(),
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isScanning by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onCredentialsSaved()
            viewModel.onSavedHandled()
        }
    }

    if (isScanning) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
            QrScanEffect(
                onQrPayload = { payload ->
                    isScanning = false
                    viewModel.onQrPayloadScanned(payload)
                },
                modifier = Modifier.fillMaxSize(),
            )
            TextButton(
                onClick = { isScanning = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
            ) {
                Text("Cancel", style = typography.label, color = colors.content)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Cloudflare Access",
            style = typography.title,
            color = colors.content,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Enter your service-token credentials to continue.",
            style = typography.body,
            color = colors.contentMuted,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.clientId,
            onValueChange = viewModel::onClientIdChange,
            label = { Text("Client ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = typography.body.copy(color = colors.content),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.clientSecret,
            onValueChange = viewModel::onClientSecretChange,
            label = { Text("Client Secret") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = typography.body.copy(color = colors.content),
        )
        Spacer(Modifier.height(16.dp))

        state.error?.let { error ->
            Text(
                text = error,
                style = typography.label,
                color = colors.danger,
            )
            Spacer(Modifier.height(12.dp))
        }

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = viewModel::submit,
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.ledOn,
                    contentColor = Color(0xFF0B0E11),
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        color = colors.background,
                        modifier = Modifier.height(20.dp),
                    )
                } else {
                    Text("Save", style = typography.label)
                }
            }
            TextButton(onClick = { isScanning = true }) {
                Text("Scan QR", style = typography.label, color = colors.ledOn)
            }
        }
    }
}
