package s.how.see.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "see_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BASE_URL] ?: DEFAULT_BASE_URL
    }

    val defaultLinkDomain: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_LINK_DOMAIN]
    }

    val defaultTextDomain: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_TEXT_DOMAIN]
    }

    val defaultFileDomain: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_FILE_DOMAIN]
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "system"
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    }

    val fileLinkDisplayType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FILE_LINK_DISPLAY_TYPE] ?: "DIRECT_LINK"
    }

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { it[KEY_BASE_URL] = url }
    }

    suspend fun setDefaultLinkDomain(domain: String) {
        context.dataStore.edit { it[KEY_DEFAULT_LINK_DOMAIN] = domain }
    }

    suspend fun setDefaultTextDomain(domain: String) {
        context.dataStore.edit { it[KEY_DEFAULT_TEXT_DOMAIN] = domain }
    }

    suspend fun setDefaultFileDomain(domain: String) {
        context.dataStore.edit { it[KEY_DEFAULT_FILE_DOMAIN] = domain }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setFileLinkDisplayType(type: String) {
        context.dataStore.edit { it[KEY_FILE_LINK_DISPLAY_TYPE] = type }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://s.ee/api/v1/"

        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_DEFAULT_LINK_DOMAIN = stringPreferencesKey("default_link_domain")
        private val KEY_DEFAULT_TEXT_DOMAIN = stringPreferencesKey("default_text_domain")
        private val KEY_DEFAULT_FILE_DOMAIN = stringPreferencesKey("default_file_domain")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_FILE_LINK_DISPLAY_TYPE = stringPreferencesKey("file_link_display_type")
    }
}
