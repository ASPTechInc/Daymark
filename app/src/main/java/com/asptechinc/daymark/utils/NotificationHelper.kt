package com.asptechinc.daymark.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.asptechinc.daymark.MainActivity
import com.asptechinc.daymark.R

object NotificationHelper {
    private const val CHANNEL_ID = "activity_end_channel"
    private const val START_NOTIFICATION_ID_OFFSET = 100000

    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val name = context.getString(R.string.notification_channel_name)
        val descriptionText = context.getString(R.string.notification_channel_description)

        val channel =
            NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showActivityEndNotification(
        context: Context,
        activityId: Int,
        activityName: String,
    ) {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                activityId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_info)
                .setContentTitle(context.getString(R.string.notification_title_end))
                .setContentText(context.getString(R.string.notification_content_end, activityName))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(activityId, builder.build())
    }

    fun showActivityStartNotification(
        context: Context,
        activityId: Int,
        activityName: String,
        notificationIndex: Int,
    ) {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                activityId + START_NOTIFICATION_ID_OFFSET, // Use a different ID to avoid collision with end notification
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val periodText =
            when (notificationIndex) {
                1 -> context.getString(R.string.notification_period_week)
                2 -> context.getString(R.string.notification_period_tomorrow)
                3 -> context.getString(R.string.notification_period_today)
                else -> ""
            }

        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_info)
                .setContentTitle(context.getString(R.string.notification_title_start))
                .setContentText(
                    context.getString(
                        R.string.notification_content_start,
                        activityName,
                        periodText,
                    ),
                ).setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(activityId + START_NOTIFICATION_ID_OFFSET, builder.build())
    }
}
