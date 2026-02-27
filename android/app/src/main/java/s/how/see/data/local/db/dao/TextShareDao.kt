package s.how.see.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import s.how.see.data.local.db.entity.TextShareEntity

@Dao
interface TextShareDao {

    @Query("SELECT * FROM text_shares ORDER BY created_at DESC")
    fun getAll(): Flow<List<TextShareEntity>>

    @Query("SELECT * FROM text_shares WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR slug LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun search(query: String): Flow<List<TextShareEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(textShare: TextShareEntity): Long

    @Query("DELETE FROM text_shares WHERE domain = :domain AND slug = :slug")
    suspend fun delete(domain: String, slug: String)

    @Query("UPDATE text_shares SET content = :content, title = :title WHERE domain = :domain AND slug = :slug")
    suspend fun update(domain: String, slug: String, content: String, title: String)

    @Query("DELETE FROM text_shares")
    suspend fun deleteAll()
}
