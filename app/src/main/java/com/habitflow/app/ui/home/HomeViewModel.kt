package com.habitflow.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.data.local.entity.HabitCompletion
import com.habitflow.app.data.local.entity.HomeViewMode
import com.habitflow.app.data.repository.HabitRepository
import com.habitflow.app.data.repository.SettingsRepository
import com.habitflow.app.domain.HabitStats
import com.habitflow.app.domain.StreakCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitCardUiState(
    val habit: Habit,
    val isCompletedToday: Boolean,
    val stats: HabitStats
)

data class HomeUiState(
    val habitCards: List<HabitCardUiState> = emptyList(),
    val doneToday: Int = 0,
    val scheduledToday: Int = 0,
    val viewMode: HomeViewMode = HomeViewMode.LIST,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        habitRepository.observeActiveHabits(),
        habitRepository.observeAllCompletions(),
        settingsRepository.settings.map { it.homeViewMode }.distinctUntilChanged()
    ) { habits, allCompletions, viewMode ->
        val today = LocalDate.now()
        val byHabit: Map<Long, List<HabitCompletion>> = allCompletions.groupBy { it.habitId }
        val todaysByHabit = allCompletions.filter { it.dateEpochDay == today.toEpochDay() }.associateBy { it.habitId }

        val cards = habits.map { habit ->
            val completions = byHabit[habit.id].orEmpty()
            HabitCardUiState(
                habit = habit,
                isCompletedToday = todaysByHabit[habit.id]?.isComplete == true,
                stats = StreakCalculator.computeStats(habit, completions, today)
            )
        }
        val (done, scheduled) = StreakCalculator.todayProgress(habits, todaysByHabit, today)
        HomeUiState(
            habitCards = cards,
            doneToday = done,
            scheduledToday = scheduled,
            viewMode = viewMode,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.toggleCompletion(habit, LocalDate.now())
        }
    }

    fun setViewMode(mode: HomeViewMode) {
        viewModelScope.launch { settingsRepository.setHomeViewMode(mode) }
    }
}
