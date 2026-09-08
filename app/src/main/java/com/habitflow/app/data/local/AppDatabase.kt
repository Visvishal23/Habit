package com.habitflow.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.habitflow.app.data.local.dao.HabitCompletionDao
import com.habitflow.app.data.local.dao.HabitDao
import com.habitflow.app.data.local.dao.JournalDao
import com.habitflow.app.data.local.dao.ReminderDao
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.data.local.entity.HabitCompletion
import com.habitflow.app.data.local.entity.JournalEntry
import com.habitflow.app.data.local.entity.JournalPhoto
import com.habitflow.app.data.local.entity.Reminder

@Database(
    entities = [
        Habit::class,
        HabitCompletion::class,
        JournalEntry::class,
        JournalPhoto::class,
        Reminder::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun journalDao(): JournalDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habitflow.db"
                ).build().also { INSTANCE = it }
            }
    }
}
