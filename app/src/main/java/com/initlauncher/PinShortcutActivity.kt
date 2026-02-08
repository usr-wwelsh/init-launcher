package com.initlauncher

import android.app.Activity
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class PinShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val launcherApps = getSystemService(LauncherApps::class.java)
            val request = launcherApps.getPinItemRequest(intent)

            if (request != null) {
                if (request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                    val shortcutInfo = request.shortcutInfo
                    val label = shortcutInfo?.shortLabel?.toString() ?: "App"
                    val packageName = shortcutInfo?.`package` ?: ""
                    val shortcutId = shortcutInfo?.id ?: ""

                    // Accept the pin request
                    request.accept()

                    // Save the icon
                    if (shortcutInfo != null) {
                        try {
                            val iconDrawable = launcherApps.getShortcutIconDrawable(
                                shortcutInfo,
                                resources.displayMetrics.densityDpi
                            )
                            if (iconDrawable != null) {
                                saveIcon(packageName, shortcutId, iconDrawable)
                            }
                        } catch (e: Exception) {
                            // Icon save failed, not critical
                        }
                    }

                    // Save the PWA shortcut so it appears in app drawer
                    savePwaShortcut(label, packageName, shortcutId)

                    // Invalidate app drawer cache
                    AppDrawerActivity.invalidateCache()

                    Toast.makeText(
                        this,
                        "$label installed - check app drawer",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        finish()
    }

    private fun saveIcon(packageName: String, shortcutId: String, drawable: Drawable) {
        val iconDir = File(filesDir, "pwa_icons")
        iconDir.mkdirs()
        val iconFile = File(iconDir, "${packageName}_${shortcutId.hashCode()}.png")

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        FileOutputStream(iconFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bitmap.recycle()
    }

    private fun savePwaShortcut(label: String, packageName: String, shortcutId: String) {
        val prefs = getSharedPreferences("pwa_shortcuts", Context.MODE_PRIVATE)
        val existing = prefs.getStringSet("shortcuts", mutableSetOf()) ?: mutableSetOf()
        val entry = "$label||$packageName||$shortcutId"
        val updated = existing.toMutableSet()
        updated.add(entry)
        prefs.edit().putStringSet("shortcuts", updated).apply()
    }
}
