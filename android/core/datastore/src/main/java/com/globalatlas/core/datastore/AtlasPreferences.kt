package com.globalatlas.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.atlasDataStore by preferencesDataStore(name = "atlas_preferences")

@Singleton
class AtlasPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val language = stringPreferencesKey("language")
        val offlineOnly = booleanPreferencesKey("offline_only")
        val showLabels = booleanPreferencesKey("show_labels")
    }

    val language: Flow<String> = context.atlasDataStore.data.map { it[Keys.language] ?: "system" }
    val offlineOnly: Flow<Boolean> = context.atlasDataStore.data.map { it[Keys.offlineOnly] ?: false }
    val showLabels: Flow<Boolean> = context.atlasDataStore.data.map { it[Keys.showLabels] ?: true }

    suspend fun setLanguage(language: String) {
        require(language in setOf("system", "fa", "en"))
        context.atlasDataStore.edit { it[Keys.language] = language }
    }

    suspend fun setOfflineOnly(enabled: Boolean) {
        context.atlasDataStore.edit { it[Keys.offlineOnly] = enabled }
    }

    suspend fun setShowLabels(enabled: Boolean) {
        context.atlasDataStore.edit { it[Keys.showLabels] = enabled }
    }
}
