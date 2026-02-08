package com.initlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import java.io.File
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class AppDrawerActivity : Activity() {

    companion object {
        private var cachedApps: List<AppInfo>? = null
        private var lastCacheTime: Long = 0
        private var cachedAppCount: Int = 0
        private const val CACHE_VALIDITY_MS = 30 * 1000L // 30 seconds

        // Public method to invalidate cache when apps are installed/removed
        fun invalidateCache() {
            cachedApps = null
            lastCacheTime = 0
            cachedAppCount = 0
        }
    }

    private lateinit var searchBox: EditText
    private lateinit var sortButton: TextView
    private lateinit var appList: RecyclerView
    private val apps = mutableListOf<AppInfo>()
    private val allApps = mutableListOf<AppInfo>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var adapter: AppDrawerAdapter
    private var sortByRecent = true  // Default to Recent sorting

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_drawer)

        searchBox = findViewById(R.id.searchBox)
        sortButton = findViewById(R.id.sortButton)
        appList = findViewById(R.id.appList)
        setupRecyclerView()
        setupSearch()
        setupSort()
        updateSortButtonText()
        loadApps()
    }

    private fun updateSortButtonText() {
        // Button shows the CURRENT mode
        sortButton.text = if (sortByRecent) "Recent" else "A-Z"
    }

    private fun setupSort() {
        sortButton.setOnClickListener {
            sortByRecent = !sortByRecent
            updateSortButtonText()
            sortApps()
        }
    }

    private fun sortApps() {
        allApps.sortWith(if (sortByRecent) {
            // Sort by launch time, with never-launched apps at the bottom
            compareByDescending<AppInfo> { it.lastLaunchTime > 0 }
                .thenByDescending { it.lastLaunchTime }
                .thenBy { it.name }
        } else {
            compareBy { it.name }
        })
        filterApps(searchBox.text.toString())
    }

    private fun setupSearch() {
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterApps(query: String) {
        apps.clear()
        if (query.isEmpty()) {
            apps.addAll(allApps)
        } else {
            apps.addAll(allApps.filter {
                it.name.contains(query, ignoreCase = true)
            })
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadApps() {
        val currentTime = System.currentTimeMillis()

        // Use cache if valid, but refresh launch times
        if (cachedApps != null && (currentTime - lastCacheTime) < CACHE_VALIDITY_MS) {
            val launchPrefs = getSharedPreferences("app_launch_times", android.content.Context.MODE_PRIVATE)
            allApps.clear()
            // Update launch times from cache
            allApps.addAll(cachedApps!!.map { app ->
                app.copy(lastLaunchTime = launchPrefs.getLong(app.packageName, 0L))
            })
            // Apply current sort
            sortApps()
            return
        }

        // Load apps asynchronously
        val context = this
        coroutineScope.launch {
            val loadedApps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val launchPrefs = context.getSharedPreferences("app_launch_times", android.content.Context.MODE_PRIVATE)

                installedApps.mapNotNull { appInfo ->
                    val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        val lastLaunchTime = launchPrefs.getLong(appInfo.packageName, 0L)
                        AppInfo(
                            pm.getApplicationLabel(appInfo).toString(),
                            appInfo.packageName,
                            appInfo.loadIcon(pm),
                            lastLaunchTime
                        )
                    } else null
                }.sortedBy { it.name }
            }

            // Load saved PWA shortcuts
            val pwaPrefs = context.getSharedPreferences("pwa_shortcuts", Context.MODE_PRIVATE)
            val pwaEntries = pwaPrefs.getStringSet("shortcuts", emptySet()) ?: emptySet()
            val iconDir = File(context.filesDir, "pwa_icons")
            val pwaApps = pwaEntries.mapNotNull { entry ->
                val parts = entry.split("||")
                if (parts.size >= 3) {
                    val label = parts[0]
                    val browserPkg = parts[1]
                    val shortcutId = parts[2]
                    val pwaId = "pwa://$browserPkg/$shortcutId"
                    val launchPrefs = context.getSharedPreferences("app_launch_times", Context.MODE_PRIVATE)
                    val lastLaunchTime = launchPrefs.getLong(pwaId, 0L)
                    // Load saved icon
                    val iconFile = File(iconDir, "${browserPkg}_${shortcutId.hashCode()}.png")
                    val icon = if (iconFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(iconFile.absolutePath)
                        if (bitmap != null) BitmapDrawable(context.resources, bitmap) else null
                    } else null
                    AppInfo(label, pwaId, icon, lastLaunchTime)
                } else null
            }

            val allLoadedApps = loadedApps + pwaApps

            // Update cache
            cachedApps = allLoadedApps
            cachedAppCount = allLoadedApps.size
            lastCacheTime = System.currentTimeMillis()

            // Update UI
            allApps.clear()
            allApps.addAll(allLoadedApps)

            // Apply current sort
            sortApps()
        }
    }

    private fun setupRecyclerView() {
        appList.layoutManager = LinearLayoutManager(this)
        adapter = AppDrawerAdapter(apps) { position ->
            launchApp(apps[position])
        }
        adapter.onLongClick = { position ->
            showAppMenu(apps[position])
        }
        appList.adapter = adapter
    }

    private fun showAppMenu(appInfo: AppInfo) {
        if (appInfo.packageName.startsWith("pwa://")) {
            // PWA-specific menu
            val options = arrayOf("Open", "Remove")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle(appInfo.name)
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> launchApp(appInfo)
                    1 -> removePwa(appInfo)
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            builder.show()
        } else {
            val options = arrayOf("Open", "App Info", "Uninstall")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle(appInfo.name)
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> launchApp(appInfo)
                    1 -> openAppSettings(appInfo)
                    2 -> uninstallApp(appInfo)
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            builder.show()
        }
    }

    private fun removePwa(appInfo: AppInfo) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Remove PWA")
        builder.setMessage("Remove \"${appInfo.name}\" from app list?")
        builder.setPositiveButton("Remove") { _, _ ->
            val parts = appInfo.packageName.removePrefix("pwa://").split("/", limit = 2)
            val browserPkg = parts[0]
            val shortcutId = parts[1]

            // Remove from SharedPreferences
            val prefs = getSharedPreferences("pwa_shortcuts", Context.MODE_PRIVATE)
            val existing = prefs.getStringSet("shortcuts", mutableSetOf()) ?: mutableSetOf()
            val updated = existing.toMutableSet()
            updated.removeAll { it.contains("||$browserPkg||$shortcutId") }
            prefs.edit().putStringSet("shortcuts", updated).apply()

            // Remove saved icon
            val iconFile = File(filesDir, "pwa_icons/${browserPkg}_${shortcutId.hashCode()}.png")
            iconFile.delete()

            // Refresh list
            invalidateCache()
            loadApps()

            Toast.makeText(this, "${appInfo.name} removed", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun launchApp(appInfo: AppInfo) {
        // Track launch time
        trackAppLaunch(appInfo.packageName)

        if (appInfo.packageName.startsWith("pwa://")) {
            launchPwa(appInfo)
        } else {
            val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (intent != null) {
                startActivity(intent)
                finish()
            }
        }
    }

    private fun launchPwa(appInfo: AppInfo) {
        val parts = appInfo.packageName.removePrefix("pwa://").split("/", limit = 2)
        val browserPkg = parts[0]
        val shortcutId = parts[1]

        // Try LauncherApps.startShortcut first (proper way)
        try {
            val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.startShortcut(
                browserPkg, shortcutId, null, null,
                android.os.Process.myUserHandle()
            )
            finish()
            return
        } catch (e: Exception) {
            // Fall through to URL fallback
        }

        // Fallback: if shortcut ID looks like a URL, open it in the browser
        if (shortcutId.startsWith("http")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(shortcutId))
                intent.setPackage(browserPkg)
                startActivity(intent)
                finish()
                return
            } catch (e: Exception) {
                // Fall through
            }
        }

        Toast.makeText(this, "Could not launch ${appInfo.name}", Toast.LENGTH_SHORT).show()
    }

    private fun trackAppLaunch(packageName: String) {
        val prefs = getSharedPreferences("app_launch_times", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong(packageName, System.currentTimeMillis()).apply()
    }

    private fun openAppSettings(appInfo: AppInfo) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${appInfo.packageName}")
        startActivity(intent)
    }

    private fun uninstallApp(appInfo: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:${appInfo.packageName}")
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Quick check: count launchable apps and compare to cache
        val currentAppCount = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .count { packageManager.getLaunchIntentForPackage(it.packageName) != null }

        if (cachedApps == null || currentAppCount != cachedAppCount) {
            // App count changed - something was installed or removed
            invalidateCache()
            loadApps()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
