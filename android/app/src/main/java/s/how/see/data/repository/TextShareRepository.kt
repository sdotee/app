package s.how.see.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import s.how.see.data.local.db.dao.TextShareDao
import s.how.see.data.local.db.entity.TextShareEntity
import s.how.see.data.remote.api.SEEApiService
import s.how.see.data.remote.model.CreateTextRequest
import s.how.see.data.remote.model.CreateTextResponse
import s.how.see.data.remote.model.DeleteTextRequest
import s.how.see.data.remote.model.Result
import s.how.see.data.remote.model.UpdateTextRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextShareRepository @Inject constructor(
    private val api: SEEApiService,
    private val dao: TextShareDao,
) {
    fun getLocalTextShares(): Flow<List<TextShareEntity>> = dao.getAll()

    fun searchLocalTextShares(query: String): Flow<List<TextShareEntity>> = dao.search(query)

    suspend fun getTextDomains(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTextDomains()
            if (response.code == 200 && response.data != null) {
                Result.Success(response.data.domains)
            } else {
                Result.Error(response.code, response.message ?: "Failed to get text domains")
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error", exception = e)
        }
    }

    suspend fun createTextShare(request: CreateTextRequest): Result<CreateTextResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.createText(request)
                if (response.code == 200 && response.data != null) {
                    dao.insert(
                        TextShareEntity(
                            domain = request.domain ?: "fs.to",
                            slug = response.data.slug,
                            shortUrl = response.data.shortUrl,
                            title = request.title,
                            content = request.content,
                            textType = request.textType ?: "plain_text",
                            customSlug = response.data.customSlug,
                        )
                    )
                    Result.Success(response.data)
                } else {
                    Result.Error(response.code, response.message ?: "Failed to create text share")
                }
            } catch (e: Exception) {
                Result.Error(message = e.message ?: "Network error", exception = e)
            }
        }

    suspend fun updateTextShare(request: UpdateTextRequest): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.updateText(request)
                if (response.code == 200) {
                    dao.update(request.domain, request.slug, request.content, request.title)
                    Result.Success(Unit)
                } else {
                    Result.Error(response.code, response.message ?: "Failed to update text share")
                }
            } catch (e: Exception) {
                Result.Error(message = e.message ?: "Network error", exception = e)
            }
        }

    suspend fun deleteTextShare(domain: String, slug: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.deleteText(DeleteTextRequest(domain, slug))
                if (response.code == 200) {
                    dao.delete(domain, slug)
                    Result.Success(Unit)
                } else {
                    Result.Error(response.code, response.message ?: "Failed to delete text share")
                }
            } catch (e: Exception) {
                Result.Error(message = e.message ?: "Network error", exception = e)
            }
        }

    suspend fun clearLocalHistory() = withContext(Dispatchers.IO) { dao.deleteAll() }
}
