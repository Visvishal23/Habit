package com.habitflow.app.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.data.local.entity.JournalEntry
import com.habitflow.app.data.local.entity.JournalPhoto
import com.habitflow.app.data.local.entity.Mood
import com.habitflow.app.data.repository.HabitRepository
import com.habitflow.app.data.repository.JournalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class JournalEntryWithHabit(val entry: JournalEntry, val habit: Habit?)

data class JournalUiState(
    val entries: List<JournalEntryWithHabit> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = true
)

class JournalViewModel(
    private val habitRepository: HabitRepository,
    private val journalRepository: JournalRepository
) : ViewModel() {

    val uiState: StateFlow<JournalUiState> = combine(
        journalRepository.observeAll(),
        habitRepository.observeActiveHabits()
    ) { entries, habits ->
        val habitsById = habits.associateBy { it.id }
        JournalUiState(
            entries = entries.map { JournalEntryWithHabit(it, habitsById[it.habitId]) },
            habits = habits,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalUiState())

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch { journalRepository.deleteEntry(entry) }
    }
}

data class AddEntryUiState(
    val habitId: Long = -1,
    val text: String = "",
    val mood: Mood = Mood.OKAY,
    val date: LocalDate = LocalDate.now(),
    val photoPaths: List<String> = emptyList(),
    val isSaving: Boolean = false
)

class AddJournalEntryViewModel(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEntryUiState())
    val state: StateFlow<AddEntryUiState> = _state.asStateFlow()

    fun init(habitId: Long) {
        _state.value = _state.value.copy(habitId = habitId)
    }

    fun update(transform: (AddEntryUiState) -> AddEntryUiState) {
        _state.value = transform(_state.value)
    }

    fun addPhotoBytes(bytes: ByteArray) {
        val path = journalRepository.copyPhotoIntoStorage(bytes)
        _state.value = _state.value.copy(photoPaths = _state.value.photoPaths + path)
    }

    fun removePhoto(path: String) {
        _state.value = _state.value.copy(photoPaths = _state.value.photoPaths - path)
    }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (s.text.isBlank() || s.habitId <= 0) return
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val entryId = journalRepository.addEntry(
                JournalEntry(
                    habitId = s.habitId,
                    dateEpochDay = s.date.toEpochDay(),
                    text = s.text.trim(),
                    mood = s.mood
                )
            )
            s.photoPaths.forEach { path -> journalRepository.addPhoto(entryId, path) }
            _state.value = s.copy(isSaving = false)
            onSaved()
        }
    }
}
