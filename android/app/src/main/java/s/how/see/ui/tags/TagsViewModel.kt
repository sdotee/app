package s.how.see.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import s.how.see.data.remote.model.Result
import s.how.see.data.remote.model.Tag
import s.how.see.data.repository.TagRepository
import javax.inject.Inject

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val repository: TagRepository,
) : ViewModel() {

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags = _tags.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadTags() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getTags()) {
                is Result.Success -> _tags.value = result.data
                else -> {}
            }
            _isLoading.value = false
        }
    }
}
