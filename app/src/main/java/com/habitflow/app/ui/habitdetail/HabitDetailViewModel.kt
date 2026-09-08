package com.habitflow.app.ui.habitdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.data.local.entity.HabitCompletion
import com.habitflow.app.data.local.entity.JournalEntry
import com.habitflow.app.data.repository.HabitRepository
import com.habitflow.app.data.repository.JournalRepository
import com.habitflow.app.data.repository.ReminderRepository
import com.habitflow.app.domain.HabitStats
import com.habitflow.app.domain.StreakCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HabitDetailUiState(
    val habit: Habit? = null,
    val stats: HabitStats = HabitStats(0, 0, 0, 0f, 0, 0f, 0f),
    val completedDays: Set<Long> = emptySet(),
    val recentJournalEntries: List<JournalEntry> = emptyList(),
    val visibleMonth: YearMonth = YearMonth.now(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

class HabitDetailViewModel(
    private val habitRepository: HabitRepository,
    private val journalRepository: JournalRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private var habitId: Long = -1
    private val visibleMonth = MutableStateFlow(YearMonth.now())

    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    fun load(id: Long) {
        habitId = id
        viewModelScope.launch {
            combine(
                habitRepository.observeHabit(id),
                habitRepository.observeCompletionsForHabit(id),
                journalRepository.observeForHabit(id),
                visibleMonth
            ) { habit, completions, entries, month ->
                if (habit == null) {
                    HabitDetailUiState(isLoading = false, isDeleted = true)
                } else {
                    HabitDetailUiState(
                        habit = habit,
                        stats = StreakCalculator.computeStats(habit, completions),
                        completedDays = completions.filter { it.isComplete }.map { it.dateEpochDay }.toSet(),
                        recentJournalEntries = entries.take(5),
                        visibleMonth = month,
                        isLoading = false
                    )
                }
            }.collect { _uiState.value = it }
        }
    }

    fun toggleToday() {
        val habit = _uiState.value.habit ?: return
        viewModelScope.launch { habitRepository.toggleCompletion(habit, LocalDate.now()) }
    }

    fun setDayStatus(date: LocalDate, complete: Boolean) {
        val habit = _uiState.value.habit ?: return
        viewModelScope.launch { habitRepository.setHistoricalStatus(habit, date, complete) }
    }

    /**
     * Marks the most recent [days] days (including today) as complete in one
     * go - useful when switching from another tracker and wanting to carry
     * an existing streak over. If the habit's start date is later than the
     * requested range, the start date is pulled back automatically so those
     * days actually count toward the streak instead of being silently
     * skipped as "not yet started".
     */
    fun backfillLastNDays(days: Int) {
        val currentHabit = _uiState.value.habit ?: return
        if (days <= 0) return
        viewModelScope.launch {
            val today = LocalDate.now()
            val backfillStart = today.minusDays((days - 1).toLong())
            val needsEarlierStart = backfillStart.toEpochDay() < currentHabit.startDateEpochDay
            val effectiveHabit = if (needsEarlierStart) {
                currentHabit.copy(startDateEpochDay = backfillStart.toEpochDay())
            } else currentHabit

            if (needsEarlierStart) {
                habitRepository.updateHabit(effectiveHabit)
            }

            var d = backfillStart
            while (!d.isAfter(today)) {
                if (StreakCalculator.isScheduledOn(effectiveHabit, d)) {
                    habitRepository.setHistoricalStatus(effectiveHabit, d, true)
                }
                d = d.plusDays(1)
            }
        }
    }

    fun goToPreviousMonth() { visibleMonth.value = visibleMonth.value.minusMonths(1) }
    fun goToNextMonth() { visibleMonth.value = visibleMonth.value.plusMonths(1) }

    fun archiveHabit(onDone: () -> Unit) {
        viewModelScope.launch {
            habitRepository.setArchived(habitId, true)
            onDone()
        }
    }

    fun deleteHabit(onDone: () -> Unit) {
        val habit = _uiState.value.habit ?: return
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
            onDone()
        }
    }
}
