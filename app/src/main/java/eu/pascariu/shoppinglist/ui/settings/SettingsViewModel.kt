package eu.pascariu.shoppinglist.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.pascariu.shoppinglist.data.prefs.ThemeRepository
import eu.pascariu.shoppinglist.ui.theme.ThemeId
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val theme: StateFlow<ThemeId> = themeRepository.theme

    val changeToastsEnabled: StateFlow<Boolean> = themeRepository.changeToastsEnabled

    fun onThemeSelected(themeId: ThemeId) {
        viewModelScope.launch {
            themeRepository.setTheme(themeId)
        }
    }

    fun onChangeToastsToggled(enabled: Boolean) {
        viewModelScope.launch {
            themeRepository.setChangeToastsEnabled(enabled)
        }
    }
}
