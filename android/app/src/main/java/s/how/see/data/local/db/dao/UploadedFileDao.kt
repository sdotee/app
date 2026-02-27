package s.how.see.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import s.how.see.data.local.db.entity.UploadedFileEntity

@Dao
interface UploadedFileDao {

    @Query("SELECT * FROM uploaded_files ORDER BY created_at DESC")
    fun getAll(): Flow<List<UploadedFileEntity>>

    @Query("SELECT * FROM uploaded_files WHERE filename LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun search(query: String): Flow<List<UploadedFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: UploadedFileEntity): Long

    @Query("DELETE FROM uploaded_files WHERE hash = :hash")
    suspend fun deleteByHash(hash: String)

    @Query("DELETE FROM uploaded_files")
    suspend fun deleteAll()
}
