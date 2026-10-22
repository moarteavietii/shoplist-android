package eu.pascariu.shoppinglist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.pascariu.shoppinglist.data.auth.AuthEvent
import eu.pascariu.shoppinglist.data.auth.AuthEventBus
import eu.pascariu.shoppinglist.ui.auth.CredentialEntryScreen
import eu.pascariu.shoppinglist.ui.health.HealthScreen
import eu.pascariu.shoppinglist.ui.theme.ShoplistTheme
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authEventBus: AuthEventBus

    private var credentialsRejected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            authEventBus.events.collect {
                if (it is AuthEvent.CredentialsRejected) {
                    credentialsRejected = true
                }
            }
        }
        setContent {
            ShoplistTheme {
                if (credentialsRejected) {
                    CredentialEntryScreen(onCredentialsSaved = { credentialsRejected = false })
                } else {
                    HealthScreen()
                }
            }
        }
    }
}
