package com.gsoft.opus.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gsoft.opus.domain.model.ColorPalette
import com.gsoft.opus.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "opus_prefs")

@Singleton
class UserPreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_COLOR_PALETTE = stringPreferencesKey("color_palette")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    val colorPalette: Flow<ColorPalette> = context.dataStore.data.map { prefs ->
        ColorPalette.fromKey(prefs[KEY_COLOR_PALETTE])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setColorPalette(palette: ColorPalette) {
        context.dataStore.edit { it[KEY_COLOR_PALETTE] = palette.key }
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }
    val rememberMe: Flow<Boolean> = context.dataStore.data.map { it[KEY_REMEMBER_ME] ?: false }
    val savedUsername: Flow<String> = context.dataStore.data.map { it[KEY_USERNAME] ?: "" }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.first()[KEY_ACCESS_TOKEN]

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.first()[KEY_REFRESH_TOKEN]

    suspend fun saveAuthData(
        accessToken: String,
        refreshToken: String,
        username: String,
        rememberMe: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_IS_LOGGED_IN] = true
            if (rememberMe) {
                prefs[KEY_USERNAME] = username
                prefs[KEY_REMEMBER_ME] = true
            } else {
                prefs.remove(KEY_USERNAME)
                prefs[KEY_REMEMBER_ME] = false
            }
        }
    }

    suspend fun updateAccessToken(accessToken: String) {
        context.dataStore.edit { it[KEY_ACCESS_TOKEN] = accessToken }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_IS_LOGGED_IN)
            if (prefs[KEY_REMEMBER_ME] != true) {
                prefs.remove(KEY_USERNAME)
            }
        }
    }
}
