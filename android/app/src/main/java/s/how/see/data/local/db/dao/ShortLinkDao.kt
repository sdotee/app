package s.how.see.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import s.how.see.data.local.db.entity.ShortLinkEntity

@Dao
interface ShortLinkDao {

    @Query("SELECT * FROM short_links ORDER BY created_at DESC")
    fun getAll(): Flow<List<ShortLinkEntity>>

    @Query("SELECT * FROM short_links WHERE domain LIKE '%' || :query || '%' OR slug LIKE '%' || :query || '%' OR target_url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun search(query: String): Flow<List<ShortLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: ShortLinkEntity): Long

    @Query("DELETE FROM short_links WHERE domain = :domain AND slug = :slug")
    suspend fun delete(domain: String, slug: String)

    @Query("UPDATE short_links SET target_url = :targetUrl, title = :title WHERE domain = :domain AND slug = :slug")
    suspend fun update(domain: String, slug: String, targetUrl: String, title: String?)

    @Query("DELETE FROM short_links")
    suspend fun deleteAll()
}
