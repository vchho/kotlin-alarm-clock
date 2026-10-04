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
    private val _groups = MutableStateFlow<List<AlarmGroup>>(emptyList())
    val groups: StateFlow<List<AlarmGroup>> = _groups.asStateFlow()

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

        val groupList = mutableListOf<AlarmGroup>()
        try {
            val groupArray = JSONArray(prefs.getString("groups_list", "[]"))
            for (i in 0 until groupArray.length()) {
                groupList.add(deserializeGroup(groupArray.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _groups.value = groupList
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

    // ---- Groups -----------------------------------------------------------

    fun createGroup(name: String): AlarmGroup {
        val group = AlarmGroup(name = name.trim())
        saveGroup(group)
        return group
    }

    fun saveGroup(group: AlarmGroup) {
        val list = _groups.value.toMutableList()
        val index = list.indexOfFirst { it.id == group.id }
        if (index >= 0) list[index] = group else list.add(group)
        _groups.value = list
        persistGroups(list)
    }

    fun setGroupEnabled(groupId: Long, enabled: Boolean) {
        _groups.value.find { it.id == groupId }?.let { saveGroup(it.copy(enabled = enabled)) }
    }

    fun alarmsInGroup(groupId: Long): List<AlarmItem> =
        _alarms.value.filter { it.groupId == groupId }

    /**
     * Deletes a group. If [deleteAlarms] is true every alarm in it is deleted too;
     * otherwise the alarms are kept and moved out of the group. An alarm that was silenced
     * only because its group was off stays off after being ungrouped.
     */
    fun deleteGroup(groupId: Long, deleteAlarms: Boolean) {
        val group = _groups.value.find { it.id == groupId }
        val newAlarms = if (deleteAlarms) {
            _alarms.value.filter { it.groupId != groupId }
        } else {
            _alarms.value.map {
                if (it.groupId == groupId) {
                    it.copy(groupId = null, enabled = it.enabled && (group?.enabled ?: true))
                } else it
            }
        }
        val newGroups = _groups.value.filter { it.id != groupId }
        _alarms.value = newAlarms
        _groups.value = newGroups
        persistAlarms(newAlarms)
        persistGroups(newGroups)
    }

    /** True if the alarm is on AND its group (if any) is on. This is what decides whether it rings. */
    fun isEffectivelyEnabled(alarm: AlarmItem): Boolean {
        if (!alarm.enabled) return false
        val gid = alarm.groupId ?: return true
        return _groups.value.find { it.id == gid }?.enabled ?: true
    }

    /** Copy of the alarm whose `enabled` already accounts for its group; safe to hand to AlarmScheduler. */
    fun effective(alarm: AlarmItem): AlarmItem = alarm.copy(enabled = isEffectivelyEnabled(alarm))

    private fun persistGroups(groups: List<AlarmGroup>) {
        val arr = JSONArray()
        for (g in groups) {
            arr.put(JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("enabled", g.enabled)
                put("expanded", g.expanded)
            })
        }
        prefs.edit().putString("groups_list", arr.toString()).apply()
    }

    private fun deserializeGroup(obj: JSONObject) = AlarmGroup(
        id = obj.getLong("id"),
        name = obj.optString("name", "Group"),
        enabled = obj.optBoolean("enabled", true),
        expanded = obj.optBoolean("expanded", true)
    )

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
        obj.put("notificationMode", alarm.notificationMode.name)
        alarm.groupId?.let { obj.put("groupId", it) }
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
        val notificationModeName = obj.optString("notificationMode", AlarmNotificationMode.SOUND_AND_VIBRATION.name)
        val notificationMode = try {
            AlarmNotificationMode.valueOf(notificationModeName)
        } catch (e: Exception) {
            AlarmNotificationMode.SOUND_AND_VIBRATION
        }
        val daysArray = obj.getJSONArray("selectedDays")
        val selectedDays = mutableSetOf<DayOfWeek>()
        for (i in 0 until daysArray.length()) {
            val dayValue = daysArray.getInt(i)
            selectedDays.add(DayOfWeek.of(dayValue))
        }
        val groupId = if (obj.has("groupId") && !obj.isNull("groupId")) obj.getLong("groupId") else null
        return AlarmItem(
            id = id,
            hour = hour,
            minute = minute,
            enabled = enabled,
            repeatMode = repeatMode,
            selectedDays = selectedDays,
            everyOtherDayStartDate = everyOtherDayStartDate,
            label = label,
            notificationMode = notificationMode,
            groupId = groupId
        )
    }
}
