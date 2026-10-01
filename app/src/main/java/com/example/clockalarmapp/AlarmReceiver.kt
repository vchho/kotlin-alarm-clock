package com.example.clockalarmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", 0L)
        val hour = intent.getIntExtra("hour", 8)
        val minute = intent.getIntExtra("minute", 0)
        val label = intent.getStringExtra("label") ?: ""

        showNotification(context, label, hour, minute)

        // Reschedule the alarm for the next occurrence
        val repository = AlarmRepository(context)
        val alarm = repository.alarms.value.find { it.id == alarmId }
        if (alarm != null) {
            AlarmScheduler.scheduleAlarm(context, alarm)
        }
    }

    private fun showNotification(context: Context, label: String, hour: Int, minute: Int) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channelId = "alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmText = if (label.isNotEmpty()) {
            "$label - ${formatClock(hour, minute)}"
        } else {
            formatClock(hour, minute)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm")
            .setContentText(alarmText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun formatClock(hour: Int, minute: Int): String {
        val suffix = if (hour >= 12) "PM" else "AM"
        val convertedHour = if (hour % 12 == 0) 12 else hour % 12
        return "${convertedHour}:${String.format("%02d", minute)} $suffix"
    }
}
