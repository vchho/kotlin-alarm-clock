package com.example.clockalarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.util.Calendar

object AlarmScheduler {

    fun scheduleAlarm(context: Context, alarm: AlarmItem) {
        if (!alarm.enabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        val triggerTime = getNextTriggerTime(alarm)
        val requestCode = alarm.id.toInt()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.clockalarmapp.ALARM"
            putExtra("alarm_id", alarm.id)
            putExtra("hour", alarm.hour)
            putExtra("minute", alarm.minute)
            putExtra("label", alarm.label)
            putExtra("repeat_mode", alarm.repeatMode.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            val requestIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            requestIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(requestIntent)
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.clockalarmapp.ALARM"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.cancel(it)
        }
    }

    private fun getNextTriggerTime(alarm: AlarmItem): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the time has already passed today, start checking from tomorrow
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Find the next day this alarm should trigger
        while (!alarm.shouldTriggerToday(
            target.get(Calendar.DAY_OF_WEEK).toJavaDayOfWeek(),
            target.timeInMillis
        )) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }
}

private fun Int.toJavaDayOfWeek(): java.time.DayOfWeek {
    // Calendar.DAY_OF_WEEK: SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
    // DayOfWeek: MONDAY = 1, TUESDAY = 2, ..., SUNDAY = 7
    return when (this) {
        Calendar.MONDAY -> java.time.DayOfWeek.MONDAY
        Calendar.TUESDAY -> java.time.DayOfWeek.TUESDAY
        Calendar.WEDNESDAY -> java.time.DayOfWeek.WEDNESDAY
        Calendar.THURSDAY -> java.time.DayOfWeek.THURSDAY
        Calendar.FRIDAY -> java.time.DayOfWeek.FRIDAY
        Calendar.SATURDAY -> java.time.DayOfWeek.SATURDAY
        Calendar.SUNDAY -> java.time.DayOfWeek.SUNDAY
        else -> java.time.DayOfWeek.MONDAY
    }
}
