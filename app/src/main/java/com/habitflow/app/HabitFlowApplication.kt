package com.habitflow.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.habitflow.app.data.local.AppDatabase
import com.habitflow.app.data.repository.BackupRepository
import com.habitflow.app.data.repository.HabitRepository
import com.habitflow.app.data.repository.JournalRepository
import com.habitflow.app.data.repository.ReminderRepository
import com.habitflow.app.data.repository.SettingsRepository

/**
 * HabitFlow never talks to the network. Everything below is wired locally:
 * a single Room database and a handful of repositories built on top of it.
 * No dependency-injection framework is used so the whole data layer is easy
 * to read in one pass.
 */
class HabitFlowApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val habitRepository: HabitRepository by lazy {
        HabitRepository(database.habitDao(), database.habitCompletionDao())
    }
    val journalRepository: JournalRepository by lazy {
        JournalRepository(database.journalDao(), filesDir)
    }
    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao())
    }
    val backupRepository: BackupRepository by lazy {
        BackupRepository(database, applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Habit reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Local reminders to complete your habits"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "habit_reminders"
    }
}
