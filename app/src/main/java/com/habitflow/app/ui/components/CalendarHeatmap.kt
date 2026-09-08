package com.habitflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.domain.StreakCalculator
import com.habitflow.app.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

enum class DayStatus { COMPLETED, MISSED, PARTIAL, FUTURE, NOT_SCHEDULED }

/** A single month calendar grid. Each cell is colored by [DayStatus]. */
@Composable
fun MonthCalendarGrid(
    month: YearMonth,
    completedDays: Set<Long>,
    partialDays: Set<Long> = emptySet(),
    habit: Habit? = null,
    onDayClick: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val firstOfMonth = month.atDay(1)
    val leadingBlanks = (firstOfMonth.dayOfWeek.value - 1) // Monday = 0 blanks
    val daysInMonth = month.lengthOfMonth()

    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val totalCells = leadingBlanks + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - leadingBlanks + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val date = month.atDay(dayNum)
                            val status = dayStatus(date, today, habit, completedDays, partialDays)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorForStatus(status))
                                    .clickable { onDayClick(date) },
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text(
                                    dayNum.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColorForStatus(status)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun dayStatus(
    date: LocalDate,
    today: LocalDate,
    habit: Habit?,
    completedDays: Set<Long>,
    partialDays: Set<Long>
): DayStatus {
    if (date.isAfter(today)) return DayStatus.FUTURE
    if (habit != null && !StreakCalculator.isScheduledOn(habit, date)) return DayStatus.NOT_SCHEDULED
    val epoch = date.toEpochDay()
    return when {
        completedDays.contains(epoch) -> DayStatus.COMPLETED
        partialDays.contains(epoch) -> DayStatus.PARTIAL
        else -> DayStatus.MISSED
    }
}

@Composable
private fun colorForStatus(status: DayStatus) = when (status) {
    DayStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    DayStatus.PARTIAL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    DayStatus.MISSED -> MaterialTheme.colorScheme.errorContainer
    DayStatus.FUTURE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    DayStatus.NOT_SCHEDULED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
}

@Composable
private fun textColorForStatus(status: DayStatus) = when (status) {
    DayStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimary
    DayStatus.MISSED -> MaterialTheme.colorScheme.onErrorContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
