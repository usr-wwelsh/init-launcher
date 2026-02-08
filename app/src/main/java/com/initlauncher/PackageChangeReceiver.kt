package com.initlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Invalidate the app drawer cache
                AppDrawerActivity.invalidateCache()

                // Optional: Show a toast for newly installed apps
                if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                    val packageName = intent.data?.schemeSpecificPart
                    if (packageName != null && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        // This is a new install, not an update
                        Toast.makeText(
                            context,
                            "New app installed - check app drawer",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}
