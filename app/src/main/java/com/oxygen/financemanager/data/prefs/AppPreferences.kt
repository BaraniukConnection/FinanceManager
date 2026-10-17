package com.oxygen.financemanager.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "settings")
private val THEME = stringPreferencesKey("theme_mode")
private val EXCLUDE_SAVINGS = booleanPreferencesKey("exclude_savings_from_total")

@Singleton
class AppPreferences @Inject constructor(private val context: Context) {

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME] = mode.name }
    }

    /** Не враховувати скарбнички в загальному балансі на Home: відкладене — не для витрат. */
    val excludeSavingsFromTotal: Flow<Boolean> = context.dataStore.data.map { it[EXCLUDE_SAVINGS] ?: false }

    suspend fun setExcludeSavingsFromTotal(exclude: Boolean) {
        context.dataStore.edit { it[EXCLUDE_SAVINGS] = exclude }
    }
}
