package s.how.see.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import s.how.see.data.remote.api.SEEApiService
import s.how.see.data.remote.model.Result
import s.how.see.data.remote.model.Tag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val api: SEEApiService,
) {
    suspend fun getTags(): Result<List<Tag>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTags()
            if (response.code == 200 && response.data != null) {
                Result.Success(response.data.tags)
            } else {
                Result.Error(response.code, response.message ?: "Failed to get tags")
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error", exception = e)
        }
    }
}
