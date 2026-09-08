package com.habitflow.app.ui.createhabit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflow.app.data.local.entity.FrequencyType
import com.habitflow.app.data.local.entity.GoalType
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.data.local.entity.Reminder
import com.habitflow.app.data.repository.HabitRepository
import com.habitflow.app.data.repository.ReminderRepository
import com.habitflow.app.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CreateHabitFormState(
    val habitId: Long? = null,
    val name: String = "",
    val description: String = "",
    val icon: String = "⭐",
    val colorHex: String = "#3D6E5C",
    val category: String = "Other",
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val scheduledWeekdays: Set<Int> = (1..7).toSet(),
    val timesPerWeek: Int = 3,
    val goalType: GoalType = GoalType.SIMPLE_CHECK,
    val goalTarget: Int = 1,
    val goalUnit: String = "",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val startDate: LocalDate = LocalDate.now(),
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false,
    val nameError: String? = null
)

class CreateHabitViewModel(
    private val habitRepository: HabitRepository,
    private val reminderRepository: ReminderRepository,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CreateHabitFormState())
    val state: StateFlow<CreateHabitFormState> = _state.asStateFlow()

    fun loadForEdit(habitId: Long) {
        if (habitId <= 0) {
            _state.value = _state.value.copy(isLoaded = true)
            return
        }
        viewModelScope.launch {
            val habit = habitRepository.getHabit(habitId) ?: run {
                _state.value = _state.value.copy(isLoaded = true)
                return@launch
            }
            _state.value = CreateHabitFormState(
                habitId = habit.id,
                name = habit.name,
                description = habit.description,
                icon = habit.icon,
                colorHex = habit.colorHex,
                category = habit.category,
                frequencyType = habit.frequencyType,
                scheduledWeekdays = habit.scheduledWeekdays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
                timesPerWeek = habit.timesPerWeek,
                goalType = habit.goalType,
                goalTarget = habit.goalTarget,
                goalUnit = habit.goalUnit,
                reminderEnabled = habit.reminderEnabled,
                startDate = LocalDate.ofEpochDay(habit.startDateEpochDay),
                isLoaded = true
            )
        }
    }

    fun update(transform: (CreateHabitFormState) -> CreateHabitFormState) {
        _state.value = transform(_state.value).copy(nameError = null)
    }

    fun save(onSaved: (Long) -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(nameError = "Give this habit a name")
            return
        }
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val habit = Habit(
                id = s.habitId ?: 0,
                name = s.name.trim(),
                description = s.description.trim(),
                icon = s.icon,
                colorHex = s.colorHex,
                category = s.category,
                frequencyType = s.frequencyType,
                scheduledWeekdays = s.scheduledWeekdays.sorted().joinToString(","),
                timesPerWeek = s.timesPerWeek,
                goalType = s.goalType,
                goalTarget = s.goalTarget,
                goalUnit = s.goalUnit,
                reminderEnabled = s.reminderEnabled,
                startDateEpochDay = s.startDate.toEpochDay()
            )
            val id = if (s.habitId != null) {
                habitRepository.updateHabit(habit)
                s.habitId
            } else {
                habitRepository.createHabit(habit)
            }

            if (s.reminderEnabled) {
                val reminder = Reminder(
                    habitId = id,
                    hour = s.reminderHour,
                    minute = s.reminderMinute,
                    label = "Time for ${s.name.trim()}"
                )
                val reminderId = reminderRepository.add(reminder)
                ReminderScheduler.schedule(appContext, reminder.copy(id = reminderId))
            }

            _state.value = s.copy(isSaving = false)
            onSaved(id)
        }
    }
}
