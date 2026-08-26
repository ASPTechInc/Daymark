package com.asptechinc.daymark.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.asptechinc.daymark.R
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.data.AppDatabase
import com.asptechinc.daymark.receivers.ActivityEndReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmHelper {
    fun scheduleActivityEndAlarm(
        context: Context,
        activityId: Int,
        activityName: String,
        endTime: LocalDateTime
    ) {
        val prefs = context.getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
        val notificationsEnabled =
            prefs.getBoolean(context.getString(R.string.settings_key_notifications), true)

        if (!notificationsEnabled) {
            Log.i("AlarmHelper", "Notifications disabled, not scheduling alarm")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ActivityEndReceiver::class.java).apply {
            putExtra("ACTIVITY_ID", activityId)
            putExtra("ACTIVITY_NAME", activityName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            activityId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val triggerAtMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        if (triggerAtMillis > now) {
            Log.i("AlarmHelper", "Scheduling alarm for activity: $activityName at $endTime")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            Log.i("AlarmHelper", "Not scheduling alarm for past end time: $endTime")
        }
    }

    fun cancelActivityEndAlarm(context: Context, activityId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ActivityEndReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            activityId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        if (pendingIntent != null) {
            Log.i("AlarmHelper", "Canceling alarm for activity ID: $activityId")
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAllAlarms(context: Context) {
        Log.i("AlarmHelper", "Rescheduling all alarms")
        val db = AppDatabase.getDatabase(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            val activities = db.activityDao().getAll()
            val now = LocalDateTime.now()

            activities.forEach { activity ->
                activity.endDateTime?.let { endTime ->
                    if (endTime.isAfter(now)) {
                        scheduleActivityEndAlarm(
                            context,
                            activity.id,
                            activity.activityName,
                            endTime
                        )
                    }
                }
            }
        }
    }

    fun cancelAllAlarms(context: Context) {
        Log.i("AlarmHelper", "Canceling all alarms")
        val db = AppDatabase.getDatabase(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            val activities = db.activityDao().getAll()
            activities.forEach { activity ->
                cancelActivityEndAlarm(context, activity.id)
            }
        }
    }
}
