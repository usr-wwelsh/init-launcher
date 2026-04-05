package com.initlauncher.lockscreen

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class SetupActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_POST_NOTIF)
            }
        }

        findViewById<Button>(R.id.btnNotifAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnFullScreenIntent).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            })
        }

        findViewById<Button>(R.id.btnToggleService).setOnClickListener {
            val intent = Intent(this, LockscreenService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        }

        findViewById<Button>(R.id.btnSetPin).setOnClickListener { showSetPinDialog() }
        findViewById<Button>(R.id.btnClearPin).setOnClickListener { clearPin() }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_POST_NOTIF) updateStatus()
    }

    private fun showSetPinDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "4-digit PIN"
            filters = arrayOf(InputFilter.LengthFilter(4))
        }
        AlertDialog.Builder(this)
            .setTitle("Set PIN")
            .setMessage("Enter a 4-digit PIN for the lockscreen.")
            .setView(input)
            .setPositiveButton("Set") { _, _ ->
                val pin = input.text.toString()
                if (pin.length == 4 && pin.all { it.isDigit() }) {
                    getSharedPreferences(LockscreenService.PREFS, MODE_PRIVATE)
                        .edit().putString(LockscreenService.PREF_PIN, pin).apply()
                    updateStatus()
                    Toast.makeText(this, "PIN set", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearPin() {
        getSharedPreferences(LockscreenService.PREFS, MODE_PRIVATE)
            .edit().remove(LockscreenService.PREF_PIN).apply()
        updateStatus()
        Toast.makeText(this, "PIN cleared — swipe up will unlock directly", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus() {
        updateRow(
            statusView = findViewById(R.id.statusNotif),
            btn = findViewById(R.id.btnNotifAccess),
            granted = isNotificationListenerEnabled(),
            grantedLabel = "NOTIFICATION ACCESS: GRANTED",
            pendingLabel = "GRANT NOTIFICATION ACCESS"
        )

        updateRow(
            statusView = findViewById(R.id.statusOverlay),
            btn = findViewById(R.id.btnOverlay),
            granted = Settings.canDrawOverlays(this),
            grantedLabel = "DISPLAY OVER OTHER APPS: GRANTED",
            pendingLabel = "GRANT DISPLAY OVER OTHER APPS"
        )

        val btnFsi = findViewById<Button>(R.id.btnFullScreenIntent)
        val statusFsi = findViewById<TextView>(R.id.statusFullScreen)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            updateRow(statusFsi, btnFsi, isFullScreenIntentGranted(),
                "FULL SCREEN INTENT: GRANTED", "GRANT FULL SCREEN INTENT")
        } else {
            statusFsi.text = "[ OK ]"
            statusFsi.setTextColor(getColor(android.R.color.holo_green_light))
            btnFsi.text = "FULL SCREEN INTENT: AUTO-GRANTED"
        }

        // PIN status
        val hasPin = getSharedPreferences(LockscreenService.PREFS, MODE_PRIVATE)
            .getString(LockscreenService.PREF_PIN, "").isNullOrEmpty().not()
        val pinStatusView = findViewById<TextView>(R.id.statusPin)
        if (hasPin) {
            pinStatusView.text = "[ OK ]  PIN SET"
            pinStatusView.setTextColor(getColor(android.R.color.holo_green_light))
        } else {
            pinStatusView.text = "[ -- ] NO PIN (swipe unlocks directly)"
            pinStatusView.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    private fun updateRow(statusView: TextView, btn: Button, granted: Boolean, grantedLabel: String, pendingLabel: String) {
        if (granted) {
            statusView.text = "[ OK ]"
            statusView.setTextColor(getColor(android.R.color.holo_green_light))
            btn.text = grantedLabel
        } else {
            statusView.text = "[ ! ]"
            statusView.setTextColor(getColor(android.R.color.holo_red_light))
            btn.text = pendingLabel
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return listeners.contains(packageName)
    }

    private fun isFullScreenIntentGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        }
        return true
    }

    companion object {
        private const val REQ_POST_NOTIF = 1001
    }
}
