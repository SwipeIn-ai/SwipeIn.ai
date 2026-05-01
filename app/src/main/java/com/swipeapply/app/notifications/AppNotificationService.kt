package com.swipeapply.app.notifications

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.swipeapply.app.MainActivity

object AppNotificationService {

    const val CHANNEL_MATCHES = "job_matches"
    const val CHANNEL_FOLLOW_UP = "follow_ups"
    private const val GROUP_MATCHES = "com.swipeapply.GROUP_MATCHES"
    private const val GROUP_FOLLOW_UPS = "com.swipeapply.GROUP_FOLLOW_UPS"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        val matchesChannel = NotificationChannel(
            CHANNEL_MATCHES,
            "High-Match Jobs",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for strong-fit jobs and opportunities"
        }

        val followUpChannel = NotificationChannel(
            CHANNEL_FOLLOW_UP,
            "Follow-up Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for saved jobs and intro follow-ups"
        }

        manager.createNotificationChannel(matchesChannel)
        manager.createNotificationChannel(followUpChannel)
    }

    fun canNotify(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun post(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String
    ) {
        if (!canNotify(context)) return

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val groupKey = when (channelId) {
            CHANNEL_MATCHES -> GROUP_MATCHES
            CHANNEL_FOLLOW_UP -> GROUP_FOLLOW_UPS
            else -> null
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .apply { if (groupKey != null) setGroup(groupKey) }
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission may be revoked between check and notify.
        }
    }
}
