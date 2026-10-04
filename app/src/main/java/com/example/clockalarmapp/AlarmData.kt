package com.example.clockalarmapp

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

data class AlarmItem(
    val id: Long = System.currentTimeMillis(),
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val repeatMode: AlarmRepeatMode = AlarmRepeatMode.DAILY,
    val selectedDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val everyOtherDayStartDate: Long = System.currentTimeMillis(),
    val label: String = "",
    val notificationMode: AlarmNotificationMode = AlarmNotificationMode.SOUND_AND_VIBRATION,
    // null = not in any group
    val groupId: Long? = null
)

/**
 * A named collection of alarms. Turning a group off silences every alarm in it without
 * touching each alarm's own on/off switch, so turning the group back on restores them as they were.
 */
data class AlarmGroup(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val enabled: Boolean = true,
    val expanded: Boolean = true
)

enum class AlarmRepeatMode {
    DAILY,
    SPECIFIC_DAYS,
    EVERY_OTHER_DAY
}

enum class AlarmNotificationMode {
    SOUND,
    VIBRATION,
    SOUND_AND_VIBRATION
}

fun AlarmItem.shouldTriggerToday(currentDay: DayOfWeek, currentDate: Long): Boolean {
    if (!enabled) return false

    return when (repeatMode) {
        AlarmRepeatMode.DAILY -> true
        AlarmRepeatMode.SPECIFIC_DAYS -> selectedDays.contains(currentDay)
        AlarmRepeatMode.EVERY_OTHER_DAY -> {
            val startDate = java.util.Calendar.getInstance().apply {
                timeInMillis = everyOtherDayStartDate
            }
            val today = java.util.Calendar.getInstance().apply {
                timeInMillis = currentDate
            }
            val daysDifference = ((today.timeInMillis - startDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            daysDifference % 2 == 0 && selectedDays.contains(currentDay)
        }
    }
}
