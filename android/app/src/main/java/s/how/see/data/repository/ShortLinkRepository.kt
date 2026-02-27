package s.how.see.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import s.how.see.data.local.db.dao.ShortLinkDao
import s.how.see.data.local.db.entity.ShortLinkEntity
import s.how.see.data.remote.api.SEEApiService
import s.how.see.data.remote.model.CreateShortUrlRequest
import s.how.see.data.remote.model.CreateShortUrlResponse
import s.how.see.data.remote.model.DeleteShortUrlRequest
import s.how.see.data.remote.model.Result
import s.how.see.data.remote.model.UpdateShortUrlRequest
import s.how.see.data.remote.model.VisitStatResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortLinkRepository @Inject constructor(
    private val api: SEEApiService,
    private val dao: ShortLinkDao,
) {
    fun getLocalLinks(): Flow<List<ShortLinkEntity>> = dao.getAll()

    fun searchLocalLinks(query: String): Flow<List<ShortLinkEntity>> = dao.search(query)

    suspend fun getDomains(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDomains()
            if (response.code == 200 && response.data != null) {
                Result.Success(response.data.domains)
            } else {
                Result.Error(response.code, response.message ?: "Failed to get domains")
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error", exception = e)
        }
    }

    suspend fun createShortUrl(request: CreateShortUrlRequest): Result<CreateShortUrlResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.createShortUrl(request)
                if (response.code == 200 && response.data != null) {
                    dao.insert(
                        ShortLinkEntity(
                            domain = request.domain,
                            slug = response.data.slug,
                            shortUrl = response.data.shortUrl,
                            targetUrl = request.targetUrl,
                            title = request.title,
                            customSlug = response.data.customSlug,
                        )
                    )
                    Result.Success(response.data)
                } else {
                    Result.Error(response.code, response.message ?: "Failed to create short URL")
                }
            } catch (e: Exception) {
                Result.Error(message = e.message ?: "Network error", exception = e)
            }
        }

    suspend fun updateShortUrl(request: UpdateShortUrlRequest): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.updateShortUrl(request)
                if (response.code == 200) {
                    dao.update(request.domain, request.slug, request.targetUrl, request.title)
                    Result.Success(Unit)
                } else {
                    Result.Error(response.code, response.message ?: "Failed to update short URL")
                }
            } catch (e: Exception) {
                Result.Error(message = e.message ?: "Network error", exception = e)
            }
        }

    suspend fun deleteShortUrl(domain: String, slug: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.deleteShortUrl(DeleteShortUrlRequest(domain, slug))
                if (response.code == 200) {
                    dao.delete(domain, slug)
                    Result.Success(Unit)
                } else {
                    Result.Error(response.code, response.message ?: "Failed to delete short URL")
                }
            } catch (e: Exception) {
                Result.Error(message = e.message ?: "Network error", exception = e)
            }
        }

    suspend fun getVisitStat(domain: String, slug: String, period: String): Result<VisitStatResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getLinkVisitStat(domain, slug, period)
                if (response.code == 200 && response.data != null) {
                    Result.Success(response.data)
                } else {
                    Result.Error(response.code, response.message ?: "Failed to get stats")
                }
            } catch (e: Exception) {
                Result.Error(message = e.message ?: "Network error", exception = e)
            }
        }

    suspend fun clearLocalHistory() = withContext(Dispatchers.IO) { dao.deleteAll() }
}
