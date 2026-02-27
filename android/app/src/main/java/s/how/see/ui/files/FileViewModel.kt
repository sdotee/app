package s.how.see.ui.files

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import s.how.see.data.local.db.entity.UploadedFileEntity
import s.how.see.data.local.preferences.AppPreferences
import s.how.see.data.remote.model.Result
import s.how.see.data.repository.FileRepository
import javax.inject.Inject
import kotlin.math.ceil

private const val PAGE_SIZE = 50
private const val BATCH_DELETE_DELAY_MS = 500L

@HiltViewModel
class FileViewModel @Inject constructor(
    private val repository: FileRepository,
    appPreferences: AppPreferences,
) : ViewModel() {

    val fileLinkDisplayType = appPreferences.fileLinkDisplayType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DIRECT_LINK")

    private val allFiles = repository.getLocalFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentPage = MutableStateFlow(1)
    val currentPage = _currentPage.asStateFlow()

    val totalPages = allFiles.combine(_currentPage) { list, _ ->
        ceil(list.size.toDouble() / PAGE_SIZE).toInt().coerceAtLeast(1)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val files = combine(allFiles, _currentPage) { list, page ->
        val start = (page - 1) * PAGE_SIZE
        list.drop(start).take(PAGE_SIZE)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode = _selectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    private val _batchDeleteProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val batchDeleteProgress = _batchDeleteProgress.asStateFlow()

    fun toggleSelectionMode() {
        _selectionMode.value = !_selectionMode.value
        if (!_selectionMode.value) _selectedIds.value = emptySet()
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: Long) {
        _selectedIds.value = if (id in _selectedIds.value) {
            _selectedIds.value - id
        } else {
            _selectedIds.value + id
        }
    }

    fun selectAll() {
        _selectedIds.value = files.value.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun batchDelete(items: List<UploadedFileEntity>) {
        viewModelScope.launch {
            val total = items.size
            _batchDeleteProgress.value = 0 to total
            items.forEachIndexed { index, file ->
                repository.deleteFile(file.hash)
                _batchDeleteProgress.value = (index + 1) to total
                if (index < total - 1) delay(BATCH_DELETE_DELAY_MS)
            }
            _batchDeleteProgress.value = null
            exitSelectionMode()
        }
    }

    val uploadProgress = repository.uploadProgress

    private val _domains = MutableStateFlow<List<String>>(emptyList())
    val domains = _domains.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _uploadResult = MutableStateFlow<Result<String>?>(null)
    val uploadResult = _uploadResult.asStateFlow()

    fun setPage(page: Int) { _currentPage.value = page }

    fun loadDomains() {
        viewModelScope.launch {
            when (val result = repository.getFileDomains()) {
                is Result.Success -> _domains.value = result.data
                else -> {}
            }
        }
    }

    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            val result = repository.uploadFile(uri)
            _uploadResult.value = when (result) {
                is Result.Success -> Result.Success(result.data.url)
                is Result.Error -> Result.Error(result.code, result.message)
                is Result.Loading -> null
            }
            _isUploading.value = false
        }
    }

    fun deleteFile(hash: String) {
        viewModelScope.launch { repository.deleteFile(hash) }
    }

    fun clearUploadResult() { _uploadResult.value = null }
}
