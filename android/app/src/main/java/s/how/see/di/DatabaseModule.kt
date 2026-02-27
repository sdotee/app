package s.how.see.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import s.how.see.data.local.db.SEEDatabase
import s.how.see.data.local.db.dao.ShortLinkDao
import s.how.see.data.local.db.dao.TextShareDao
import s.how.see.data.local.db.dao.UploadedFileDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SEEDatabase {
        return Room.databaseBuilder(
            context,
            SEEDatabase::class.java,
            "see_database",
        ).build()
    }

    @Provides
    fun provideShortLinkDao(database: SEEDatabase): ShortLinkDao = database.shortLinkDao()

    @Provides
    fun provideTextShareDao(database: SEEDatabase): TextShareDao = database.textShareDao()

    @Provides
    fun provideUploadedFileDao(database: SEEDatabase): UploadedFileDao = database.uploadedFileDao()
}
