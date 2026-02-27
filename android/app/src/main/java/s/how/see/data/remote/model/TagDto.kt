package s.how.see.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class TagsResponse(
    val tags: List<Tag>,
)

@Serializable
data class Tag(
    val id: Int,
    val name: String,
)
