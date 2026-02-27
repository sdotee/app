package s.how.see.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "short_links")
data class ShortLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val slug: String,
    @ColumnInfo(name = "short_url") val shortUrl: String,
    @ColumnInfo(name = "target_url") val targetUrl: String,
    val title: String? = null,
    @ColumnInfo(name = "custom_slug") val customSlug: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
