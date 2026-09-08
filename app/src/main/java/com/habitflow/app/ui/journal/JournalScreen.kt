@file:OptIn(ExperimentalMaterial3Api::class)

package com.habitflow.app.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitflow.app.data.local.entity.Mood
import com.habitflow.app.ui.components.EmptyState
import com.habitflow.app.util.DateUtils
import java.time.LocalDate

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onAddEntry: (habitId: Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showHabitPicker by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (state.habits.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = { showHabitPicker = true }, text = { Text("Add Entry") }, icon = {})
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.entries.isEmpty() && !state.isLoading) {
                EmptyState(
                    emoji = "📔",
                    title = "Your journey starts here.",
                    subtitle = "Capture notes and photos alongside your habits.",
                    actionLabel = if (state.habits.isNotEmpty()) "Add Entry" else null,
                    onAction = if (state.habits.isNotEmpty()) { { showHabitPicker = true } } else null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
                ) {
                    item {
                        Text("Journal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(16.dp))
                    }
                    items(state.entries, key = { it.entry.id }) { item ->
                        JournalEntryCard(item, onDelete = { viewModel.deleteEntry(item.entry) })
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            if (showHabitPicker) {
                ModalBottomSheet(onDismissRequest = { showHabitPicker = false }) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Add entry for which habit?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        state.habits.forEach { habit ->
                            ListItem(
                                headlineContent = { Text(habit.name) },
                                leadingContent = { Text(habit.icon) },
                                modifier = Modifier.clickable {
                                    showHabitPicker = false
                                    onAddEntry(habit.id)
                                }
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEntryCard(item: JournalEntryWithHabit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        DateUtils.formatHeaderDate(LocalDate.ofEpochDay(item.entry.dateEpochDay)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.habit != null) {
                        Text("${item.habit.icon} ${item.habit.name}", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(moodEmoji(item.entry.mood))
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete entry") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.entry.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun moodEmoji(mood: Mood) = when (mood) {
    Mood.GREAT -> "😄"
    Mood.GOOD -> "🙂"
    Mood.OKAY -> "😐"
    Mood.LOW -> "😕"
    Mood.ROUGH -> "😞"
}
