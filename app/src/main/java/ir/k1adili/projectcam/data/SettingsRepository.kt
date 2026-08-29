package ir.k1adili.projectcam.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "projectcam_settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class SettingsRepository(private val context: Context) {

    private val photographerNameKey = stringPreferencesKey("photographer_name")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val photographerName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[photographerNameKey] ?: ""
    }

    suspend fun setPhotographerName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[photographerNameKey] = name.trim()
        }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[themeModeKey]?.let { stored ->
            runCatching { ThemeMode.valueOf(stored) }.getOrNull()
        } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}
