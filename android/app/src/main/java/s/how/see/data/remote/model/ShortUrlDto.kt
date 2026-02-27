package s.how.see.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DomainsResponse(
    val domains: List<String>,
)

@Serializable
data class CreateShortUrlRequest(
    @SerialName("target_url") val targetUrl: String,
    val domain: String,
    @SerialName("custom_slug") val customSlug: String? = null,
    val title: String? = null,
    val password: String? = null,
    @SerialName("expire_at") val expireAt: Long? = null,
    @SerialName("expiration_redirect_url") val expirationRedirectUrl: String? = null,
    @SerialName("tag_ids") val tagIds: List<Int>? = null,
)

@Serializable
data class CreateShortUrlResponse(
    @SerialName("short_url") val shortUrl: String,
    val slug: String,
    @SerialName("custom_slug") val customSlug: String? = null,
)

@Serializable
data class UpdateShortUrlRequest(
    val domain: String,
    val slug: String,
    @SerialName("target_url") val targetUrl: String,
    val title: String,
)

@Serializable
data class DeleteShortUrlRequest(
    val domain: String,
    val slug: String,
)

@Serializable
data class VisitStatResponse(
    @SerialName("visit_count") val visitCount: Int,
)
