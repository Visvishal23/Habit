package com.habitflow.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.data.local.entity.HomeViewMode
import com.habitflow.app.ui.components.EmptyState
import com.habitflow.app.ui.components.HabitProgressBar
import com.habitflow.app.util.DateUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onHabitClick: (Long) -> Unit,
    onCreateHabit: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateHabit) {
                Icon(Icons.Filled.Add, contentDescription = "Create habit")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp, top = 12.dp)
        ) {
            item { HomeHeader(state.doneToday, state.scheduledToday) }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                ViewModeToggle(
                    current = state.viewMode,
                    onChange = viewModel::setViewMode
                )
                Spacer(Modifier.height(12.dp))
            }

            if (state.habitCards.isEmpty() && !state.isLoading) {
                item {
                    EmptyState(
                        emoji = "🌱",
                        title = "Start building your routine.",
                        subtitle = "Create your first habit and begin tracking your progress.",
                        actionLabel = "Create Habit",
                        onAction = onCreateHabit
                    )
                }
            } else {
                items(state.habitCards, key = { it.habit.id }) { card ->
                    when (state.viewMode) {
                        HomeViewMode.LIST -> HabitListCard(
                            card = card,
                            onToggle = { viewModel.toggleHabit(card.habit) },
                            onClick = { onHabitClick(card.habit.id) }
                        )
                        HomeViewMode.STREAK -> HabitStreakCard(
                            card = card,
                            onToggle = { viewModel.toggleHabit(card.habit) },
                            onClick = { onHabitClick(card.habit.id) }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(done: Int, scheduled: Int) {
    val progress = if (scheduled > 0) done.toFloat() / scheduled else 0f
    Column {
        Text(
            "${DateUtils.greeting()} 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            DateUtils.formatHeaderDate(LocalDate.now()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Today", style = MaterialTheme.typography.titleMedium)
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                HabitProgressBar(progress = progress, height = 10.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "$done of $scheduled habits completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ViewModeToggle(current: HomeViewMode, onChange: (HomeViewMode) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        listOf(HomeViewMode.LIST to "List", HomeViewMode.STREAK to "Streak").forEach { (mode, label) ->
            val selected = current == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onChange(mode) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun HabitListCard(card: HabitCardUiState, onToggle: () -> Unit, onClick: () -> Unit) {
    val habit = card.habit
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(habit.colorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(habit.icon, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${card.stats.currentStreak} day streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                HabitProgressBar(progress = card.stats.completionRate, height = 5.dp)
            }
            Spacer(Modifier.width(10.dp))
            CompleteButton(isCompleted = card.isCompletedToday, accentColor = accentColor, onToggle = onToggle)
        }
    }
}

@Composable
private fun HabitStreakCard(card: HabitCardUiState, onToggle: () -> Unit, onClick: () -> Unit) {
    val habit = card.habit
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(habit.colorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(habit.icon, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "Best ${card.stats.bestStreak} · ${(card.stats.completionRate * 100).toInt()}% overall",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CompleteButton(isCompleted = card.isCompletedToday, accentColor = accentColor, onToggle = onToggle)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StreakMetric("🔥", "${card.stats.currentStreak}", "Current")
                StreakMetric("🏆", "${card.stats.bestStreak}", "Best")
                StreakMetric("📅", "${(card.stats.weeklyConsistency * 100).toInt()}%", "This week")
            }
        }
    }
}

@Composable
private fun StreakMetric(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$emoji $value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompleteButton(isCompleted: Boolean, accentColor: Color, onToggle: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isCompleted) accentColor else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isCompleted) {
            Icon(Icons.Filled.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}
