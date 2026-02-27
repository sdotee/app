package s.how.see.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import s.how.see.data.local.db.dao.UploadedFileDao
import s.how.see.data.local.db.entity.UploadedFileEntity
import s.how.see.data.remote.ProgressRequestBody
import s.how.see.data.remote.api.SEEApiService
import s.how.see.data.remote.model.Result
import s.how.see.data.remote.model.UploadFileResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val api: SEEApiService,
    private val dao: UploadedFileDao,
) {
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress = _uploadProgress.asStateFlow()

    fun getLocalFiles(): Flow<List<UploadedFileEntity>> = dao.getAll()

    fun searchLocalFiles(query: String): Flow<List<UploadedFileEntity>> = dao.search(query)

    suspend fun getFileDomains(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFileDomains()
            if (response.code == 200 && response.data != null) {
                Result.Success(response.data.domains)
            } else {
                Result.Error(response.code, response.message ?: "Failed to get file domains")
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error", exception = e)
        }
    }

    suspend fun uploadFile(uri: Uri): Result<UploadFileResponse> = withContext(Dispatchers.IO) {
        try {
            _uploadProgress.value = 0f
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(uri) ?: "file"

            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.Error(message = "Cannot read file")
            val bytes = inputStream.use { it.readBytes() }

            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val progressBody = ProgressRequestBody(requestBody) { written, total ->
                if (total > 0) {
                    _uploadProgress.value = written.toFloat() / total.toFloat()
                }
            }

            val part = MultipartBody.Part.createFormData("file", fileName, progressBody)
            val response = api.uploadFile(part)

            if (response.code == 200 && response.data != null) {
                dao.insert(
                    UploadedFileEntity(
                        fileId = response.data.fileId,
                        filename = response.data.filename,
                        size = response.data.size,
                        width = response.data.width,
                        height = response.data.height,
                        url = response.data.url,
                        page = response.data.page,
                        hash = response.data.hash,
                        deleteUrl = response.data.delete,
                    )
                )
                _uploadProgress.value = 1f
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message ?: "Failed to upload file")
            }
        } catch (e: Exception) {
            _uploadProgress.value = 0f
            Result.Error(message = e.message ?: "Network error", exception = e)
        }
    }

    suspend fun deleteFile(hash: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteFile(hash)
            if (response.code == 200 || response.code == 0) {
                dao.deleteByHash(hash)
                Result.Success(Unit)
            } else {
                // Even if the remote delete fails, remove from local DB
                dao.deleteByHash(hash)
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            // Still remove from local DB on network error
            dao.deleteByHash(hash)
            Result.Success(Unit)
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }

    suspend fun clearLocalHistory() = withContext(Dispatchers.IO) { dao.deleteAll() }
}
