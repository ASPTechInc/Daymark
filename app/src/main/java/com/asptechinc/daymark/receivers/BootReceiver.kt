package com.asptechinc.daymark.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asptechinc.daymark.utils.AlarmHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device rebooted, rescheduling alarms")
            AlarmHelper.rescheduleAllAlarms(context)
        }
    }
}
