package com.example.clockalarmapp

import android.Manifest
import android.app.Activity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockAlarmDashboardScreen(context: Context) {
    val currentTime = remember { mutableStateOf(getCurrentTime()) }
    val timePickerState = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = false)
    var enabled by remember { mutableStateOf(true) }
    var repeatMode by remember { mutableStateOf(AlarmMode.DAILY) }
    var statusText by remember { mutableStateOf("Alarm not set") }

    val currentTimeUpdater = rememberUpdatedState(currentTime.value)

    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = getCurrentTime()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1020),
                        Color(0xFF111B2F),
                        Color(0xFF16253D)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101C34))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Clock",
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121B2E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = Color(0xFF7C9BFF),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Alarm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

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

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = repeatMode == AlarmMode.DAILY,
                            onClick = { repeatMode = AlarmMode.DAILY },
                            label = { Text("Daily") }
                        )

                        FilterChip(
                            selected = repeatMode == AlarmMode.EVERY_OTHER_DAY,
                            onClick = { repeatMode = AlarmMode.EVERY_OTHER_DAY },
                            label = { Text("Every Other Day") }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (!enabled) {
                                AlarmScheduler.cancel(context)
                                statusText = "Alarm disabled"
                                return@Button
                            }

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

                            val hour = timePickerState.hour
                            val minute = timePickerState.minute
                            AlarmScheduler.scheduleNext(context, hour, minute, repeatMode)
                            val label = when (repeatMode) {
                                AlarmMode.DAILY -> "Daily"
                                AlarmMode.EVERY_OTHER_DAY -> "Every Other Day"
                            }
                            statusText = "Alarm set for ${formatClock(hour, minute)} ($label)"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(text = "Save Alarm")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF92D5C3),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = statusText,
                            color = Color(0xFFB9F5DE),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    return sdf.format(Calendar.getInstance().time)
}

private fun formatClock(hour: Int, minute: Int): String {
    val suffix = if (hour >= 12) "PM" else "AM"
    val convertedHour = if (hour % 12 == 0) 12 else hour % 12
    return "${convertedHour}:${String.format("%02d", minute)} $suffix"
}
