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
import com.asptechinc.daymark.receivers.ActivityStartReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmHelper {
    private const val START_ALARM_ID_OFFSET = 100000

    fun scheduleActivityEndAlarm(
        context: Context,
        activityId: Int,
        activityName: String,
        endTime: LocalDateTime,
    ) {
        val prefs = context.getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
        val notificationsEnabled =
            prefs.getBoolean(context.getString(R.string.settings_key_notifications), true)

        if (!notificationsEnabled) {
            Log.i("AlarmHelper", "Notifications disabled, not scheduling alarm")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent =
            Intent(context, ActivityEndReceiver::class.java).apply {
                putExtra("ACTIVITY_ID", activityId)
                putExtra("ACTIVITY_NAME", activityName)
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                activityId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val triggerAtMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        if (triggerAtMillis > now) {
            Log.i("AlarmHelper", "Scheduling alarm for activity: $activityName at $endTime")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } else {
            Log.i("AlarmHelper", "Not scheduling alarm for past end time: $endTime")
        }
    }

    fun scheduleActivityStartAlarm(
        context: Context,
        activityId: Int,
        activityName: String,
        startTime: LocalDateTime,
    ) {
        val prefs = context.getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
        val notificationsEnabled =
            prefs.getBoolean(context.getString(R.string.settings_key_notifications), true)

        if (!notificationsEnabled) {
            Log.i("AlarmHelper", "Notifications disabled, not scheduling start alarm")
            return
        }

        val notificationIndex = ActivityNotificationManager.getSavedNotificationIndex(context)
        if (notificationIndex == 0) { // None
            Log.i("AlarmHelper", "Start notification set to None, not scheduling")
            return
        }

        val triggerTime =
            when (notificationIndex) {
                1 -> startTime.minusWeeks(1)
                2 -> startTime.minusDays(1)
                3 -> startTime
                else -> null
            }

        if (triggerTime == null) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent =
            Intent(context, ActivityStartReceiver::class.java).apply {
                putExtra("ACTIVITY_ID", activityId)
                putExtra("ACTIVITY_NAME", activityName)
                putExtra("NOTIFICATION_INDEX", notificationIndex)
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                activityId + START_ALARM_ID_OFFSET, // Different ID to avoid collision
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val triggerAtMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val startTimeMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Only schedule if the event is in the future
        if (startTimeMillis > now) {
            val isSameDayAsTrigger =
                triggerTime.toLocalDate().isEqual(LocalDateTime.now().toLocalDate())

            if (triggerAtMillis > now || isSameDayAsTrigger) {
                // If the trigger time is in the past, but it's still the same day, trigger in 1 second
                val finalTriggerMillis = if (triggerAtMillis > now) triggerAtMillis else now + 1000

                Log.i(
                    "AlarmHelper",
                    "Scheduling start alarm for activity: $activityName at $triggerTime (Actual trigger: ${
                        LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(finalTriggerMillis),
                            ZoneId.systemDefault(),
                        )
                    })",
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        finalTriggerMillis,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        finalTriggerMillis,
                        pendingIntent,
                    )
                }
            } else {
                Log.i("AlarmHelper", "Not scheduling start alarm for past time/day: $triggerTime")
            }
        } else {
            Log.i("AlarmHelper", "Not scheduling start alarm for past activity: $activityName")
        }
    }

    fun cancelActivityEndAlarm(
        context: Context,
        activityId: Int,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ActivityEndReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                activityId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
            )

        if (pendingIntent != null) {
            Log.i("AlarmHelper", "Canceling end alarm for activity ID: $activityId")
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelActivityStartAlarm(
        context: Context,
        activityId: Int,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ActivityStartReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                activityId + START_ALARM_ID_OFFSET,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
            )

        if (pendingIntent != null) {
            Log.i("AlarmHelper", "Canceling start alarm for activity ID: $activityId")
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
                // Start alarms
                scheduleActivityStartAlarm(
                    context,
                    activity.id,
                    activity.activityName,
                    activity.startDateTime,
                )

                // End alarms
                activity.endDateTime?.let { endTime ->
                    if (endTime.isAfter(now)) {
                        scheduleActivityEndAlarm(
                            context,
                            activity.id,
                            activity.activityName,
                            endTime,
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
                cancelActivityStartAlarm(context, activity.id)
                cancelActivityEndAlarm(context, activity.id)
            }
        }
    }
}
