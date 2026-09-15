package com.asptechinc.daymark.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asptechinc.daymark.utils.NotificationHelper

class ActivityStartReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val activityId = intent.getIntExtra("ACTIVITY_ID", -1)
        val activityName = intent.getStringExtra("ACTIVITY_NAME") ?: "An activity"
        val notificationIndex = intent.getIntExtra("NOTIFICATION_INDEX", 0)

        Log.i(
            "ActivityStartReceiver",
            "Received start notification for activity: $activityName (ID: $activityId, Index: $notificationIndex)",
        )

        if (activityId != -1) {
            NotificationHelper.createNotificationChannel(context)
            NotificationHelper.showActivityStartNotification(context, activityId, activityName, notificationIndex)
        }
    }
}
