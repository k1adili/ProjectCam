package ir.k1adili.projectcam.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "projectcam_settings")

class SettingsRepository(private val context: Context) {

    private val photographerNameKey = stringPreferencesKey("photographer_name")

    val photographerName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[photographerNameKey] ?: ""
    }

    suspend fun setPhotographerName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[photographerNameKey] = name.trim()
        }
    }
}
