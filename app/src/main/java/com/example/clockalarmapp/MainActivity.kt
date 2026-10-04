package com.example.clockalarmapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    // Survives rotation, so a running timer isn't lost.
    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClockAlarmDashboardTheme {
                ClockApp(context = this, timerViewModel = timerViewModel)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Theme: Material You (dynamic color on Android 12+, static M3 fallback below)
// ---------------------------------------------------------------------------

private val FallbackLightColors = lightColorScheme(
    primary = Color(0xFF3F5AA9),
    secondary = Color(0xFF585E71),
    tertiary = Color(0xFF745470)
)

private val FallbackDarkColors = darkColorScheme(
    primary = Color(0xFFB2C5FF),
    secondary = Color(0xFFC0C6DC),
    tertiary = Color(0xFFE3BADA)
)

@Composable
fun ClockAlarmDashboardTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDarkColors
        else -> FallbackLightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

// ---------------------------------------------------------------------------
// Root: bottom tab navigation (Alarms | Timer)
// ---------------------------------------------------------------------------

private enum class ClockTab(val label: String, val icon: ImageVector) {
    Alarms("Alarms", Icons.Default.Alarm),
    Timer("Timer", Icons.Default.Timer)
}

@Composable
fun ClockApp(context: Context, timerViewModel: TimerViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(ClockTab.Alarms) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                ClockTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { outerPadding ->
        // Each tab handles its own status-bar inset; only the nav bar's height is applied here.
        Box(modifier = Modifier.padding(bottom = outerPadding.calculateBottomPadding())) {
            when (selectedTab) {
                ClockTab.Alarms -> ClockAlarmDashboardScreen(context = context)
                ClockTab.Timer -> TimerScreen(viewModel = timerViewModel)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Alarms tab
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockAlarmDashboardScreen(context: Context) {
    val currentTime = remember { mutableStateOf(getCurrentTime()) }
    val currentDate = remember { mutableStateOf(getCurrentDate()) }
    var showAddAlarmSheet by remember { mutableStateOf(false) }
    var addAlarmGroupId by remember { mutableStateOf<Long?>(null) }
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var renamingGroup by remember { mutableStateOf<AlarmGroup?>(null) }
    var deletingGroup by remember { mutableStateOf<AlarmGroup?>(null) }
    val repository = remember { AlarmRepository(context) }
    val alarms by repository.alarms.collectAsState()
    val groups by repository.groups.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = getCurrentTime()
            currentDate.value = getCurrentDate()
            delay(1000)
        }
    }

    // Re-sync schedules whenever an alarm OR a group changes. An alarm only rings if it is on
    // and its group (if any) is on.
    LaunchedEffect(alarms, groups) {
        alarms.forEach { alarm ->
            val effective = repository.effective(alarm)
            if (effective.enabled) AlarmScheduler.scheduleAlarm(context, effective)
            else AlarmScheduler.cancelAlarm(context, alarm.id)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTime.value,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentDate.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showNewGroupDialog = true },
                    icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                    text = { Text("New group") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                ExtendedFloatingActionButton(
                    onClick = {
                        addAlarmGroupId = null
                        showAddAlarmSheet = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add alarm") }
                )
            }
        }
    ) { innerPadding ->
        if (alarms.isEmpty() && groups.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            val groupIds = groups.map { it.id }.toSet()
            // Alarms with no group (or whose group no longer exists) are shown below the groups.
            val ungrouped = alarms.filter { it.groupId == null || it.groupId !in groupIds }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    // leave room so the last card isn't hidden behind the two FABs
                    bottom = innerPadding.calculateBottomPadding() + 168.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { "group_${it.id}" }) { group ->
                    GroupCard(
                        group = group,
                        groupAlarms = alarms.filter { it.groupId == group.id },
                        repository = repository,
                        context = context,
                        onEditAlarm = { editingAlarm = it },
                        onAddAlarm = {
                            addAlarmGroupId = group.id
                            showAddAlarmSheet = true
                        },
                        onRename = { renamingGroup = group },
                        onDelete = { deletingGroup = group }
                    )
                }
                if (groups.isNotEmpty() && ungrouped.isNotEmpty()) {
                    item(key = "ungrouped_header") {
                        Text(
                            text = "Ungrouped",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                        )
                    }
                }
                items(ungrouped, key = { "alarm_${it.id}" }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        repository = repository,
                        context = context,
                        onEdit = { editingAlarm = alarm }
                    )
                }
            }
        }
    }

    if (showAddAlarmSheet) {
        AlarmEditorSheet(
            repository = repository,
            context = context,
            existingAlarm = null,
            initialGroupId = addAlarmGroupId,
            onDismiss = { showAddAlarmSheet = false }
        )
    }

    editingAlarm?.let { alarm ->
        AlarmEditorSheet(
            repository = repository,
            context = context,
            existingAlarm = alarm,
            onDismiss = { editingAlarm = null }
        )
    }

    if (showNewGroupDialog) {
        GroupNameDialog(
            title = "New group",
            initialName = "",
            confirmLabel = "Create",
            onConfirm = {
                repository.createGroup(it)
                showNewGroupDialog = false
            },
            onDismiss = { showNewGroupDialog = false }
        )
    }

    renamingGroup?.let { group ->
        GroupNameDialog(
            title = "Rename group",
            initialName = group.name,
            confirmLabel = "Save",
            onConfirm = {
                repository.saveGroup(group.copy(name = it))
                renamingGroup = null
            },
            onDismiss = { renamingGroup = null }
        )
    }

    deletingGroup?.let { group ->
        DeleteGroupDialog(
            group = group,
            alarmCount = alarms.count { it.groupId == group.id },
            onDeleteAll = {
                repository.alarmsInGroup(group.id).forEach {
                    AlarmScheduler.cancelAlarm(context, it.id)
                }
                repository.deleteGroup(group.id, deleteAlarms = true)
                deletingGroup = null
            },
            onKeepAlarms = {
                repository.deleteGroup(group.id, deleteAlarms = false)
                deletingGroup = null
            },
            onDismiss = { deletingGroup = null }
        )
    }
}

