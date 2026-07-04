package io.github.matheusghenriques.thedoor.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.matheusghenriques.thedoor.R
import io.github.matheusghenriques.thedoor.data.AppConfig
import io.github.matheusghenriques.thedoor.data.AppInfo
import io.github.matheusghenriques.thedoor.data.TimeSchedule
import io.github.matheusghenriques.thedoor.ui.viewmodel.AppLimitsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

private val dayLabels = listOf(
    R.string.day_sun to Calendar.SUNDAY,
    R.string.day_mon to Calendar.MONDAY,
    R.string.day_tue to Calendar.TUESDAY,
    R.string.day_wed to Calendar.WEDNESDAY,
    R.string.day_thu to Calendar.THURSDAY,
    R.string.day_fri to Calendar.FRIDAY,
    R.string.day_sat to Calendar.SATURDAY
)

@Composable
fun AppLimitsScreen(onBack: () -> Unit, onComplete: () -> Unit) {
    val vm: AppLimitsViewModel = viewModel()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dialogApp = remember { mutableStateOf<AppInfo?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.app_limits_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.app_limits_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        if (!vm.usagePermissionGranted) {
            UsagePermissionBanner(vm)
        }

        OutlinedTextField(
            value = vm.searchQuery,
            onValueChange = { vm.searchQuery = it },
            placeholder = { Text(stringResource(R.string.app_limits_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(vm.filteredApps, key = { it.packageName }) { app ->
                FadeInItem(animate = !vm.loadingComplete) {
                    AppListTile(
                        app = app,
                        config = vm.getConfig(app.packageName),
                        modifier = Modifier.padding(bottom = 8.dp),
                        onClick = { dialogApp.value = app })
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedButton(
                onClick = onBack, modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.btn_voltar))
            }
            Spacer(Modifier.width(12.dp))
            TimedButton(
                text = stringResource(R.string.btn_proximo),
                onClick = onComplete,
                modifier = Modifier.weight(1f)
            )
        }
    }

    dialogApp.value?.let { app ->
        val weeklyAverage = remember(app) {
            vm.getWeeklyAverageMinutes(app.packageName)
        }
        AppConfigDialog(
            app = app,
            initialConfig = vm.getConfig(app.packageName),
            weeklyAverage = weeklyAverage,
            onSave = { blocked, schedules, perDayLimits, perDayTargets, maxOpensPerDay ->
                vm.saveAll(
                    app.packageName, blocked, schedules, perDayLimits, perDayTargets, maxOpensPerDay
                )
                dialogApp.value = null
            },
            onDismiss = { dialogApp.value = null })
    }
}

@Composable
private fun FadeInItem(animate: Boolean, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (animate) alpha.animateTo(1f, animationSpec = tween(300))
    }
    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha.value }) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppListTile(
    app: AppInfo, config: AppConfig, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(drawable = app.icon, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (config.blocked || config.schedules.isNotEmpty() || config.perDayLimits.any { it.value > 0 } || config.maxOpensPerDay != null) {
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (config.blocked) {
                            AppBadge(
                                text = stringResource(R.string.app_limits_status_blocked),
                                backgroundColor = MaterialTheme.colorScheme.error,
                                textColor = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            if (config.schedules.isNotEmpty()) {
                                AppBadge(
                                    text = stringResource(R.string.app_config_status_scheduled),
                                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    textColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            if (config.maxOpensPerDay != null) {
                                AppBadge(
                                    text = stringResource(
                                        R.string.app_config_status_opens, config.maxOpensPerDay
                                    ),
                                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                    textColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            val activeLimit = config.perDayLimits[Calendar.getInstance()
                                .get(Calendar.DAY_OF_WEEK)]
                                ?: config.perDayLimits.values.maxOrNull()
                            val todayTarget = config.perDayTargets[Calendar.getInstance()
                                .get(Calendar.DAY_OF_WEEK)]
                                ?: config.perDayTargets.values.maxOrNull()
                            if (activeLimit != null && activeLimit > 0) {
                                val label =
                                    if (todayTarget != null && todayTarget != activeLimit) "$activeLimit \u2192 $todayTarget min"
                                    else "$activeLimit min"
                                AppBadge(
                                    text = label,
                                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                    textColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else if (todayTarget != null && todayTarget > 0) {
                                AppBadge(
                                    text = "\u2192 $todayTarget min",
                                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    textColor = MaterialTheme.colorScheme.onTertiaryContainer
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
private fun AppBadge(
    text: String, backgroundColor: Color, textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp), color = backgroundColor.copy(alpha = 0.8f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private data class LimitEntry(val days: List<Int>, val minutes: Int, val startMinutes: Int = 0)

private fun toEntries(
    perDayLimits: Map<Int, Int>, perDayTargets: Map<Int, Int> = emptyMap()
): List<LimitEntry> {
    if (perDayTargets.isNotEmpty()) {
        val grouped = mutableMapOf<Int, MutableList<Int>>()
        perDayTargets.forEach { (day, target) ->
            grouped.getOrPut(target) { mutableListOf() }.add(day)
        }
        return grouped.map { (target, days) ->
            LimitEntry(days, target, startMinutes = perDayLimits[days.first()] ?: target)
        }
    }
    val grouped = mutableMapOf<Int, MutableList<Int>>()
    perDayLimits.forEach { (day, limit) ->
        grouped.getOrPut(limit) { mutableListOf() }.add(day)
    }
    return grouped.map { (limit, days) -> LimitEntry(days, limit) }
}

private fun entriesToMap(entries: List<LimitEntry>): Map<Int, Int> {
    return entries.flatMap { e ->
        e.days.map { it to (if (e.minutes > 0) e.minutes else e.startMinutes) }
    }.toMap()
}

private fun entriesToStartMap(entries: List<LimitEntry>): Map<Int, Int> {
    return entries.flatMap { e ->
        e.days.map { it to (if (e.startMinutes > 0) e.startMinutes else e.minutes) }
    }.toMap()
}

private fun weeksToTarget(start: Int, target: Int): Int {
    if (start <= target) return 0
    var weeks = 0
    var current = start
    while (current > target) {
        val raw = (current * 0.10).toInt()
        val minR = minOf(5, current - target)
        val reduction = raw.coerceIn(minR, 30)
        current = maxOf(target, current - reduction)
        weeks++
    }
    return weeks
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppConfigDialog(
    app: AppInfo,
    initialConfig: AppConfig,
    weeklyAverage: Int?,
    onSave: (blocked: Boolean, schedules: List<TimeSchedule>, perDayLimits: Map<Int, Int>, perDayTargets: Map<Int, Int>, maxOpensPerDay: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var blocked by remember { mutableStateOf(initialConfig.blocked) }
    var schedules by remember { mutableStateOf(initialConfig.schedules) }
    var limitEntries by remember {
        mutableStateOf(toEntries(initialConfig.perDayLimits, initialConfig.perDayTargets).map {
            if (weeklyAverage != null && weeklyAverage > 0 && it.startMinutes <= 0) it.copy(
                startMinutes = weeklyAverage
            )
            else it
        })
    }
    val entriesMap by remember(limitEntries) { derivedStateOf { entriesToMap(limitEntries) } }
    var maxOpensPerDay by remember { mutableStateOf(initialConfig.maxOpensPerDay) }
    var currentTab by remember { mutableIntStateOf(0) }

    val minusInteractionSource = remember { MutableInteractionSource() }
    val plusInteractionSource = remember { MutableInteractionSource() }
    var minusJob by remember { mutableStateOf<Job?>(null) }
    var plusJob by remember { mutableStateOf<Job?>(null) }
    val minusRepeating = remember { mutableStateOf(false) }
    val plusRepeating = remember { mutableStateOf(false) }

    LaunchedEffect(minusInteractionSource) {
        minusInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    plusJob?.cancel()
                    minusRepeating.value = false
                    minusJob?.cancel()
                    minusJob = launch {
                        delay(300)
                        minusRepeating.value = true
                        while (isActive) {
                            val cur = maxOpensPerDay
                            if (cur != null) {
                                val next = cur - 1
                                maxOpensPerDay = if (next >= 1) next else null
                            }
                            delay(50)
                        }
                    }
                }

                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    minusJob?.cancel()
                    minusJob = null
                    if (!minusRepeating.value) {
                        val cur = maxOpensPerDay
                        maxOpensPerDay = if (cur != null && cur > 1) cur - 1 else null
                    }
                }
            }
        }
    }

    LaunchedEffect(plusInteractionSource) {
        plusInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    minusJob?.cancel()
                    plusRepeating.value = false
                    plusJob?.cancel()
                    plusJob = launch {
                        delay(300)
                        plusRepeating.value = true
                        while (isActive) {
                            maxOpensPerDay = (maxOpensPerDay ?: 0) + 1
                            delay(50)
                        }
                    }
                }

                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    plusJob?.cancel()
                    plusJob = null
                    if (!plusRepeating.value) {
                        maxOpensPerDay = (maxOpensPerDay ?: 0) + 1
                    }
                }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(drawable = app.icon, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            PrimaryTabRow(
                selectedTabIndex = currentTab, containerColor = Color.Transparent
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text(stringResource(R.string.app_config_tab_block)) },
                    enabled = true
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text(stringResource(R.string.app_config_tab_schedule)) },
                    enabled = !blocked
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    text = { Text(stringResource(R.string.app_config_tab_limit)) },
                    enabled = !blocked
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                when (currentTab) {
                    0 -> {
                        Text(
                            text = stringResource(R.string.app_config_block_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        if (blocked) {
                            Button(
                                onClick = { blocked = false }, modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.app_config_block_label))
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    blocked = true
                                    currentTab = 0
                                    maxOpensPerDay = null
                                }, modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.app_config_block_label))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_config_opens_label) + ":",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {},
                                interactionSource = minusInteractionSource,
                                enabled = !blocked
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = maxOpensPerDay?.toString() ?: "∞",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                                ),
                                color = if (!blocked) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                modifier = Modifier.width(48.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {},
                                interactionSource = plusInteractionSource,
                                enabled = !blocked
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = stringResource(R.string.app_config_schedule_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.app_config_schedule_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))

                        schedules.forEachIndexed { index, schedule ->
                            ScheduleWindowRow(schedule = schedule, onUpdate = { updated ->
                                schedules = schedules.toMutableList().also { it[index] = updated }
                            }, onRemove = {
                                schedules = schedules.toMutableList().also { it.removeAt(index) }
                            })
                            if (index < schedules.lastIndex) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                schedules = schedules + TimeSchedule(emptyList(), 8, 0, 18, 0)
                            }, modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.app_config_schedule_add))
                        }
                    }

                    2 -> {
                        Text(
                            text = stringResource(R.string.app_config_limit_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))

                        if (weeklyAverage != null && weeklyAverage > 0) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = stringResource(
                                            R.string.app_config_limit_weekly_avg, weeklyAverage
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.app_config_limit_start_prefill_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Text(
                            text = stringResource(R.string.app_config_limit_per_day),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))

                        val usedDays = remember(limitEntries) {
                            limitEntries.flatMap { it.days }.toSet()
                        }

                        limitEntries.forEachIndexed { index, entry ->
                            LimitEntryRow(
                                entry = entry,
                                usedDays = usedDays,
                                onUpdate = { updated ->
                                    limitEntries =
                                        limitEntries.toMutableList().also { it[index] = updated }
                                },
                                onRemove = {
                                    limitEntries =
                                        limitEntries.toMutableList().also { it.removeAt(index) }
                                })
                            if (entry.startMinutes > entry.minutes) {
                                val weeks = weeksToTarget(entry.startMinutes, entry.minutes)
                                if (weeks > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.app_config_limit_weeks_to_target, weeks
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            if (index < limitEntries.lastIndex) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        if (usedDays.size < 7) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    limitEntries = limitEntries + LimitEntry(
                                        emptyList(), 0, weeklyAverage ?: 0
                                    )
                                }, modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.app_config_limit_add))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss, modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_cancelar))
                }
                Spacer(Modifier.width(12.dp))
                val canSave by remember(limitEntries, schedules) {
                    derivedStateOf {
                        val invalidLimit = limitEntries.any { it.days.isEmpty() || it.minutes < 0 }
                        val invalidSchedule = schedules.any { it.daysOfWeek.isEmpty() }
                        !(invalidLimit || invalidSchedule)
                    }
                }
                if (canSave) {
                    Button(
                        onClick = {
                            onSave(
                                blocked,
                                schedules,
                                entriesToStartMap(limitEntries),
                                entriesMap,
                                maxOpensPerDay
                            )
                        }, modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_salvar))
                    }
                } else {
                    OutlinedButton(
                        onClick = {}, modifier = Modifier.weight(1f), enabled = false
                    ) {
                        Text(stringResource(R.string.btn_salvar))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleWindowRow(
    schedule: TimeSchedule, onUpdate: (TimeSchedule) -> Unit, onRemove: () -> Unit
) {
    val showStartPicker = remember { mutableStateOf(false) }
    val showEndPicker = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()
        ) {
            dayLabels.forEach { (labelRes, dayValue) ->
                val selected = schedule.daysOfWeek.contains(dayValue)
                FilterChip(
                    selected = selected, onClick = {
                    val newDays = if (selected) schedule.daysOfWeek - dayValue
                    else schedule.daysOfWeek + dayValue
                    onUpdate(schedule.copy(daysOfWeek = newDays))
                }, modifier = Modifier.height(28.dp), label = {
                    Text(
                        stringResource(labelRes).take(1),
                        style = MaterialTheme.typography.bodySmall
                    )
                }, colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_config_schedule_start),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showStartPicker.value = true }) {
                    Text(
                        text = "%02d:%02d".format(schedule.startHour, schedule.startMinute),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = stringResource(R.string.app_config_schedule_end),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showEndPicker.value = true }) {
                    Text(
                        text = "%02d:%02d".format(schedule.endHour, schedule.endMinute),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.app_config_schedule_remove),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showStartPicker.value) {
        M3TimePickerDialog(
            initialHour = schedule.startHour,
            initialMinute = schedule.startMinute,
            onConfirm = { h, m ->
                onUpdate(
                    schedule.copy(
                        startHour = h, startMinute = m
                    )
                ); showStartPicker.value = false
            },
            onDismiss = { showStartPicker.value = false })
    }
    if (showEndPicker.value) {
        M3TimePickerDialog(
            initialHour = schedule.endHour,
            initialMinute = schedule.endMinute,
            onConfirm = { h, m ->
                onUpdate(
                    schedule.copy(
                        endHour = h, endMinute = m
                    )
                ); showEndPicker.value = false
            },
            onDismiss = { showEndPicker.value = false })
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LimitEntryRow(
    entry: LimitEntry, usedDays: Set<Int>, onUpdate: (LimitEntry) -> Unit, onRemove: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()
        ) {
            dayLabels.forEach { (labelRes, dayValue) ->
                val selected = dayValue in entry.days
                val disabled = !selected && dayValue in usedDays
                FilterChip(
                    selected = selected, onClick = {
                    val newDays = if (selected) entry.days - dayValue
                    else entry.days + dayValue
                    onUpdate(entry.copy(days = newDays))
                }, modifier = Modifier.height(28.dp), label = {
                    Text(
                        stringResource(labelRes).take(1),
                        style = MaterialTheme.typography.bodySmall
                    )
                }, enabled = !disabled, colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = if (entry.startMinutes > 0) entry.startMinutes.toString() else "",
                onValueChange = {
                    val n = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                    onUpdate(entry.copy(startMinutes = n))
                },
                modifier = Modifier.width(90.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
                label = { Text(stringResource(R.string.app_config_limit_entry_start)) })
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = if (entry.minutes > 0) entry.minutes.toString() else "",
                onValueChange = {
                    val n = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                    onUpdate(entry.copy(minutes = n))
                },
                modifier = Modifier.width(90.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
                label = {
                    Text(stringResource(R.string.app_config_limit_entry_target))
                })
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.app_config_schedule_remove),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = remember { TimePickerState(initialHour, initialMinute, is24Hour = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_config_time_picker_title)) },
        text = {
            TimePicker(
                state = state, colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.onSurface,
                    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    selectorColor = MaterialTheme.colorScheme.primary,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancelar))
            }
        })
}

@Composable
private fun UsagePermissionBanner(vm: AppLimitsViewModel) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.app_limits_banner_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.app_limits_banner_desc),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    context.startActivity(vm.getUsageAccessIntent())
                }) {
                Text(stringResource(R.string.app_limits_banner_btn))
            }
        }
    }
}
