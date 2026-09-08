package com.habitflow.app.ui.habitdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.habitflow.app.ui.components.HabitProgressBar
import com.habitflow.app.ui.components.MonthCalendarGrid
import com.habitflow.app.util.DateUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: Long,
    viewModel: HabitDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenJournal: (Long) -> Unit
) {
    LaunchedEffect(habitId) { viewModel.load(habitId) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showBackfillDialog by remember { mutableStateOf(false) }
    var backfillDaysText by remember { mutableStateOf("7") }

    val habit = state.habit

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { habit?.let { onEdit(it.id) } }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Log past days") }, onClick = {
                            showMenu = false
                            showBackfillDialog = true
                        })
                        DropdownMenuItem(text = { Text("Archive") }, onClick = {
                            showMenu = false
                            viewModel.archiveHabit(onBack)
                        })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        })
                    }
                }
            )
        }
    ) { padding ->
        if (habit == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.isLoading) CircularProgressIndicator()
            }
            return@Scaffold
        }

        val accentColor = runCatching { Color(android.graphics.Color.parseColor(habit.colorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
        val isCompletedToday = state.completedDays.contains(LocalDate.now().toEpochDay())

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            if (habit.description.isNotBlank()) {
                Text(habit.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("🔥", "${state.stats.currentStreak}", "Current streak", Modifier.weight(1f))
                StatCard("🏆", "${state.stats.bestStreak}", "Best streak", Modifier.weight(1f))
                StatCard("✅", "${(state.stats.completionRate * 100).toInt()}%", "Completion", Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = viewModel::toggleToday,
                colors = ButtonDefaults.buttonColors(containerColor = if (isCompletedToday) accentColor else MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(if (isCompletedToday) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isCompletedToday) "Completed today" else "Mark Complete")
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("This month")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::goToPreviousMonth) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month") }
                Text(DateUtils.monthYear(state.visibleMonth.atDay(1)), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = viewModel::goToNextMonth) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next month") }
            }
            MonthCalendarGrid(
                month = state.visibleMonth,
                completedDays = state.completedDays,
                habit = habit,
                onDayClick = { date ->
                    if (!date.isAfter(LocalDate.now())) {
                        val isComplete = state.completedDays.contains(date.toEpochDay())
                        viewModel.setDayStatus(date, !isComplete)
                    }
                }
            )

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Journal")
                TextButton(onClick = { onOpenJournal(habit.id) }) { Text("View all") }
            }
            if (state.recentJournalEntries.isEmpty()) {
                Text(
                    "No entries yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.recentJournalEntries.forEach { entry ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                DateUtils.formatHeaderDate(LocalDate.ofEpochDay(entry.dateEpochDay)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(entry.text, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete this habit and its history?") },
                text = { Text("This will permanently remove \"${habit.name}\" and all of its completion history. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteHabit(onBack)
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }

        if (showBackfillDialog) {
            AlertDialog(
                onDismissRequest = { showBackfillDialog = false },
                title = { Text("Log past days") },
                text = {
                    Column {
                        Text(
                            "Mark the most recent days as complete for \"${habit.name}\" \u2014 handy if you're switching from another app and want to carry your streak over.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = backfillDaysText,
                            onValueChange = { text -> backfillDaysText = text.filter { it.isDigit() }.take(3) },
                            label = { Text("Number of days, including today") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "If this habit's start date is more recent than that, the start date will be moved back automatically so the streak counts correctly.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val days = backfillDaysText.toIntOrNull() ?: 0
                        viewModel.backfillLastNDays(days)
                        showBackfillDialog = false
                    }) { Text("Mark Complete") }
                },
                dismissButton = {
                    TextButton(onClick = { showBackfillDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}
