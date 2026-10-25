package eu.pascariu.shoppinglist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import eu.pascariu.shoppinglist.BuildConfig
import eu.pascariu.shoppinglist.data.auth.AuthEvent
import eu.pascariu.shoppinglist.data.auth.AuthEventBus
import eu.pascariu.shoppinglist.data.auth.TokenStore
import eu.pascariu.shoppinglist.data.prefs.ThemeRepository
import eu.pascariu.shoppinglist.ui.auth.CredentialEntryScreen
import eu.pascariu.shoppinglist.ui.list.ListScreen
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.ShoplistTheme
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authEventBus: AuthEventBus

    @Inject
    lateinit var themeRepository: ThemeRepository

    @Inject
    lateinit var tokenStore: TokenStore

    private var credentialsRejected by mutableStateOf(false)
    private var credentialsMissing by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState != null) {
            credentialsRejected = savedInstanceState.getBoolean(KEY_CREDENTIALS_REJECTED)
        } else {
            credentialsMissing = BuildConfig.REQUIRES_CREDENTIALS && tokenStore.load() == null
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authEventBus.events.collect {
                    if (it is AuthEvent.CredentialsRejected) {
                        credentialsRejected = true
                    }
                }
            }
        }
        setContent {
            val themeId by themeRepository.theme.collectAsStateWithLifecycle()
            ShoplistTheme(themeId = themeId) {
                val colors = LocalShoplistColors.current
                SideEffect {
                    val window = this@MainActivity.window
                    val darkIcons = colors.chrome.luminance() > 0.5f
                    WindowCompat.getInsetsController(window, window.decorView)
                        .isAppearanceLightStatusBars = darkIcons
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                            .background(colors.chrome),
                    )
                    if (credentialsRejected || credentialsMissing) {
                        CredentialEntryScreen(
                            onCredentialsSaved = {
                                credentialsRejected = false
                                credentialsMissing = false
                            },
                            shownAfterRejection = credentialsRejected,
                        )
                    } else {
                        ListScreen()
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_CREDENTIALS_REJECTED, credentialsRejected)
    }

    private companion object {
        const val KEY_CREDENTIALS_REJECTED = "credentialsRejected"
    }
}
