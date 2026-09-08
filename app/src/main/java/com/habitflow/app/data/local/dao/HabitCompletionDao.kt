package com.habitflow.app.data.local.dao

import androidx.room.*
import com.habitflow.app.data.local.entity.HabitCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY dateEpochDay ASC")
    fun observeForHabit(habitId: Long): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY dateEpochDay ASC")
    suspend fun getForHabit(habitId: Long): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :day LIMIT 1")
    suspend fun getForHabitOnDay(habitId: Long, day: Long): HabitCompletion?

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay = :day")
    fun observeForDay(day: Long): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay BETWEEN :start AND :end")
    fun observeInRange(start: Long, end: Long): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions")
    fun observeAll(): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllOnce(): List<HabitCompletion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: HabitCompletion): Long

    @Delete
    suspend fun delete(completion: HabitCompletion)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :day")
    suspend fun deleteForHabitOnDay(habitId: Long, day: Long)
}
