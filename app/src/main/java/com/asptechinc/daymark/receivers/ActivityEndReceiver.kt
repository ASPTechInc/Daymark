package com.asptechinc.daymark.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asptechinc.daymark.utils.NotificationHelper

class ActivityEndReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val activityId = intent.getIntExtra("ACTIVITY_ID", -1)
        val activityName = intent.getStringExtra("ACTIVITY_NAME") ?: "An activity"

        Log.i("ActivityEndReceiver", "Received end notification for activity: $activityName (ID: $activityId)")

        if (activityId != -1) {
            NotificationHelper.createNotificationChannel(context)
            NotificationHelper.showActivityEndNotification(context, activityId, activityName)
        }
    }
}
