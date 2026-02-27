package s.how.see.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null,
)

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(
        val code: Int? = null,
        val message: String,
        val exception: Throwable? = null,
    ) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
