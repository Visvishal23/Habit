package com.habitflow.app.ui.createhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.habitflow.app.data.local.entity.FrequencyType
import com.habitflow.app.data.local.entity.GoalType
import com.habitflow.app.ui.components.DefaultCategories
import com.habitflow.app.ui.components.HabitIcons
import com.habitflow.app.ui.theme.HabitAccentColors
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateHabitScreen(
    habitId: Long,
    viewModel: CreateHabitViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(habitId) { viewModel.loadForEdit(habitId) }
    val state by viewModel.state.collectAsState()
    val isEditing = state.habitId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Habit" else "Create Habit") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        if (!state.isLoaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = { name -> viewModel.update { it.copy(name = name) } },
                label = { Text("Habit name") },
                isError = state.nameError != null,
                supportingText = { state.nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = { d -> viewModel.update { it.copy(description = d) } },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Start Date")
            Text(
                "Already been doing this habit elsewhere? Set an earlier start date, then use \"Log past days\" on the habit's detail screen to carry your streak over.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            var showDatePicker by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(state.startDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")))
            }
            if (showDatePicker) {
                val initialMillis = state.startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                                viewModel.update { it.copy(startDate = date) }
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Icon")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(HabitIcons.all) { emoji ->
                    val selected = emoji == state.icon
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.update { s -> s.copy(icon = emoji) } },
                        contentAlignment = Alignment.Center
                    ) { Text(emoji, style = MaterialTheme.typography.titleLarge) }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Color")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(HabitAccentColors) { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val selected = hex == state.colorHex
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selected) Modifier.background(color) else Modifier
                            )
                            .clickable { viewModel.update { s -> s.copy(colorHex = hex) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DefaultCategories.forEach { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { viewModel.update { it.copy(category = category) } },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Frequency")
            Column {
                FrequencyType.values().forEach { freq ->
                    FrequencyRow(
                        label = freqLabel(freq),
                        selected = state.frequencyType == freq,
                        onClick = { viewModel.update { it.copy(frequencyType = freq) } }
                    )
                }
            }

            AnimatedVisibilityFrequencyDetail(state, viewModel)

            Spacer(Modifier.height(20.dp))
            SectionLabel("Goal")
            Column {
                GoalRow("Complete once per day", state.goalType == GoalType.SIMPLE_CHECK) {
                    viewModel.update { it.copy(goalType = GoalType.SIMPLE_CHECK, goalTarget = 1) }
                }
                GoalRow("Numeric target (e.g. minutes, pages)", state.goalType == GoalType.NUMERIC_TARGET) {
                    viewModel.update { it.copy(goalType = GoalType.NUMERIC_TARGET) }
                }
            }
            if (state.goalType == GoalType.NUMERIC_TARGET) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = if (state.goalTarget == 0) "" else state.goalTarget.toString(),
                        onValueChange = { v -> viewModel.update { it.copy(goalTarget = v.toIntOrNull() ?: 0) } },
                        label = { Text("Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = state.goalUnit,
                        onValueChange = { v -> viewModel.update { it.copy(goalUnit = v) } },
                        label = { Text("Unit (minutes, pages...)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Reminder")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Remind me")
                Switch(
                    checked = state.reminderEnabled,
                    onCheckedChange = { checked -> viewModel.update { it.copy(reminderEnabled = checked) } }
                )
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.save { onDone() } },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (isEditing) "Save Changes" else "Create Habit")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AnimatedVisibilityFrequencyDetail(state: CreateHabitFormState, viewModel: CreateHabitViewModel) {
    when (state.frequencyType) {
        FrequencyType.SPECIFIC_WEEKDAYS -> {
            Spacer(Modifier.height(10.dp))
            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { index, label ->
                    val day = index + 1
                    val selected = state.scheduledWeekdays.contains(day)
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                viewModel.update {
                                    val newSet = it.scheduledWeekdays.toMutableSet()
                                    if (newSet.contains(day)) newSet.remove(day) else newSet.add(day)
                                    it.copy(scheduledWeekdays = newSet)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        FrequencyType.TIMES_PER_WEEK -> {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${state.timesPerWeek} times per week")
                Spacer(Modifier.width(12.dp))
                Slider(
                    value = state.timesPerWeek.toFloat(),
                    onValueChange = { v -> viewModel.update { it.copy(timesPerWeek = v.toInt()) } },
                    valueRange = 1f..7f,
                    steps = 5,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        else -> {}
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun FrequencyRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(label)
    }
}

@Composable
private fun GoalRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(label)
    }
}

private fun freqLabel(freq: FrequencyType) = when (freq) {
    FrequencyType.DAILY -> "Every day"
    FrequencyType.SPECIFIC_WEEKDAYS -> "Specific weekdays"
    FrequencyType.TIMES_PER_WEEK -> "X times per week"
    FrequencyType.CUSTOM -> "Custom schedule"
}
