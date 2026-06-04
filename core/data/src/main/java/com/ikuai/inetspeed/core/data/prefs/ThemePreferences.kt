package com.ikuai.inetspeed.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
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
