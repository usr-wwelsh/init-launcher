package com.initlauncher

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.initlauncher.theme.ThemeManager

/**
 * Read-only cross-app entry point so the lockscreen module (a separate installed app) can
 * pick up the launcher's active theme. Protected by the signature-level READ_THEME permission
 * declared alongside it in the manifest, so only apps signed with the same key can query it.
 */
class ThemeContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != METHOD_GET_THEME) return null
        val context = context ?: return null
        val theme = ThemeManager.getActiveTheme(context)
        val bundle = Bundle()
        for ((field, value) in theme.fields()) {
            bundle.putInt(field, value)
        }
        return bundle
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        const val AUTHORITY = "com.initlauncher.themeprovider"
        const val METHOD_GET_THEME = "getTheme"
    }
}
