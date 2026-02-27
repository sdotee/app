package s.how.see.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uploaded_files")
data class UploadedFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "file_id") val fileId: Int,
    val filename: String,
    val size: Long,
    val width: Int? = null,
    val height: Int? = null,
    val url: String,
    val page: String? = null,
    val hash: String,
    @ColumnInfo(name = "delete_url") val deleteUrl: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
