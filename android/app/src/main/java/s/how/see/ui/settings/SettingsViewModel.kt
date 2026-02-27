package s.how.see.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import s.how.see.data.local.preferences.AppPreferences
import s.how.see.data.local.preferences.SecureStorage
import s.how.see.data.remote.model.Result
import s.how.see.data.repository.FileRepository
import s.how.see.data.repository.ShortLinkRepository
import s.how.see.data.repository.TextShareRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val appPreferences: AppPreferences,
    private val shortLinkRepository: ShortLinkRepository,
    private val textShareRepository: TextShareRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {

    val hasApiKey = MutableStateFlow(secureStorage.hasApiKey())

    val baseUrl = appPreferences.baseUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences.DEFAULT_BASE_URL)

    val themeMode = appPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val dynamicColor = appPreferences.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val fileLinkDisplayType = appPreferences.fileLinkDisplayType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DIRECT_LINK")

    val defaultLinkDomain = appPreferences.defaultLinkDomain
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val defaultTextDomain = appPreferences.defaultTextDomain
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val defaultFileDomain = appPreferences.defaultFileDomain
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _validationState = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val validationState = _validationState.asStateFlow()

    private val _linkDomains = MutableStateFlow<List<String>>(emptyList())
    val linkDomains = _linkDomains.asStateFlow()

    private val _textDomains = MutableStateFlow<List<String>>(emptyList())
    val textDomains = _textDomains.asStateFlow()

    private val _fileDomains = MutableStateFlow<List<String>>(emptyList())
    val fileDomains = _fileDomains.asStateFlow()

    fun getApiKey(): String = secureStorage.getApiKey() ?: ""

    fun saveApiKey(apiKey: String) {
        secureStorage.saveApiKey(apiKey)
        hasApiKey.value = true
    }

    fun validateApiKey(apiKey: String) {
        viewModelScope.launch {
            _validationState.value = ValidationState.Validating
            secureStorage.saveApiKey(apiKey)
            when (val result = shortLinkRepository.getDomains()) {
                is Result.Success -> {
                    _validationState.value = ValidationState.Valid
                    _linkDomains.value = result.data
                    hasApiKey.value = true
                    loadDomains()
                }
                is Result.Error -> {
                    _validationState.value = ValidationState.Invalid(result.message)
                    if (!secureStorage.hasApiKey()) {
                        secureStorage.clearApiKey()
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadDomains() {
        viewModelScope.launch {
            when (val result = shortLinkRepository.getDomains()) {
                is Result.Success -> {
                    _linkDomains.value = result.data
                    if (defaultLinkDomain.value == null && result.data.isNotEmpty()) {
                        appPreferences.setDefaultLinkDomain(result.data.first())
                    }
                }
                else -> {}
            }
        }
        viewModelScope.launch {
            when (val result = textShareRepository.getTextDomains()) {
                is Result.Success -> {
                    _textDomains.value = result.data
                    if (defaultTextDomain.value == null && result.data.isNotEmpty()) {
                        appPreferences.setDefaultTextDomain(result.data.first())
                    }
                }
                else -> {}
            }
        }
        viewModelScope.launch {
            when (val result = fileRepository.getFileDomains()) {
                is Result.Success -> {
                    _fileDomains.value = result.data
                    if (defaultFileDomain.value == null && result.data.isNotEmpty()) {
                        appPreferences.setDefaultFileDomain(result.data.first())
                    }
                }
                else -> {}
            }
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch { appPreferences.setBaseUrl(url) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDynamicColor(enabled) }
    }

    fun setFileLinkDisplayType(type: String) {
        viewModelScope.launch { appPreferences.setFileLinkDisplayType(type) }
    }

    fun setDefaultLinkDomain(domain: String) {
        viewModelScope.launch { appPreferences.setDefaultLinkDomain(domain) }
    }

    fun setDefaultTextDomain(domain: String) {
        viewModelScope.launch { appPreferences.setDefaultTextDomain(domain) }
    }

    fun setDefaultFileDomain(domain: String) {
        viewModelScope.launch { appPreferences.setDefaultFileDomain(domain) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            shortLinkRepository.clearLocalHistory()
            textShareRepository.clearLocalHistory()
            fileRepository.clearLocalHistory()
        }
    }

    sealed class ValidationState {
        data object Idle : ValidationState()
        data object Validating : ValidationState()
        data object Valid : ValidationState()
        data class Invalid(val message: String) : ValidationState()
    }
}