// ---------------------------------------------------------------------------
// Groups
// ---------------------------------------------------------------------------

@Composable
private fun GroupCard(
    group: AlarmGroup,
    groupAlarms: List<AlarmItem>,
    repository: AlarmRepository,
    context: Context,
    onEditAlarm: (AlarmItem) -> Unit,
    onAddAlarm: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val count = groupAlarms.size
    val subtitle = when {
        count == 0 -> "No alarms"
        !group.enabled -> "$count ${if (count == 1) "alarm" else "alarms"} \u00B7 group off"
        else -> "$count ${if (count == 1) "alarm" else "alarms"} \u00B7 ${groupAlarms.count { it.enabled }} on"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (group.enabled) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { repository.saveGroup(group.copy(expanded = !group.expanded)) }
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (group.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (group.expanded) "Collapse group" else "Expand group"
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = group.enabled,
                    onCheckedChange = { repository.setGroupEnabled(group.id, it) }
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Group options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Add alarm to group") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onAddAlarm()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename group") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete group", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            if (group.expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (groupAlarms.isEmpty()) {
                        Text(
                            text = "No alarms in this group yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        TextButton(onClick = onAddAlarm) { Text("Add alarm") }
                    } else {
                        groupAlarms.forEach { alarm ->
                            androidx.compose.runtime.key(alarm.id) {
                                AlarmCard(
                                    alarm = alarm,
                                    repository = repository,
                                    context = context,
                                    onEdit = { onEditAlarm(alarm) },
                                    groupEnabled = group.enabled
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeleteGroupDialog(
    group: AlarmGroup,
    alarmCount: Int,
    onDeleteAll: () -> Unit,
    onKeepAlarms: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete \"${group.name}\"?") },
        text = {
            Column {
                if (alarmCount == 0) {
                    Text("This group is empty.")
                } else {
                    Text(
                        "This group has $alarmCount ${if (alarmCount == 1) "alarm" else "alarms"}. " +
                            "You can delete them along with the group, or keep them as ungrouped alarms."
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onKeepAlarms) { Text("Keep alarms, delete group only") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDeleteAll,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (alarmCount == 0) "Delete" else "Delete group & alarms")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .alpha(0.6f),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No alarms yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap ?Add alarm? to set your first one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Alarm list item
// ---------------------------------------------------------------------------

@Composable
fun AlarmCard(
    alarm: AlarmItem,
    repository: AlarmRepository,
    context: Context,
    onEdit: () -> Unit,
    groupEnabled: Boolean = true
) {
    // An alarm only rings if both it and its group are on.
    val active = alarm.enabled && groupEnabled
    val containerColor =
        if (active) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor =
        if (active) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (groupEnabled) 1f else 0.6f),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatClock(alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    if (alarm.label.isNotEmpty()) {
                        Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = { repository.saveAlarm(alarm.copy(enabled = it)) }
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text = getRepeatText(alarm) + if (!groupEnabled) " \u00B7 group is off" else "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit alarm")
                }
                IconButton(
                    onClick = {
                        AlarmScheduler.cancelAlarm(context, alarm.id)
                        repository.deleteAlarm(alarm.id)
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete alarm",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Editor (bottom sheet ? scrolls, respects insets, fits any phone height)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorSheet(
    repository: AlarmRepository,
    context: Context,
    existingAlarm: AlarmItem?,
    onDismiss: () -> Unit,
    initialGroupId: Long? = null
) {
    val groups by repository.groups.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timePickerState = rememberTimePickerState(
        initialHour = existingAlarm?.hour ?: 8,
        initialMinute = existingAlarm?.minute ?: 0,
        is24Hour = false
    )
    var label by remember(existingAlarm?.id) { mutableStateOf(existingAlarm?.label ?: "") }
    var groupId by remember(existingAlarm?.id) {
        mutableStateOf(existingAlarm?.groupId ?: initialGroupId)
    }
    var repeatMode by remember(existingAlarm?.id) {
        mutableStateOf(existingAlarm?.repeatMode ?: AlarmRepeatMode.DAILY)
    }
    var selectedDays by remember(existingAlarm?.id) {
        mutableStateOf(existingAlarm?.selectedDays ?: DayOfWeek.values().toSet())
    }
    var startDate by remember(existingAlarm?.id) {
        mutableStateOf(existingAlarm?.everyOtherDayStartDate ?: todayAtStartOfDay())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (existingAlarm == null) "Add alarm" else "Edit alarm",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(16.dp))

            // TimePicker has an intrinsic size that fits within ~360dp-wide phones.
            TimePicker(state = timePickerState)

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (groups.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionTitle("Group")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = groupId == null,
                        onClick = { groupId = null },
                        label = { Text("None") }
                    )
                    groups.forEach { group ->
                        FilterChip(
                            selected = groupId == group.id,
                            onClick = { groupId = group.id },
                            label = {
                                Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Repeat")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepeatChip("Daily", repeatMode == AlarmRepeatMode.DAILY) {
                    repeatMode = AlarmRepeatMode.DAILY
                }
                RepeatChip("Days", repeatMode == AlarmRepeatMode.SPECIFIC_DAYS) {
                    repeatMode = AlarmRepeatMode.SPECIFIC_DAYS
                }
                RepeatChip("Every other", repeatMode == AlarmRepeatMode.EVERY_OTHER_DAY) {
                    repeatMode = AlarmRepeatMode.EVERY_OTHER_DAY
                }
            }

            if (repeatMode != AlarmRepeatMode.DAILY) {
                Spacer(Modifier.height(16.dp))
                SectionTitle("Days")
                // All seven days on one row, each taking an equal share of the width.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays =
                                    if (day in selectedDays) selectedDays - day else selectedDays + day
                            },
                            label = {
                                Text(
                                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (repeatMode == AlarmRepeatMode.EVERY_OTHER_DAY) {
                Spacer(Modifier.height(16.dp))
                StartDatePicker(
                    selectedDate = startDate,
                    onDateSelected = { startDate = it }
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val base = existingAlarm
                            ?: AlarmItem(hour = timePickerState.hour, minute = timePickerState.minute)
                        val alarmToSave = base.copy(
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                            repeatMode = repeatMode,
                            selectedDays = selectedDays,
                            everyOtherDayStartDate = startDate,
                            label = label,
                            groupId = groupId
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
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Save")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.RepeatChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        modifier = Modifier.weight(1f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDatePicker(selectedDate: Long, onDateSelected: (Long) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val dateText = formatDate(selectedDate)

    FilledTonalButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text("Start date: $dateText")
    }

    if (showDialog) {
        // DatePicker works in UTC millis; convert to/from local midnight.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = localToUtcMidnight(selectedDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateSelected(utcToLocalMidnight(it)) }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun localToUtcMidnight(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcToLocalMidnight(utcMillis: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}

private fun todayAtStartOfDay(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun getCurrentTime(): String =
    SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Calendar.getInstance().time)

private fun getCurrentDate(): String =
    SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Calendar.getInstance().time)

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
            alarm.selectedDays.sortedBy { it.value }.mapNotNull(labels::get).joinToString(", ")
        AlarmRepeatMode.EVERY_OTHER_DAY ->
            "Every other: " + alarm.selectedDays.sortedBy { it.value }.mapNotNull(labels::get).joinToString(", ") +
                    " from " + formatDate(alarm.everyOtherDayStartDate)
    }
}
