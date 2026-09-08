package com.habitflow.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.habitflow.app.HabitFlowApplication
import com.habitflow.app.ui.calendar.CalendarViewModel
import com.habitflow.app.ui.createhabit.CreateHabitViewModel
import com.habitflow.app.ui.habitdetail.HabitDetailViewModel
import com.habitflow.app.ui.home.HomeViewModel
import com.habitflow.app.ui.journal.AddJournalEntryViewModel
import com.habitflow.app.ui.journal.JournalViewModel
import com.habitflow.app.ui.settings.SettingsViewModel
import com.habitflow.app.ui.statistics.StatisticsViewModel

/**
 * A single factory that knows how to build every screen's ViewModel from the
 * repositories held on [HabitFlowApplication]. Avoids pulling in a DI
 * framework for what is otherwise a small, single-module app.
 */
class ViewModelFactory(private val app: HabitFlowApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(app.habitRepository, app.settingsRepository) as T
            CalendarViewModel::class.java -> CalendarViewModel(app.habitRepository) as T
            StatisticsViewModel::class.java -> StatisticsViewModel(app.habitRepository) as T
            JournalViewModel::class.java -> JournalViewModel(app.habitRepository, app.journalRepository) as T
            AddJournalEntryViewModel::class.java -> AddJournalEntryViewModel(app.journalRepository) as T
            SettingsViewModel::class.java -> SettingsViewModel(app.settingsRepository, app.backupRepository) as T
            CreateHabitViewModel::class.java -> CreateHabitViewModel(app.habitRepository, app.reminderRepository, app) as T
            HabitDetailViewModel::class.java -> HabitDetailViewModel(app.habitRepository, app.journalRepository, app.reminderRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
