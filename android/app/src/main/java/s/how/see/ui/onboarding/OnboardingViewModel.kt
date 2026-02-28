package s.how.see.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import s.how.see.data.local.preferences.AppPreferences
import s.how.see.data.local.preferences.SecureStorage
import s.how.see.data.remote.model.Result
import s.how.see.data.repository.FileRepository
import s.how.see.data.repository.ShortLinkRepository
import s.how.see.data.repository.TextShareRepository
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val appPreferences: AppPreferences,
    private val shortLinkRepository: ShortLinkRepository,
    private val textShareRepository: TextShareRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val state = _state.asStateFlow()

    fun getBaseUrl(): String = runBlocking { appPreferences.baseUrl.first() }

    fun verifyAndContinue(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            _state.value = OnboardingState.Validating

            // Save base URL and API key
            appPreferences.setBaseUrl(baseUrl)
            secureStorage.saveApiKey(apiKey)

            // Validate by fetching domains
            when (val result = shortLinkRepository.getDomains()) {
                is Result.Success -> {
                    _state.value = OnboardingState.Verified

                    // Auto-set default domains
                    if (result.data.isNotEmpty()) {
                        appPreferences.setDefaultLinkDomain(result.data.first())
                    }

                    // Fetch text domains
                    when (val textResult = textShareRepository.getTextDomains()) {
                        is Result.Success -> {
                            if (textResult.data.isNotEmpty()) {
                                appPreferences.setDefaultTextDomain(textResult.data.first())
                            }
                        }
                        else -> {}
                    }

                    // Fetch file domains
                    when (val fileResult = fileRepository.getFileDomains()) {
                        is Result.Success -> {
                            if (fileResult.data.isNotEmpty()) {
                                appPreferences.setDefaultFileDomain(fileResult.data.first())
                            }
                        }
                        else -> {}
                    }

                    _state.value = OnboardingState.Success
                }
                is Result.Error -> {
                    secureStorage.clearApiKey()
                    _state.value = OnboardingState.Error(result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    sealed class OnboardingState {
        data object Idle : OnboardingState()
        data object Validating : OnboardingState()
        data object Verified : OnboardingState()
        data object Success : OnboardingState()
        data class Error(val message: String) : OnboardingState()
    }
}
