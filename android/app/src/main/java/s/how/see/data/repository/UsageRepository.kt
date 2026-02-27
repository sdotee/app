package s.how.see.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import s.how.see.data.remote.api.SEEApiService
import s.how.see.data.remote.model.Result
import s.how.see.data.remote.model.UsageResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val api: SEEApiService,
) {
    suspend fun getUsage(): Result<UsageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getUsage()
            if ((response.code == 200 || response.code == 0) && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message ?: "Failed to get usage")
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error", exception = e)
        }
    }
}
