package com.initlauncher.lockscreen

import android.app.PendingIntent
import android.graphics.Bitmap
import android.media.session.MediaSession

data class NotificationItem(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val key: String = "",
    // Media-specific fields (non-null when isMedia = true)
    val isMedia: Boolean = false,
    val albumArt: Bitmap? = null,
    val mediaActions: List<Pair<String, PendingIntent?>> = emptyList(),
    val mediaSessionToken: MediaSession.Token? = null
)
