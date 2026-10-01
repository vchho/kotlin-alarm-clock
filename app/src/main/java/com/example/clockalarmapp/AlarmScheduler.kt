package com.example.clockalarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.util.Calendar

object AlarmScheduler {
    private const val REQUEST_CODE = 1001
    private const val ACTION_ALARM = "com.example.clockalarmapp.ALARM"

    fun scheduleNext(context: Context, hour: Int, minute: Int, mode: AlarmMode) {
        val triggerTime = getNextTriggerTime(hour, minute, mode)
        scheduleAtTime(context, triggerTime, hour, minute, mode)
    }

    fun scheduleAtTime(
        context: Context,
        triggerTime: Long,
        hour: Int,
        minute: Int,
        mode: AlarmMode
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra("hour", hour)
            putExtra("minute", minute)
            putExtra("mode", mode.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
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

    fun cancel(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.cancel(it)
        }
    }

    private fun getNextTriggerTime(hour: Int, minute: Int, mode: AlarmMode): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, if (mode == AlarmMode.EVERY_OTHER_DAY) 1 else 1)
        }

        return target.timeInMillis
    }
}
