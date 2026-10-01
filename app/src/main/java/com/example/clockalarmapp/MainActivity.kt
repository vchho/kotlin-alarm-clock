package com.example.clockalarmapp

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClockAlarmDashboardTheme {
                ClockAlarmDashboardScreen(context = this)
            }
        }
    }
}

@Composable
fun ClockAlarmDashboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7C9BFF),
            secondary = Color(0xFF92D5C3),
            background = Color(0xFF0B1020),
            surface = Color(0xFF121B2E),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun ClockAlarmDashboardScreen(context: Context) {
    val currentTime = remember { mutableStateOf(getCurrentTime()) }
    var showAddAlarmDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    val repository = remember { AlarmRepository(context) }
    val alarms by repository.alarms.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = getCurrentTime()
            delay(1000)
        }
    }

    LaunchedEffect(alarms) {
        alarms.forEach { alarm ->
            if (alarm.enabled) AlarmScheduler.scheduleAlarm(context, alarm)
            else AlarmScheduler.cancelAlarm(context, alarm.id)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1020), Color(0xFF111B2F), Color(0xFF16253D))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101C34))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Time",
                        color = Color(0xFF9CB6FF),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentTime.value,
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Alarms",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        repository = repository,
                        context = context,
                        onEdit = { editingAlarm = alarm }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddAlarmDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Color(0xFF7C9BFF)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm", tint = Color.White)
        }
    }

    if (showAddAlarmDialog) {
        AlarmEditorDialog(
            repository = repository,
            context = context,
            existingAlarm = null,
            onDismiss = { showAddAlarmDialog = false }
        )
    }

    if (editingAlarm != null) {
        AlarmEditorDialog(
            repository = repository,
            context = context,
            existingAlarm = editingAlarm,
            onDismiss = { editingAlarm = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorDialog(
    repository: AlarmRepository,
    context: Context,
    existingAlarm: AlarmItem?,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = existingAlarm?.hour ?: 8,
        initialMinute = existingAlarm?.minute ?: 0,
        is24Hour = false
    )
    var label by remember(existingAlarm?.id) { mutableStateOf(existingAlarm?.label ?: "") }
    var repeatMode by remember(existingAlarm?.id) {
        mutableStateOf(existingAlarm?.repeatMode ?: AlarmRepeatMode.DAILY)
    }
    var selectedDays by remember(existingAlarm?.id) {
        mutableStateOf(existingAlarm?.selectedDays ?: DayOfWeek.values().toSet())
    }
    var startDate by remember(existingAlarm?.id) {
        mutableStateOf(existingAlarm?.everyOtherDayStartDate ?: todayAtStartOfDay())
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121B2E))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (existingAlarm == null) "Add Alarm" else "Edit Alarm",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("Alarm label (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF1F2B46),
                        focusedContainerColor = Color(0xFF1F2B46),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = Color(0xFF7C9BFF)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TimePickerDefaults.colors(
                        clockDialColor = Color(0xFF1F2B46),
                        selectorColor = Color(0xFF7C9BFF),
                        containerColor = Color(0xFF101C34),
                        periodSelectorSelectedContainerColor = Color(0xFF7C9BFF),
                        periodSelectorSelectedContentColor = Color.White,
                        periodSelectorUnselectedContentColor = Color(0xFFB9C9FF),
                        timeSelectorSelectedContainerColor = Color(0xFF7C9BFF),
                        timeSelectorSelectedContentColor = Color.White,
                        timeSelectorUnselectedContentColor = Color(0xFFB9C9FF),
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = Color(0xFFCED9FF)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Repeat",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = repeatMode == AlarmRepeatMode.DAILY,
                        onClick = { repeatMode = AlarmRepeatMode.DAILY },
                        label = { Text("Daily") }
                    )
                    FilterChip(
                        selected = repeatMode == AlarmRepeatMode.SPECIFIC_DAYS,
                        onClick = { repeatMode = AlarmRepeatMode.SPECIFIC_DAYS },
                        label = { Text("Days") }
                    )
                    FilterChip(
                        selected = repeatMode == AlarmRepeatMode.EVERY_OTHER_DAY,
                        onClick = { repeatMode = AlarmRepeatMode.EVERY_OTHER_DAY },
                        label = { Text("Every Other") }
                    )
                }

                if (repeatMode == AlarmRepeatMode.SPECIFIC_DAYS || repeatMode == AlarmRepeatMode.EVERY_OTHER_DAY) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select Days",
                        color = Color(0xFF9CB6FF),
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labels = mapOf(
                            DayOfWeek.MONDAY to "Mon", DayOfWeek.TUESDAY to "Tue",
                            DayOfWeek.WEDNESDAY to "Wed", DayOfWeek.THURSDAY to "Thu",
                            DayOfWeek.FRIDAY to "Fri", DayOfWeek.SATURDAY to "Sat",
                            DayOfWeek.SUNDAY to "Sun"
                        )
                        DayOfWeek.values().forEach { day ->
                            FilterChip(
                                selected = day in selectedDays,
                                onClick = {
                                    selectedDays = if (day in selectedDays) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                },
                                label = { Text(labels[day] ?: "", fontSize = 10.sp) },
                                modifier = Modifier.size(width = 46.dp, height = 32.dp)
                            )
                        }
                    }
                }

                if (repeatMode == AlarmRepeatMode.EVERY_OTHER_DAY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    StartDatePicker(
                        selectedDate = startDate,
                        onDateSelected = { startDate = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Cancel")
                    }
                    Button(
                        onClick = {
                            val alarmToSave = AlarmItem(
                                id = existingAlarm?.id ?: System.currentTimeMillis(),
                                hour = timePickerState.hour,
                                minute = timePickerState.minute,
                                enabled = existingAlarm?.enabled ?: true,
                                repeatMode = repeatMode,
                                selectedDays = selectedDays,
                                everyOtherDayStartDate = startDate,
                                label = label
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                (context as? Activity)?.requestPermissions(
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                    100
                                )
                            }
                            repository.saveAlarm(alarmToSave)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun StartDatePicker(selectedDate: Long, onDateSelected: (Long) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val dateText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)

    Button(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val chosenDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onDateSelected(chosenDate.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = "Start date: $dateText")
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmItem,
    repository: AlarmRepository,
    context: Context,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121B2E))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatClock(alarm.hour, alarm.minute),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        color = Color(0xFF92D5C3),
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = getRepeatText(alarm),
                    color = Color(0xFF9CB6FF),
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = alarm.enabled,
                onCheckedChange = { repository.saveAlarm(alarm.copy(enabled = it)) }
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Alarm",
                    tint = Color(0xFF92D5C3),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = {
                    AlarmScheduler.cancelAlarm(context, alarm.id)
                    repository.deleteAlarm(alarm.id)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Alarm",
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun todayAtStartOfDay(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun getCurrentTime(): String =
    SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Calendar.getInstance().time)

private fun formatClock(hour: Int, minute: Int): String {
    val suffix = if (hour >= 12) "PM" else "AM"
    val convertedHour = if (hour % 12 == 0) 12 else hour % 12
    return "${convertedHour}:${String.format("%02d", minute)} $suffix"
}

private fun formatDate(timeInMillis: Long): String =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(timeInMillis)

private fun getRepeatText(alarm: AlarmItem): String {
    val labels = mapOf(
        DayOfWeek.MONDAY to "Mon", DayOfWeek.TUESDAY to "Tue",
        DayOfWeek.WEDNESDAY to "Wed", DayOfWeek.THURSDAY to "Thu",
        DayOfWeek.FRIDAY to "Fri", DayOfWeek.SATURDAY to "Sat",
        DayOfWeek.SUNDAY to "Sun"
    )
    return when (alarm.repeatMode) {
        AlarmRepeatMode.DAILY -> "Daily"
        AlarmRepeatMode.SPECIFIC_DAYS ->
            "On: " + alarm.selectedDays.sortedBy { it.value }.mapNotNull(labels::get).joinToString(", ")
        AlarmRepeatMode.EVERY_OTHER_DAY ->
            "Every other: " + alarm.selectedDays.sortedBy { it.value }.mapNotNull(labels::get).joinToString(", ") +
                    " from " + formatDate(alarm.everyOtherDayStartDate)
    }
}
