package s.how.see.ui.textsharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import s.how.see.data.local.db.entity.TextShareEntity
import s.how.see.data.remote.model.CreateTextRequest
import s.how.see.data.remote.model.Result
import s.how.see.data.remote.model.Tag
import s.how.see.data.remote.model.UpdateTextRequest
import s.how.see.data.repository.TagRepository
import s.how.see.data.repository.TextShareRepository
import javax.inject.Inject
import kotlin.math.ceil

private const val PAGE_SIZE = 50
private const val BATCH_DELETE_DELAY_MS = 500L

@HiltViewModel
class TextShareViewModel @Inject constructor(
    private val repository: TextShareRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allTextShares = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.getLocalTextShares()
        else repository.searchLocalTextShares(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentPage = MutableStateFlow(1)
    val currentPage = _currentPage.asStateFlow()

    val totalPages = allTextShares.combine(_currentPage) { list, _ ->
        ceil(list.size.toDouble() / PAGE_SIZE).toInt().coerceAtLeast(1)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val textShares = combine(allTextShares, _currentPage) { list, page ->
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
        _selectedIds.value = textShares.value.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun batchDelete(items: List<TextShareEntity>) {
        viewModelScope.launch {
            val total = items.size
            _batchDeleteProgress.value = 0 to total
            items.forEachIndexed { index, share ->
                repository.deleteTextShare(share.domain, share.slug)
                _batchDeleteProgress.value = (index + 1) to total
                if (index < total - 1) delay(BATCH_DELETE_DELAY_MS)
            }
            _batchDeleteProgress.value = null
            exitSelectionMode()
        }
    }

    private val _domains = MutableStateFlow<List<String>>(emptyList())
    val domains = _domains.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags = _tags.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _createResult = MutableStateFlow<Result<String>?>(null)
    val createResult = _createResult.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1
    }

    fun setPage(page: Int) { _currentPage.value = page }

    fun loadDomains() {
        viewModelScope.launch {
            when (val result = repository.getTextDomains()) {
                is Result.Success -> _domains.value = result.data
                else -> {}
            }
        }
    }

    fun loadTags() {
        viewModelScope.launch {
            when (val result = tagRepository.getTags()) {
                is Result.Success -> _tags.value = result.data
                else -> {}
            }
        }
    }

    fun createTextShare(
        content: String, title: String, domain: String?, customSlug: String?,
        textType: String?, password: String?, expireAt: Long?, tagIds: List<Int>?,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val actualTitle = title.ifBlank { "Untitled" }
            val result = repository.createTextShare(
                CreateTextRequest(
                    content = content, title = actualTitle, domain = domain,
                    customSlug = customSlug?.ifBlank { null }, textType = textType,
                    password = password?.ifBlank { null }, expireAt = expireAt,
                    tagIds = tagIds?.ifEmpty { null },
                )
            )
            _createResult.value = when (result) {
                is Result.Success -> Result.Success(result.data.shortUrl)
                is Result.Error -> Result.Error(result.code, result.message)
                is Result.Loading -> null
            }
            _isLoading.value = false
        }
    }

    fun updateTextShare(domain: String, slug: String, content: String, title: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateTextShare(
                UpdateTextRequest(domain = domain, slug = slug, content = content, title = title)
            )
            _isLoading.value = false
        }
    }

    fun deleteTextShare(domain: String, slug: String) {
        viewModelScope.launch { repository.deleteTextShare(domain, slug) }
    }

    fun clearCreateResult() { _createResult.value = null }
}
