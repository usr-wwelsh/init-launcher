package com.initlauncher.lockscreen

import android.content.Context
import android.net.Uri
import android.util.Log
import com.initlauncher.theme.DefaultThemes
import com.initlauncher.theme.Theme
import com.initlauncher.theme.ThemeManager

/**
 * Reads the launcher's active theme via its ContentProvider. The launcher is a separate
 * installed app, so this is best-effort: if it's not installed, or the signature permission
 * doesn't resolve, fall back to the last theme we successfully fetched, then to CURRENT.
 */
object ThemeClient {

    private const val TAG = "ThemeClient"
    private val PROVIDER_URI: Uri = Uri.parse("content://com.initlauncher.themeprovider")

    fun resolveTheme(context: Context): Theme {
        val fetched = fetchFromProvider(context)
        if (fetched != null) {
            ThemeManager.cacheFetchedTheme(context, fetched)
            return fetched
        }
        return ThemeManager.getCachedTheme(context) ?: DefaultThemes.CURRENT
    }

    private fun fetchFromProvider(context: Context): Theme? {
        return try {
            val bundle = context.contentResolver.call(PROVIDER_URI, "getTheme", null, null)
                ?: return null
            var theme = DefaultThemes.CURRENT.copy(id = "launcher", displayName = "Launcher")
            for (field in Theme.ALL_FIELDS) {
                if (!bundle.containsKey(field)) return null
                theme = theme.withColor(field, bundle.getInt(field))
            }
            theme
        } catch (e: Exception) {
            Log.d(TAG, "Launcher theme provider unavailable: ${e.message}")
            null
        }
    }
}
