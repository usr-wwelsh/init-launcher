package com.initlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
        private const val CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 hours
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

            // Update cache
            cachedApps = loadedApps
            lastCacheTime = System.currentTimeMillis()

            // Update UI
            allApps.clear()
            allApps.addAll(loadedApps)

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
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun launchApp(appInfo: AppInfo) {
        // Track launch time
        trackAppLaunch(appInfo.packageName)

        val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
        if (intent != null) {
            startActivity(intent)
            finish()
        }
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
