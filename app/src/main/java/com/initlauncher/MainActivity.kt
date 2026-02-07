package com.initlauncher

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import android.Manifest
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var uptimeText: TextView
    private lateinit var currentTimeText: TextView
    private lateinit var batteryText: TextView
    private lateinit var signalText: TextView
    private lateinit var wifiText: TextView
    private lateinit var asciiArt: RotatingAsciiTextView
    private lateinit var systemInfo: TextView
    private lateinit var networkStats: TextView
    private lateinit var networkGraph: NetworkGraphView
    private lateinit var memoryStats: TextView
    private lateinit var ramProgressBar: android.widget.ProgressBar
    private lateinit var diskStats: TextView
    private lateinit var diskProgressBar: android.widget.ProgressBar
    private lateinit var pinnedAppsGrid: RecyclerView
    private lateinit var allAppsButton: TextView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastUpdateTime = 0L

    private lateinit var sharedPreferences: SharedPreferences
    private val pinnedApps = mutableListOf<AppInfo>()
    private lateinit var adapter: AppGridAdapter
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("init_launcher_prefs", Context.MODE_PRIVATE)

        initViews()
        setupSystemInfo()
        setupPinnedApps()
        startMonitoring()
        checkDefaultLauncher()
        setupGestureDetector()

        allAppsButton.setOnClickListener {
            startActivity(Intent(this, AppDrawerActivity::class.java))
        }

        // Long press to set as default launcher
        allAppsButton.setOnLongClickListener {
            openLauncherSettings()
            true
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x

                if (abs(diffY) > abs(diffX)) {
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) {
                            // Swipe up detected
                            startActivity(Intent(this@MainActivity, AppDrawerActivity::class.java))
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun checkDefaultLauncher() {
        val hasAsked = sharedPreferences.getBoolean("has_asked_default_launcher", false)
        if (!hasAsked) {
            // Mark as asked
            sharedPreferences.edit().putBoolean("has_asked_default_launcher", true).apply()

            // Prompt user to set as default launcher
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Set as Default Launcher")
            builder.setMessage("Would you like to set Init Launcher as your default home screen?")
            builder.setPositiveButton("Yes") { _, _ ->
                openLauncherSettings()
            }
            builder.setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }
    }

    private fun openLauncherSettings() {
        try {
            // Try to open home app settings
            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Select Init Launcher from the list", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Fallback: trigger launcher chooser
            try {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(Intent.createChooser(intent, "Select Launcher"))
            } catch (e2: Exception) {
                Toast.makeText(this, "Please set Init Launcher as default in Settings > Apps > Default Apps > Home app", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initViews() {
        uptimeText = findViewById(R.id.uptimeText)
        currentTimeText = findViewById(R.id.currentTimeText)
        batteryText = findViewById(R.id.batteryText)
        signalText = findViewById(R.id.signalText)
        wifiText = findViewById(R.id.wifiText)
        asciiArt = findViewById(R.id.asciiArt)
        systemInfo = findViewById(R.id.systemInfo)
        networkStats = findViewById(R.id.networkStats)
        networkGraph = findViewById(R.id.networkGraph)
        memoryStats = findViewById(R.id.memoryStats)
        ramProgressBar = findViewById(R.id.ramProgressBar)
        diskStats = findViewById(R.id.diskStats)
        diskProgressBar = findViewById(R.id.diskProgressBar)
        pinnedAppsGrid = findViewById(R.id.pinnedAppsGrid)
        allAppsButton = findViewById(R.id.allAppsButton)
    }

    private fun setupSystemInfo() {
        // ASCII art is now handled by RotatingAsciiTextView automatically

        // System Information
        val model = Build.MODEL
        val osVersion = "Android ${Build.VERSION.RELEASE}"
        val kernel = System.getProperty("os.version") ?: "Unknown"
        val cpuInfo = getCpuInfo()
        val gpuInfo = getGpuInfo()
        val uptimeStr = getUptimeString()
        val memoryInfo = getMemoryInfo()
        val packageCount = getPackageCount()

        val info = """
            |OS: $osVersion
            |Host: $model
            |Kernel: $kernel
            |Uptime: $uptimeStr
            |CPU: $cpuInfo
            |GPU: $gpuInfo
            |Memory: $memoryInfo
            |Packages: $packageCount
        """.trimMargin()

        systemInfo.text = info
    }

    private fun getCpuInfo(): String {
        return try {
            val cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
            val cores = Runtime.getRuntime().availableProcessors()

            // Try to read CPU hardware info
            val cpuHardware = try {
                java.io.File("/proc/cpuinfo").bufferedReader().use { reader ->
                    reader.lineSequence()
                        .find { it.startsWith("Hardware") }
                        ?.substringAfter(":")
                        ?.trim() ?: Build.HARDWARE
                }
            } catch (e: Exception) {
                Build.HARDWARE
            }

            "$cpuHardware ($cores)"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getGpuInfo(): String {
        return try {
            // Try to get GPU info from system properties or build info
            val renderer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.SOC_MODEL ?: "Unknown"
            } else {
                "Unknown"
            }
            renderer
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getUptimeString(): String {
        val uptimeMillis = SystemClock.elapsedRealtime()
        val hours = (uptimeMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((uptimeMillis / (1000 * 60)) % 60).toInt()
        return "${hours}h ${minutes}m"
    }

    private fun getMemoryInfo(): String {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalMem = memInfo.totalMem / (1024 * 1024 * 1024.0)
        val availMem = memInfo.availMem / (1024 * 1024 * 1024.0)
        val usedMem = totalMem - availMem
        val memPercent = (usedMem / totalMem * 100).toInt()

        return String.format("%.1f GB / %.1f GB (%d%%)", usedMem, totalMem, memPercent)
    }

    private fun getPackageCount(): String {
        return try {
            val pm = packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val userApps = packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }.size
            val systemApps = packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 }.size
            "$userApps (user), $systemApps (system)"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun setupPinnedApps() {
        loadPinnedApps()

        pinnedAppsGrid.layoutManager = GridLayoutManager(this, 3)
        adapter = AppGridAdapter(pinnedApps) { position ->
            // Click: Launch app
            launchApp(pinnedApps[position])
        }

        adapter.onLongClick = { position ->
            // Long press: Replace app
            showAppSelector(position)
        }

        adapter.onItemMoved = {
            // Save when items are reordered
            savePinnedApps()
        }

        pinnedAppsGrid.adapter = adapter

        // Setup drag-and-drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                adapter.onItemMove(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }

            override fun isLongPressDragEnabled(): Boolean {
                return false  // We manually start drag on long press
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return false
            }
        })

        itemTouchHelper.attachToRecyclerView(pinnedAppsGrid)

        // Wire up drag start callback
        adapter.onStartDrag = { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        // Show help toast on first launch
        val hasSeenDragHelp = sharedPreferences.getBoolean("has_seen_drag_help", false)
        if (!hasSeenDragHelp) {
            Toast.makeText(this, "Tip: Long-press apps to reorder, double-tap to replace", Toast.LENGTH_LONG).show()
            sharedPreferences.edit().putBoolean("has_seen_drag_help", true).apply()
        }
    }

    private fun loadPinnedApps() {
        pinnedApps.clear()

        // Load saved pinned apps (comma-separated to preserve order)
        val savedAppsString = sharedPreferences.getString("pinned_apps_ordered", null)

        if (!savedAppsString.isNullOrEmpty()) {
            val pm = packageManager
            val packageNames = savedAppsString.split(",")

            packageNames.forEach { packageName ->
                if (packageName.isEmpty()) {
                    // Empty slot
                    pinnedApps.add(AppInfo("[EMPTY]", ""))
                } else {
                    try {
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        pinnedApps.add(AppInfo(
                            pm.getApplicationLabel(appInfo).toString(),
                            packageName
                        ))
                    } catch (e: Exception) {
                        // App not found, add empty slot
                        pinnedApps.add(AppInfo("[EMPTY]", ""))
                    }
                }
            }
        }

        // Fill empty slots if needed (3x3 grid = 9 apps)
        while (pinnedApps.size < 9) {
            pinnedApps.add(AppInfo("[EMPTY]", ""))
        }
    }

    private fun savePinnedApps() {
        // Save as comma-separated string to preserve order
        val packageNames = pinnedApps.map { it.packageName }.joinToString(",")
        sharedPreferences.edit().putString("pinned_apps_ordered", packageNames).apply()
    }

    private fun showAppSelector(position: Int) {
        val apps = getInstalledApps()
        val appNames = apps.map { it.name }.toTypedArray()

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.select_app))
        builder.setItems(appNames) { _, which ->
            pinnedApps[position] = apps[which]
            adapter.notifyItemChanged(position)
            savePinnedApps()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppInfo(pm.getApplicationLabel(it).toString(), it.packageName) }
            .sortedBy { it.name }
        return apps
    }

    private fun launchApp(appInfo: AppInfo) {
        if (appInfo.packageName.isEmpty()) return

        // Track launch time
        trackAppLaunch(appInfo.packageName)

        val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
        if (intent != null) {
            startActivity(intent)
        }
    }

    private fun trackAppLaunch(packageName: String) {
        val prefs = getSharedPreferences("app_launch_times", Context.MODE_PRIVATE)
        prefs.edit().putLong(packageName, System.currentTimeMillis()).apply()
    }

    private fun startMonitoring() {
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastUpdateTime = System.currentTimeMillis()

        updateRunnable = object : Runnable {
            override fun run() {
                updateStats()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateRunnable)
    }

    private fun updateStats() {
        // Memory Stats
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalMem = memInfo.totalMem / (1024 * 1024 * 1024.0)
        val availMem = memInfo.availMem / (1024 * 1024 * 1024.0)
        val usedMem = totalMem - availMem
        val memPercent = (usedMem / totalMem * 100).toInt()

        // Disk Stats
        val stat = StatFs(android.os.Environment.getDataDirectory().path)
        val totalDisk = stat.totalBytes / (1024 * 1024 * 1024.0)
        val availDisk = stat.availableBytes / (1024 * 1024 * 1024.0)
        val usedDisk = totalDisk - availDisk
        val diskPercent = (usedDisk / totalDisk * 100).toInt()

        // Network Stats
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()

        val timeDelta = (currentTime - lastUpdateTime) / 1000.0
        val rxSpeed = if (timeDelta > 0) ((currentRxBytes - lastRxBytes) / timeDelta / 1024).toInt() else 0
        val txSpeed = if (timeDelta > 0) ((currentTxBytes - lastTxBytes) / timeDelta / 1024).toInt() else 0

        lastRxBytes = currentRxBytes
        lastTxBytes = currentTxBytes
        lastUpdateTime = currentTime

        // Current Time (big display)
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val currentTimeStr = timeFormat.format(Date())
        currentTimeText.text = currentTimeStr

        // Uptime
        val uptimeMillis = SystemClock.elapsedRealtime()
        val hours = (uptimeMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((uptimeMillis / (1000 * 60)) % 60).toInt()
        uptimeText.text = "${hours}h ${minutes}m"

        // Battery
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        // Cellular Signal Strength
        updateCellSignal()

        // WiFi Signal Strength
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val rssi = wifiInfo.rssi
        // Convert RSSI to bars: -30 (excellent) to -90 (poor)
        val wifiBars = when {
            rssi >= -50 -> 4  // Excellent
            rssi >= -60 -> 3  // Good
            rssi >= -70 -> 2  // Fair
            rssi >= -80 -> 1  // Poor
            else -> 0         // No signal
        }

        // Update UI
        batteryText.text = "$batteryLevel%"
        wifiText.text = getSignalBars(wifiBars)

        networkStats.text = String.format(
            "↑ Upload: %d KB/s  ↓ Download: %d KB/s",
            txSpeed, rxSpeed
        )

        // Update network graph
        networkGraph.addDataPoint(txSpeed.toFloat(), rxSpeed.toFloat())

        memoryStats.text = String.format(
            "RAM: %.1f GB / %.1f GB (%d%%)",
            usedMem, totalMem, memPercent
        )

        // Update RAM progress bar
        ramProgressBar.progress = memPercent

        diskStats.text = String.format(
            "Storage: %.0f GB / %.0f GB (%d%%)",
            usedDisk, totalDisk, diskPercent
        )

        // Update disk progress bar
        diskProgressBar.progress = diskPercent
    }

    private fun getSignalBars(level: Int): String {
        // Use block characters to create ascending bars
        return when (level) {
            0 -> "▁▁▁▁"  // No signal
            1 -> "▂▁▁▁"  // 1 bar
            2 -> "▂▄▁▁"  // 2 bars
            3 -> "▂▄▆▁"  // 3 bars
            4 -> "▂▄▆█"  // 4 bars (full)
            else -> "····"
        }
    }

    private fun updateCellSignal() {
        try {
            // Check if we have permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                // Request permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_PHONE_STATE),
                    100
                )
                signalText.text = "····"
                return
            }

            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            // Get signal strength
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ (API 31+)
                val signalStrength = telephonyManager.signalStrength
                if (signalStrength != null) {
                    val level = signalStrength.level // 0-4
                    signalText.text = getSignalBars(level)
                } else {
                    signalText.text = "····"
                }
            } else {
                // For older Android versions, estimate based on network type
                val networkType = telephonyManager.networkType
                if (networkType != android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                    // Just show 2 bars as a default for older versions
                    signalText.text = getSignalBars(2)
                } else {
                    signalText.text = "····"
                }
            }
        } catch (e: Exception) {
            signalText.text = "····"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onResume() {
        super.onResume()
        // Reload pinned apps in case they were changed
        loadPinnedApps()
        adapter.notifyDataSetChanged()
    }
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable? = null,
    val lastLaunchTime: Long = 0
)
