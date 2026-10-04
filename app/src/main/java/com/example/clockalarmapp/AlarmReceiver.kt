package com.example.clockalarmapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", 0L)
        val hour = intent.getIntExtra("hour", 8)
        val minute = intent.getIntExtra("minute", 0)
        val label = intent.getStringExtra("label") ?: ""
        val notificationModeName = intent.getStringExtra("notification_mode") ?: AlarmNotificationMode.SOUND_AND_VIBRATION.name

        // Don't ring if the alarm (or its group) was switched off or deleted after this was scheduled.
        val repository = AlarmRepository(context)
        val alarm = repository.alarms.value.find { it.id == alarmId }
        if (alarm != null && !repository.isEffectivelyEnabled(alarm)) return

        // Start the alarm service to play sound/vibration repeatedly
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("hour", hour)
            putExtra("minute", minute)
            putExtra("label", label)
            putExtra("notification_mode", notificationModeName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Reschedule the alarm for the next occurrence
        if (alarm != null) {
            AlarmScheduler.scheduleAlarm(context, repository.effective(alarm))
        }
    }
}
