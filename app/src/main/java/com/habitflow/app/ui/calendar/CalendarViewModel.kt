package com.habitflow.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.data.local.entity.HabitCompletion
import com.habitflow.app.data.repository.HabitRepository
import com.habitflow.app.domain.StreakCalculator
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth

data class DayHabitStatus(val habit: Habit, val isComplete: Boolean, val isScheduled: Boolean)

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val completedDaysAll: Set<Long> = emptySet(), // days where every scheduled habit was completed
    val partialDaysAll: Set<Long> = emptySet(),    // days where some but not all scheduled habits were completed
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDayHabits: List<DayHabitStatus> = emptyList()
)

class CalendarViewModel(private val habitRepository: HabitRepository) : ViewModel() {

    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        habitRepository.observeActiveHabits(),
        habitRepository.observeAllCompletions(),
        visibleMonth,
        selectedDate
    ) { habits, completions, month, selected ->
        val byDay: Map<Long, List<HabitCompletion>> = completions.groupBy { it.dateEpochDay }
        val completedAll = mutableSetOf<Long>()
        val partialAll = mutableSetOf<Long>()

        var d = month.atDay(1)
        val end = month.atEndOfMonth()
        while (!d.isAfter(end)) {
            val scheduledHabits = habits.filter { StreakCalculator.isScheduledOn(it, d) }
            if (scheduledHabits.isNotEmpty()) {
                val dayCompletions = byDay[d.toEpochDay()].orEmpty().filter { it.isComplete }.map { it.habitId }.toSet()
                val completedCount = scheduledHabits.count { dayCompletions.contains(it.id) }
                when {
                    completedCount == scheduledHabits.size -> completedAll.add(d.toEpochDay())
                    completedCount > 0 -> partialAll.add(d.toEpochDay())
                }
            }
            d = d.plusDays(1)
        }

        val selectedDayCompletions = byDay[selected.toEpochDay()].orEmpty().associateBy { it.habitId }
        val selectedDayHabits = habits.map { habit ->
            DayHabitStatus(
                habit = habit,
                isComplete = selectedDayCompletions[habit.id]?.isComplete == true,
                isScheduled = StreakCalculator.isScheduledOn(habit, selected)
            )
        }.filter { it.isScheduled || it.isComplete }

        CalendarUiState(
            visibleMonth = month,
            completedDaysAll = completedAll,
            partialDaysAll = partialAll,
            selectedDate = selected,
            selectedDayHabits = selectedDayHabits
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun selectDate(date: LocalDate) { selectedDate.value = date }
    fun nextMonth() { visibleMonth.value = visibleMonth.value.plusMonths(1) }
    fun previousMonth() { visibleMonth.value = visibleMonth.value.minusMonths(1) }
}
