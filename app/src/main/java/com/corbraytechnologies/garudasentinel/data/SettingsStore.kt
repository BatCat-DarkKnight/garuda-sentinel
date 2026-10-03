package com.corbraytechnologies.garudasentinel.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    val eulaAccepted: Flow<Boolean> =
        context.settingsDataStore.data.map { it[EULA_ACCEPTED] ?: false }

    /** Folder the user granted for file scanning, as a persisted SAF tree URI. */
    val filesTreeUri: Flow<String?> =
        context.settingsDataStore.data.map { it[FILES_TREE_URI] }

    /** Opt-in FLAG_SECURE: blocks screenshots and screen recording of this app. */
    val blockScreenshots: Flow<Boolean> =
        context.settingsDataStore.data.map { it[BLOCK_SCREENSHOTS] ?: false }

    suspend fun setBlockScreenshots(blocked: Boolean) {
        context.settingsDataStore.edit { it[BLOCK_SCREENSHOTS] = blocked }
    }

    suspend fun setEulaAccepted(accepted: Boolean) {
        context.settingsDataStore.edit { it[EULA_ACCEPTED] = accepted }
    }

    suspend fun setFilesTreeUri(uri: String?) {
        context.settingsDataStore.edit {
            if (uri == null) it.remove(FILES_TREE_URI) else it[FILES_TREE_URI] = uri
        }
    }

    private companion object {
        val EULA_ACCEPTED = booleanPreferencesKey("eula_accepted")
        val FILES_TREE_URI = stringPreferencesKey("files_tree_uri")
        val BLOCK_SCREENSHOTS = booleanPreferencesKey("block_screenshots")
    }
}
