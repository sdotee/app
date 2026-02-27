package s.how.see.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import s.how.see.data.remote.model.Result
import s.how.see.data.remote.model.UsageResponse
import s.how.see.data.repository.UsageRepository
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(
    private val repository: UsageRepository,
) : ViewModel() {

    private val _usage = MutableStateFlow<UsageResponse?>(null)
    val usage = _usage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isAvailable = MutableStateFlow(true)
    val isAvailable = _isAvailable.asStateFlow()

    fun loadUsage() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getUsage()) {
                is Result.Success -> _usage.value = result.data
                is Result.Error -> {
                    if (result.code == 404) _isAvailable.value = false
                }
                is Result.Loading -> {}
            }
            _isLoading.value = false
        }
    }
}
