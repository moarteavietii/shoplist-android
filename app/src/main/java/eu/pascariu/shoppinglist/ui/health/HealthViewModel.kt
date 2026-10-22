package eu.pascariu.shoppinglist.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.pascariu.shoppinglist.data.health.HealthRepository
import eu.pascariu.shoppinglist.data.health.HealthStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
) : ViewModel() {

    private val _healthStatus = MutableStateFlow<HealthStatus>(HealthStatus.Loading)
    val healthStatus: StateFlow<HealthStatus> = _healthStatus.asStateFlow()

    init {
        checkHealth()
    }

    fun checkHealth() {
        viewModelScope.launch {
            _healthStatus.value = HealthStatus.Loading
            _healthStatus.value = healthRepository.refresh()
        }
    }
}
