package s.how.see.ui.shortlinks

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
import s.how.see.data.local.db.entity.ShortLinkEntity
import s.how.see.data.remote.model.CreateShortUrlRequest
import s.how.see.data.remote.model.Result
import s.how.see.data.remote.model.Tag
import s.how.see.data.remote.model.UpdateShortUrlRequest
import s.how.see.data.repository.ShortLinkRepository
import s.how.see.data.repository.TagRepository
import javax.inject.Inject
import kotlin.math.ceil

private const val PAGE_SIZE = 50
private const val BATCH_DELETE_DELAY_MS = 500L

@HiltViewModel
class ShortLinkViewModel @Inject constructor(
    private val repository: ShortLinkRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allLinks = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.getLocalLinks()
        else repository.searchLocalLinks(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentPage = MutableStateFlow(1)
    val currentPage = _currentPage.asStateFlow()

    val totalPages = allLinks.combine(_currentPage) { list, _ ->
        ceil(list.size.toDouble() / PAGE_SIZE).toInt().coerceAtLeast(1)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val links = combine(allLinks, _currentPage) { list, page ->
        val start = (page - 1) * PAGE_SIZE
        list.drop(start).take(PAGE_SIZE)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode = _selectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    private val _batchDeleteProgress = MutableStateFlow<Pair<Int, Int>?>(null) // current / total
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
        _selectedIds.value = links.value.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun batchDelete(items: List<ShortLinkEntity>) {
        viewModelScope.launch {
            val total = items.size
            _batchDeleteProgress.value = 0 to total
            items.forEachIndexed { index, link ->
                repository.deleteShortUrl(link.domain, link.slug)
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

    private val _stats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val stats = _stats.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1
    }

    fun setPage(page: Int) { _currentPage.value = page }

    fun loadDomains() {
        viewModelScope.launch {
            when (val result = repository.getDomains()) {
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

    fun createShortUrl(
        targetUrl: String, domain: String, customSlug: String?, title: String?,
        password: String?, expireAt: Long?, expirationRedirectUrl: String?, tagIds: List<Int>?,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.createShortUrl(
                CreateShortUrlRequest(
                    targetUrl = targetUrl, domain = domain, customSlug = customSlug?.ifBlank { null },
                    title = title?.ifBlank { null }, password = password?.ifBlank { null },
                    expireAt = expireAt, expirationRedirectUrl = expirationRedirectUrl?.ifBlank { null },
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

    fun updateShortUrl(domain: String, slug: String, targetUrl: String, title: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateShortUrl(
                UpdateShortUrlRequest(domain = domain, slug = slug, targetUrl = targetUrl, title = title)
            )
            _isLoading.value = false
        }
    }

    fun deleteShortUrl(domain: String, slug: String) {
        viewModelScope.launch { repository.deleteShortUrl(domain, slug) }
    }

    fun loadStats(domain: String, slug: String) {
        viewModelScope.launch {
            listOf("daily", "monthly", "totally").forEach { period ->
                when (val result = repository.getVisitStat(domain, slug, period)) {
                    is Result.Success -> {
                        _stats.value = _stats.value + (period to result.data.visitCount)
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearCreateResult() { _createResult.value = null }
}
