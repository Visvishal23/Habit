package com.habitflow.app.data.local.dao

import androidx.room.*
import com.habitflow.app.data.local.entity.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE habitId = :habitId")
    fun observeForHabit(habitId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getAllEnabled(): List<Reminder>

    @Query("SELECT * FROM reminders")
    suspend fun getAllOnce(): List<Reminder>

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)
}
