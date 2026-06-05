package com.ikuai.inetspeed.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("inetspeed_theme", Context.MODE_PRIVATE)

    fun getTheme(): ThemeChoice {
        val value = prefs.getString(KEY_THEME, ThemeChoice.SYSTEM.name) ?: ThemeChoice.SYSTEM.name
        return parseTheme(value)
    }

    fun observeTheme(): Flow<ThemeChoice> = callbackFlow {
        trySend(getTheme())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME) trySend(getTheme())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    private fun parseTheme(value: String): ThemeChoice {
        return try {
            ThemeChoice.valueOf(value)
        } catch (_: Exception) {
            ThemeChoice.SYSTEM
        }
    }

    fun setTheme(choice: ThemeChoice) {
        prefs.edit().putString(KEY_THEME, choice.name).apply()
    }

    companion object {
        private const val KEY_THEME = "theme_mode"
    }
}
