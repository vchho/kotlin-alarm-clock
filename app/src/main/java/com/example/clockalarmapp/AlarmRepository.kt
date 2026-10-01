package com.example.clockalarmapp

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

class AlarmRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    init {
        loadAlarms()
    }

    fun loadAlarms() {
        val json = prefs.getString("alarms_list", "[]")
        val alarmList = mutableListOf<AlarmItem>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val alarm = deserializeAlarm(obj)
                alarmList.add(alarm)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _alarms.value = alarmList
    }

    fun saveAlarm(alarm: AlarmItem) {
        val currentList = _alarms.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            currentList[index] = alarm
        } else {
            currentList.add(alarm)
        }
        _alarms.value = currentList
        persistAlarms(currentList)
    }

    fun deleteAlarm(alarmId: Long) {
        val currentList = _alarms.value.filter { it.id != alarmId }
        _alarms.value = currentList
        persistAlarms(currentList)
    }

    private fun persistAlarms(alarms: List<AlarmItem>) {
        val jsonArray = JSONArray()
        for (alarm in alarms) {
            jsonArray.put(serializeAlarm(alarm))
        }
        prefs.edit().putString("alarms_list", jsonArray.toString()).apply()
    }

    private fun serializeAlarm(alarm: AlarmItem): JSONObject {
        val obj = JSONObject()
        obj.put("id", alarm.id)
        obj.put("hour", alarm.hour)
        obj.put("minute", alarm.minute)
        obj.put("enabled", alarm.enabled)
        obj.put("repeatMode", alarm.repeatMode.name)
        obj.put("label", alarm.label)
        obj.put("everyOtherDayStartDate", alarm.everyOtherDayStartDate)
        val daysArray = JSONArray()
        alarm.selectedDays.forEach { day ->
            daysArray.put(day.value)
        }
        obj.put("selectedDays", daysArray)
        return obj
    }

    private fun deserializeAlarm(obj: JSONObject): AlarmItem {
        val id = obj.getLong("id")
        val hour = obj.getInt("hour")
        val minute = obj.getInt("minute")
        val enabled = obj.getBoolean("enabled")
        val repeatMode = AlarmRepeatMode.valueOf(obj.getString("repeatMode"))
        val label = obj.optString("label", "")
        val everyOtherDayStartDate = obj.optLong("everyOtherDayStartDate", System.currentTimeMillis())
        val daysArray = obj.getJSONArray("selectedDays")
        val selectedDays = mutableSetOf<DayOfWeek>()
        for (i in 0 until daysArray.length()) {
            val dayValue = daysArray.getInt(i)
            selectedDays.add(DayOfWeek.of(dayValue))
        }
        return AlarmItem(
            id = id,
            hour = hour,
            minute = minute,
            enabled = enabled,
            repeatMode = repeatMode,
            selectedDays = selectedDays,
            everyOtherDayStartDate = everyOtherDayStartDate,
            label = label
        )
    }
}
