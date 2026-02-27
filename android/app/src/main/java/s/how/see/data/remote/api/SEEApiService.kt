package s.how.see.data.remote.api

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import s.how.see.data.remote.model.ApiResponse
import s.how.see.data.remote.model.CreateShortUrlRequest
import s.how.see.data.remote.model.CreateShortUrlResponse
import s.how.see.data.remote.model.CreateTextRequest
import s.how.see.data.remote.model.CreateTextResponse
import s.how.see.data.remote.model.DeleteFileResponse
import s.how.see.data.remote.model.DeleteShortUrlRequest
import s.how.see.data.remote.model.DeleteTextRequest
import s.how.see.data.remote.model.DomainsResponse
import s.how.see.data.remote.model.TagsResponse
import s.how.see.data.remote.model.UpdateShortUrlRequest
import s.how.see.data.remote.model.UpdateTextRequest
import s.how.see.data.remote.model.UploadFileResponse
import s.how.see.data.remote.model.UsageResponse
import s.how.see.data.remote.model.VisitStatResponse

interface SEEApiService {

    // ===== Short Links =====

    @GET("domains")
    suspend fun getDomains(): ApiResponse<DomainsResponse>

    @POST("shorten")
    suspend fun createShortUrl(@Body request: CreateShortUrlRequest): ApiResponse<CreateShortUrlResponse>

    @PUT("shorten")
    suspend fun updateShortUrl(@Body request: UpdateShortUrlRequest): ApiResponse<Unit>

    @HTTP(method = "DELETE", path = "shorten", hasBody = true)
    suspend fun deleteShortUrl(@Body request: DeleteShortUrlRequest): ApiResponse<Unit>

    @GET("link/visit-stat")
    suspend fun getLinkVisitStat(
        @Query("domain") domain: String,
        @Query("slug") slug: String,
        @Query("period") period: String = "totally",
    ): ApiResponse<VisitStatResponse>

    // ===== Text Sharing =====

    @GET("text/domains")
    suspend fun getTextDomains(): ApiResponse<DomainsResponse>

    @POST("text")
    suspend fun createText(@Body request: CreateTextRequest): ApiResponse<CreateTextResponse>

    @PUT("text")
    suspend fun updateText(@Body request: UpdateTextRequest): ApiResponse<Unit>

    @HTTP(method = "DELETE", path = "text", hasBody = true)
    suspend fun deleteText(@Body request: DeleteTextRequest): ApiResponse<Unit>

    // ===== Files =====

    @GET("file/domains")
    suspend fun getFileDomains(): ApiResponse<DomainsResponse>

    @Multipart
    @POST("file/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): ApiResponse<UploadFileResponse>

    @GET("file/delete/{hash}")
    suspend fun deleteFile(@Path("hash") hash: String): DeleteFileResponse

    // ===== Tags =====

    @GET("tags")
    suspend fun getTags(): ApiResponse<TagsResponse>

    // ===== Usage =====

    @GET("usage")
    suspend fun getUsage(): ApiResponse<UsageResponse>
}
