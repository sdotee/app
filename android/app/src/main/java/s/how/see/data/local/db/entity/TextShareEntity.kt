package s.how.see.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "text_shares")
data class TextShareEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val slug: String,
    @ColumnInfo(name = "short_url") val shortUrl: String,
    val title: String,
    val content: String,
    @ColumnInfo(name = "text_type") val textType: String = "plain_text",
    @ColumnInfo(name = "custom_slug") val customSlug: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
