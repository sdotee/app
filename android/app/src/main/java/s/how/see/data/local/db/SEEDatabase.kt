package s.how.see.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import s.how.see.data.local.db.dao.ShortLinkDao
import s.how.see.data.local.db.dao.TextShareDao
import s.how.see.data.local.db.dao.UploadedFileDao
import s.how.see.data.local.db.entity.ShortLinkEntity
import s.how.see.data.local.db.entity.TextShareEntity
import s.how.see.data.local.db.entity.UploadedFileEntity

@Database(
    entities = [
        ShortLinkEntity::class,
        TextShareEntity::class,
        UploadedFileEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SEEDatabase : RoomDatabase() {
    abstract fun shortLinkDao(): ShortLinkDao
    abstract fun textShareDao(): TextShareDao
    abstract fun uploadedFileDao(): UploadedFileDao
}
