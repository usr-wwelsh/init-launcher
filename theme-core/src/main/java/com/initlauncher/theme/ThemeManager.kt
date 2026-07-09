package com.initlauncher.theme

import android.content.Context

/**
 * Local theme store for whichever app calls it. The launcher is the only app with a UI
 * to change the selection; the lockscreen uses this purely as a read/cache layer, seeded
 * from ThemeClient's ContentProvider query.
 */
object ThemeManager {

    private const val PREFS = "theme_prefs"
    private const val KEY_SELECTED_ID = "selected_theme_id"
    private const val CUSTOM_PREFIX = "custom_"
    private const val CUSTOM_ID = "custom"

    private val listeners = mutableSetOf<() -> Unit>()

    fun getActiveTheme(context: Context): Theme {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_SELECTED_ID, DefaultThemes.CURRENT.id) ?: DefaultThemes.CURRENT.id
        if (id == CUSTOM_ID) {
            return getCustomTheme(context) ?: DefaultThemes.CURRENT
        }
        return DefaultThemes.byId(id) ?: DefaultThemes.CURRENT
    }

    fun getActiveThemeId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_ID, DefaultThemes.CURRENT.id) ?: DefaultThemes.CURRENT.id
    }

    fun setActiveThemeId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SELECTED_ID, id).apply()
        notifyListeners()
    }

    fun getCustomTheme(context: Context): Theme? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(CUSTOM_PREFIX + Theme.FIELD_BACKGROUND)) return null
        var theme = DefaultThemes.CURRENT.copy(id = CUSTOM_ID, displayName = "Custom")
        for (field in Theme.ALL_FIELDS) {
            val stored = prefs.getInt(CUSTOM_PREFIX + field, theme.color(field))
            theme = theme.withColor(field, stored)
        }
        return theme
    }

    fun saveCustomTheme(context: Context, theme: Theme) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for ((field, value) in theme.fields()) {
            editor.putInt(CUSTOM_PREFIX + field, value)
        }
        editor.putString(KEY_SELECTED_ID, CUSTOM_ID)
        editor.apply()
        notifyListeners()
    }

    /** Mirrors a theme fetched from another process into this app's local cache (no id change). */
    fun cacheFetchedTheme(context: Context, theme: Theme) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for ((field, value) in theme.fields()) {
            editor.putInt("cache_$field", value)
        }
        editor.putBoolean("has_cache", true)
        editor.apply()
    }

    fun getCachedTheme(context: Context): Theme? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("has_cache", false)) return null
        var theme = DefaultThemes.CURRENT.copy(id = "cached", displayName = "Cached")
        for (field in Theme.ALL_FIELDS) {
            val stored = prefs.getInt("cache_$field", theme.color(field))
            theme = theme.withColor(field, stored)
        }
        return theme
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.toList().forEach { it() }
    }
}
