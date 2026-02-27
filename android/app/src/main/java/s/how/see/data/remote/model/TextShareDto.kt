package s.how.see.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTextRequest(
    val content: String,
    val title: String,
    val domain: String? = null,
    @SerialName("custom_slug") val customSlug: String? = null,
    @SerialName("text_type") val textType: String? = null,
    val password: String? = null,
    @SerialName("expire_at") val expireAt: Long? = null,
    @SerialName("tag_ids") val tagIds: List<Int>? = null,
)

@Serializable
data class CreateTextResponse(
    @SerialName("short_url") val shortUrl: String,
    val slug: String,
    @SerialName("custom_slug") val customSlug: String? = null,
)

@Serializable
data class UpdateTextRequest(
    val domain: String,
    val slug: String,
    val content: String,
    val title: String,
)

@Serializable
data class DeleteTextRequest(
    val domain: String,
    val slug: String,
)
