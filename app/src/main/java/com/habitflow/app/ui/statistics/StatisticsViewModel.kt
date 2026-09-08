package com.habitflow.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.data.local.entity.HabitCompletion
import com.habitflow.app.data.repository.HabitRepository
import com.habitflow.app.domain.StreakCalculator
import kotlinx.coroutines.flow.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields

data class PeriodStats(
    val completionPercent: Int,
    val completedCount: Int,
    val missedCount: Int
)

data class TopHabitStat(val habit: Habit, val currentStreak: Int, val bestStreak: Int, val completionRate: Float)

data class StatisticsUiState(
    val today: PeriodStats = PeriodStats(0, 0, 0),
    val thisWeek: PeriodStats = PeriodStats(0, 0, 0),
    val thisMonth: PeriodStats = PeriodStats(0, 0, 0),
    val thisYear: PeriodStats = PeriodStats(0, 0, 0),
    val weeklyBarValues: List<Int> = List(7) { 0 }, // completions per weekday, Mon..Sun, last 7 days
    val habitConsistency: List<TopHabitStat> = emptyList(),
    val totalCompletionsAllTime: Int = 0
)

class StatisticsViewModel(private val habitRepository: HabitRepository) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        habitRepository.observeActiveHabits(),
        habitRepository.observeAllCompletions()
    ) { habits, completions ->
        val today = LocalDate.now()
        val byHabit = completions.groupBy { it.habitId }

        val todayStats = periodStats(habits, completions, today, today)
        val weekStart = today.with(WeekFields.ISO.dayOfWeek(), 1L)
        val weekStats = periodStats(habits, completions, weekStart, today)
        val monthStart = today.withDayOfMonth(1)
        val monthStats = periodStats(habits, completions, monthStart, today)
        val yearStart = today.withDayOfYear(1)
        val yearStats = periodStats(habits, completions, yearStart, today)

        val weeklyBars = MutableList(7) { 0 }
        val last7Start = today.minusDays(6)
        var d = last7Start
        while (!d.isAfter(today)) {
            val idx = d.dayOfWeek.value - 1
            weeklyBars[idx] = completions.count { it.dateEpochDay == d.toEpochDay() && it.isComplete }
            d = d.plusDays(1)
        }

        val consistency = habits.map { habit ->
            val stats = StreakCalculator.computeStats(habit, byHabit[habit.id].orEmpty(), today)
            TopHabitStat(habit, stats.currentStreak, stats.bestStreak, stats.completionRate)
        }.sortedByDescending { it.completionRate }

        StatisticsUiState(
            today = todayStats,
            thisWeek = weekStats,
            thisMonth = monthStats,
            thisYear = yearStats,
            weeklyBarValues = weeklyBars,
            habitConsistency = consistency,
            totalCompletionsAllTime = completions.count { it.isComplete }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsUiState())

    private fun periodStats(habits: List<Habit>, completions: List<HabitCompletion>, start: LocalDate, end: LocalDate): PeriodStats {
        var scheduled = 0
        var completed = 0
        val completedSet = completions.filter { it.isComplete }.map { it.habitId to it.dateEpochDay }.toSet()
        var d = start
        while (!d.isAfter(end)) {
            habits.forEach { habit ->
                if (StreakCalculator.isScheduledOn(habit, d)) {
                    scheduled++
                    if (completedSet.contains(habit.id to d.toEpochDay())) completed++
                }
            }
            d = d.plusDays(1)
        }
        val percent = if (scheduled > 0) (completed * 100 / scheduled) else 0
        return PeriodStats(percent, completed, scheduled - completed)
    }
}
