package com.initlauncher.lockscreen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LockscreenService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: SharedPreferences
    private var overlayView: View? = null

    private val clockHandler = Handler(Looper.getMainLooper())
    private val notifHandler = Handler(Looper.getMainLooper())
    private var clockTick: Runnable? = null

    // PIN state
    private var currentPin = ""

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: ${intent.action}")
            when (intent.action) {
                Intent.ACTION_SCREEN_ON  -> showOverlay()
                Intent.ACTION_USER_PRESENT -> hideOverlay()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        windowManager = getSystemService(WindowManager::class.java)
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Init Lockscreen")
            .setContentText("Active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        hideOverlay()
    }

    override fun onBind(intent: Intent): IBinder? = null

    // ── Overlay ──────────────────────────────────────────────────────────────

    private fun showOverlay() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "showOverlay: SYSTEM_ALERT_WINDOW not granted")
            return
        }

        val view = LayoutInflater.from(this).inflate(R.layout.activity_lockscreen, null)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                @Suppress("DEPRECATION") WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.OPAQUE
        )

        val clockView    = view.findViewById<TextView>(R.id.clockView)
        val amPmView     = view.findViewById<TextView>(R.id.amPmView)
        val dateView     = view.findViewById<TextView>(R.id.dateView)
        val notifContainer = view.findViewById<LinearLayout>(R.id.notifContainer)
        val emptyNotifView = view.findViewById<TextView>(R.id.emptyNotifView)
        val mainContent  = view.findViewById<View>(R.id.mainContent)
        val pinPanel     = view.findViewById<View>(R.id.pinPanel)
        val pinPrompt    = view.findViewById<TextView>(R.id.pinPrompt)
        val swipeHint    = view.findViewById<TextView>(R.id.swipeHint)
        val pinBack      = view.findViewById<View>(R.id.pinBack)

        val pinDots = listOf(
            view.findViewById<View>(R.id.pinDot0),
            view.findViewById<View>(R.id.pinDot1),
            view.findViewById<View>(R.id.pinDot2),
            view.findViewById<View>(R.id.pinDot3)
        )

        // ── Clock ──
        currentPin = ""
        clockTick = object : Runnable {
            override fun run() {
                val now = Date()
                clockView.text = AsciiBigFont.render(
                    SimpleDateFormat("hh:mm", Locale.getDefault()).format(now)
                )
                amPmView.text = SimpleDateFormat("a", Locale.getDefault()).format(now)
                    .uppercase(Locale.getDefault())
                dateView.text = SimpleDateFormat("EEEE  MMM dd", Locale.getDefault())
                    .format(now).uppercase(Locale.getDefault())
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockTick!!)

        // ── Notifications ──
        refreshNotifications(notifContainer, emptyNotifView)
        NotificationListener.onChanged = {
            notifHandler.post { refreshNotifications(notifContainer, emptyNotifView) }
        }

        // ── PIN helpers ──
        fun updateDots() {
            pinDots.forEachIndexed { i, dot ->
                dot.setBackgroundResource(
                    if (i < currentPin.length) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty
                )
            }
        }

        fun showPinWrong() {
            pinPrompt.text = "incorrect pin"
            pinPrompt.setTextColor(0xFFC05870.toInt())
            currentPin = ""
            updateDots()
            clockHandler.postDelayed({
                pinPrompt.text = "enter pin"
                pinPrompt.setTextColor(0xFF1A2A38.toInt())
            }, 800)
        }

        fun onDigit(d: String) {
            if (currentPin.length >= 4) return
            currentPin += d
            updateDots()
            if (currentPin.length == 4) {
                val stored = prefs.getString(PREF_PIN, "") ?: ""
                if (stored.isEmpty() || currentPin == stored) {
                    hideOverlay()
                } else {
                    showPinWrong()
                }
            }
        }

        fun showPin() {
            currentPin = ""
            updateDots()
            pinPrompt.text = "enter pin"
            pinPrompt.setTextColor(0xFF1A2A38.toInt())
            mainContent.visibility = View.GONE
            pinPanel.visibility = View.VISIBLE
        }

        fun showMain() {
            currentPin = ""
            mainContent.visibility = View.VISIBLE
            pinPanel.visibility = View.GONE
        }

        // ── Numpad wiring ──
        val digitIds = mapOf(
            R.id.pin0 to "0", R.id.pin1 to "1", R.id.pin2 to "2",
            R.id.pin3 to "3", R.id.pin4 to "4", R.id.pin5 to "5",
            R.id.pin6 to "6", R.id.pin7 to "7", R.id.pin8 to "8",
            R.id.pin9 to "9"
        )
        digitIds.forEach { (id, digit) ->
            view.findViewById<View>(id).setOnClickListener { onDigit(digit) }
        }
        view.findViewById<View>(R.id.pinDel).setOnClickListener {
            if (currentPin.isNotEmpty()) {
                currentPin = currentPin.dropLast(1)
                updateDots()
            }
        }
        pinBack.setOnClickListener { showMain() }

        // ── Swipe up on the bottom bar (or tap it) to unlock ──
        // Gesture goes on swipeHint specifically — the ScrollView above would
        // otherwise consume swipe events before the root ever sees them.
        fun triggerUnlock() {
            val pin = prefs.getString(PREF_PIN, "") ?: ""
            if (pin.isEmpty()) hideOverlay() else showPin()
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                triggerUnlock(); return true
            }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                if (e1 != null && (e1.y - e2.y) > 80 && Math.abs(vY) > 150) {
                    triggerUnlock(); return true
                }
                return false
            }
        })
        swipeHint.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true  // consume — don't let the ScrollView steal it
        }

        windowManager.addView(view, params)
        overlayView = view

        // Hide the status bar once the view is attached
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.post {
                view.windowInsetsController?.hide(
                    android.view.WindowInsets.Type.statusBars()
                )
            }
        }

        Log.d(TAG, "showOverlay: done")
    }

    fun hideOverlay() {
        clockTick?.let { clockHandler.removeCallbacks(it) }
        clockTick = null
        notifHandler.removeCallbacksAndMessages(null)
        NotificationListener.onChanged = null
        currentPin = ""
        overlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Log.w(TAG, e.message ?: "removeView failed") }
        }
        overlayView = null
        Log.d(TAG, "hideOverlay: done")
    }

    // ── Notifications ────────────────────────────────────────────────────────

    private fun refreshNotifications(container: LinearLayout, emptyView: TextView) {
        val notifs = NotificationListener.notifications
        container.removeAllViews()
        if (notifs.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            return
        }
        emptyView.visibility = View.GONE
        notifs.take(6).forEach { item ->
            if (item.isMedia) {
                inflateMediaCard(container, item, emptyView)
            } else {
                val v = LayoutInflater.from(this).inflate(R.layout.item_notification, container, false)
                v.findViewById<TextView>(R.id.notifApp).text = item.appName.uppercase(Locale.getDefault())
                v.findViewById<TextView>(R.id.notifTitle).text = item.title
                val notifText = v.findViewById<TextView>(R.id.notifText)
                if (item.text.isBlank()) notifText.visibility = View.GONE else notifText.text = item.text
                addSwipeToDismiss(v, item.key, container, emptyView)
                container.addView(v)
            }
        }
    }

    private fun addSwipeToDismiss(card: View, key: String, container: LinearLayout, emptyView: TextView) {
        var startX = 0f
        card.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    v.translationX = dx
                    v.alpha = (1f - Math.abs(dx) / v.width * 1.2f).coerceAtLeast(0.1f)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = event.rawX - startX
                    if (event.actionMasked == MotionEvent.ACTION_UP && Math.abs(dx) > v.width * 0.35f) {
                        val dir = if (dx > 0) 1f else -1f
                        v.animate()
                            .translationX(dir * v.width)
                            .alpha(0f)
                            .setDuration(220)
                            .withEndAction {
                                container.removeView(v)
                                if (container.childCount == 0) emptyView.visibility = View.VISIBLE
                                NotificationListener.dismiss(key)
                            }.start()
                    } else {
                        v.animate().translationX(0f).alpha(1f).setDuration(180).start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun inflateMediaCard(container: LinearLayout, item: NotificationItem, emptyView: TextView) {
        val v = LayoutInflater.from(this).inflate(R.layout.item_notification_media, container, false)

        v.findViewById<TextView>(R.id.notifApp).text = item.appName.uppercase(Locale.getDefault())
        v.findViewById<TextView>(R.id.notifTitle).text = item.title
        val notifText = v.findViewById<TextView>(R.id.notifText)
        if (item.text.isBlank()) notifText.visibility = View.GONE else notifText.text = item.text

        val artView = v.findViewById<android.widget.ImageView>(R.id.albumArt)
        if (item.albumArt != null) artView.setImageBitmap(item.albumArt)

        val actionViews = listOf(
            v.findViewById<TextView>(R.id.mediaAction0),
            v.findViewById<TextView>(R.id.mediaAction1),
            v.findViewById<TextView>(R.id.mediaAction2)
        )

        item.mediaActions.take(3).forEachIndexed { i, (label, pendingIntent) ->
            val labelLower = label.lowercase(Locale.getDefault())
            actionViews[i].apply {
                text = label.uppercase(Locale.getDefault())
                visibility = View.VISIBLE
                setOnClickListener {
                    // Get a fresh live session on every click — avoids stale state after pause/play
                    val liveTransport = try {
                        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                        msm.getActiveSessions(ComponentName(this@LockscreenService, NotificationListener::class.java))
                            .find { it.packageName == item.packageName }
                            ?.transportControls
                    } catch (e: Exception) { null }

                    if (liveTransport != null) {
                        when {
                            "play"  in labelLower -> liveTransport.play()
                            "pause" in labelLower -> liveTransport.pause()
                            "next"  in labelLower -> liveTransport.skipToNext()
                            "prev"  in labelLower -> liveTransport.skipToPrevious()
                            "stop"  in labelLower -> liveTransport.stop()
                            else -> try { pendingIntent?.send() } catch (e: Exception) { }
                        }
                    } else {
                        try { pendingIntent?.send() } catch (e: Exception) { }
                    }
                }
            }
        }
        // Swipe on non-button areas (album art, title, text) to dismiss
        addSwipeToDismiss(v, item.key, container, emptyView)
        container.addView(v)
    }

    // ── Notification channel ─────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Lockscreen Service", NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    companion object {
        private const val TAG = "LockscreenService"
        private const val CHANNEL_ID = "init_lockscreen_service"
        private const val NOTIF_ID = 9001
        const val PREFS = "lockscreen"
        const val PREF_PIN = "pin"
    }
}
