package com.initlauncher.lockscreen

import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.session.MediaSession
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener : NotificationListenerService() {

    companion object {
        private val _notifications = mutableListOf<NotificationItem>()
        val notifications: List<NotificationItem>
            get() = synchronized(_notifications) { _notifications.toList() }

        var onChanged: (() -> Unit)? = null

        private var instance: NotificationListener? = null

        fun dismiss(key: String) {
            instance?.cancelNotification(key)
        }
    }

    override fun onListenerConnected() { instance = this; refresh() }
    override fun onListenerDisconnected() { instance = null }
    override fun onNotificationPosted(sbn: StatusBarNotification) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification) = refresh()

    private fun refresh() {
        val pm = packageManager
        synchronized(_notifications) {
            _notifications.clear()
            activeNotifications?.forEach { sbn ->
                if (sbn.isOngoing && !isMedia(sbn)) return@forEach
                if (sbn.packageName == packageName) return@forEach

                val extras = sbn.notification.extras ?: return@forEach

                val title = extras.getString(Notification.EXTRA_TITLE)
                    ?: extras.getString(Notification.EXTRA_CONVERSATION_TITLE)
                    ?: return@forEach  // nothing useful to show

                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    ?: ""

                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
                } catch (e: PackageManager.NameNotFoundException) { sbn.packageName }

                if (isMedia(sbn)) {
                    val albumArt = extractAlbumArt(sbn)
                    val actions = sbn.notification.actions
                        ?.map { Pair(it.title?.toString() ?: "", it.actionIntent) }
                        ?: emptyList()
                    val token = sbn.notification.extras
                        .getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)

                    _notifications.add(
                        NotificationItem(sbn.packageName, appName, title, text,
                            sbn.postTime, key = sbn.key, isMedia = true, albumArt = albumArt,
                            mediaActions = actions, mediaSessionToken = token)
                    )
                } else {
                    _notifications.add(
                        NotificationItem(sbn.packageName, appName, title, text,
                            sbn.postTime, key = sbn.key)
                    )
                }
            }
            _notifications.sortWith(compareByDescending<NotificationItem> { it.isMedia }
                .thenByDescending { it.postTime })
        }
        onChanged?.invoke()
    }

    private fun isMedia(sbn: StatusBarNotification): Boolean {
        val extras = sbn.notification.extras ?: return false
        return extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
               sbn.notification.category == Notification.CATEGORY_TRANSPORT
    }

    private fun extractAlbumArt(sbn: StatusBarNotification): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sbn.notification.getLargeIcon()?.loadDrawable(this)?.let { drawable ->
                return (drawable as? BitmapDrawable)?.bitmap
            }
        }
        return null
    }
}
