package com.initlauncher.lockscreen

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LockscreenActivity : Activity() {

    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private lateinit var notifContainer: LinearLayout
    private lateinit var emptyNotifView: TextView

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 1000)
        }
    }

    private val notifHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show above the system lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide the status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setContentView(R.layout.activity_lockscreen)

        clockView = findViewById(R.id.clockView)
        dateView = findViewById(R.id.dateView)
        notifContainer = findViewById(R.id.notifContainer)
        emptyNotifView = findViewById(R.id.emptyNotifView)

        // Tap unlock button to dismiss and hand off to system lockscreen
        findViewById<View>(R.id.swipeHint).setOnClickListener { finish() }

        // Swipe up anywhere on the root to dismiss
        val root = findViewById<View>(R.id.rootLayout)
        val swipeDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null && (e1.y - e2.y) > 150 && Math.abs(velocityY) > 300) {
                    finish()
                    return true
                }
                return false
            }
        })
        root.setOnTouchListener { _, event ->
            swipeDetector.onTouchEvent(event)
            false
        }

        // Receive real-time notification updates
        NotificationListener.onChanged = {
            notifHandler.post { refreshNotifications() }
        }
    }

    override fun onResume() {
        super.onResume()
        clockHandler.post(clockTick)
        refreshNotifications()
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockTick)
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationListener.onChanged = null
        notifHandler.removeCallbacksAndMessages(null)
    }

    // Disable back button — user must swipe or tap unlock
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { }

    private fun updateClock() {
        val now = Date()
        clockView.text = AsciiBigFont.render(
            SimpleDateFormat("hh:mm", Locale.getDefault()).format(now)
        )
        dateView.text = SimpleDateFormat("EEEE  MMM dd", Locale.getDefault()).format(now).uppercase(Locale.getDefault())
    }

    private fun refreshNotifications() {
        val notifs = NotificationListener.notifications
        notifContainer.removeAllViews()

        if (notifs.isEmpty()) {
            emptyNotifView.visibility = View.VISIBLE
            return
        }

        emptyNotifView.visibility = View.GONE
        notifs.take(6).forEach { item ->
            val view = layoutInflater.inflate(R.layout.item_notification, notifContainer, false)
            view.findViewById<TextView>(R.id.notifApp).text = item.appName.uppercase(Locale.getDefault())
            view.findViewById<TextView>(R.id.notifTitle).text = item.title
            val notifText = view.findViewById<TextView>(R.id.notifText)
            if (item.text.isBlank()) {
                notifText.visibility = View.GONE
            } else {
                notifText.text = item.text
            }
            notifContainer.addView(view)
        }
    }
}
