package s.how.see.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadFileResponse(
    @SerialName("file_id") val fileId: Int,
    val filename: String,
    val storename: String? = null,
    val size: Long,
    val width: Int? = null,
    val height: Int? = null,
    val url: String,
    val page: String? = null,
    val path: String? = null,
    val hash: String,
    val delete: String? = null,
    @SerialName("upload_status") val uploadStatus: Int? = null,
)

@Serializable
data class DeleteFileResponse(
    val code: Int,
    val message: String? = null,
)
