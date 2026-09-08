package com.habitflow.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class StatPeriod { TODAY, WEEK, MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var period by remember { mutableStateOf(StatPeriod.WEEK) }

    val periodStats = when (period) {
        StatPeriod.TODAY -> state.today
        StatPeriod.WEEK -> state.thisWeek
        StatPeriod.MONTH -> state.thisMonth
        StatPeriod.YEAR -> state.thisYear
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Text("Statistics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            ScrollableTabRow(selectedTabIndex = period.ordinal, edgePadding = 0.dp) {
                StatPeriod.values().forEach { p ->
                    Tab(
                        selected = period == p,
                        onClick = { period = p },
                        text = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("${periodStats.completionPercent}%", "Completion", Modifier.weight(1f))
                MetricCard("${periodStats.completedCount}", "Completed", Modifier.weight(1f))
                MetricCard("${periodStats.missedCount}", "Missed", Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(16.dp)) {
                WeeklyBarChart(
                    values = state.weeklyBarValues,
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Habit consistency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }

        items(state.habitConsistency) { stat ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row {
                            Text(stat.habit.icon)
                            Spacer(Modifier.width(8.dp))
                            Text(stat.habit.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("${(stat.completionRate * 100).toInt()}%", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    com.habitflow.app.ui.components.HabitProgressBar(progress = stat.completionRate, height = 6.dp)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyBarChart(values: List<Int>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val maxBarHeight = 80.dp

    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        values.forEachIndexed { index, value ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(value.toString(), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                val barHeight = maxBarHeight * (value.toFloat() / maxValue).coerceIn(0.04f, 1f)
                Box(
                    modifier = Modifier
                        .height(maxBarHeight)
                        .width(18.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.width(18.dp).height(barHeight)) {
                        drawRoundRect(
                            color = barColor,
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(labels[index], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
