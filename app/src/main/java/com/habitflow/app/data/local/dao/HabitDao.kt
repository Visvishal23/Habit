package com.habitflow.app.data.local.dao

import androidx.room.*
import com.habitflow.app.data.local.entity.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder ASC, createdAtEpochMillis ASC")
    fun observeActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isArchived = 1 ORDER BY createdAtEpochMillis DESC")
    fun observeArchivedHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC")
    fun observeAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun observeHabit(habitId: Long): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabit(habitId: Long): Habit?

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsOnce(): List<Habit>

    @Insert
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)

    @Query("UPDATE habits SET isArchived = :archived WHERE id = :habitId")
    suspend fun setArchived(habitId: Long, archived: Boolean)

    @Query("UPDATE habits SET sortOrder = :order WHERE id = :habitId")
    suspend fun setSortOrder(habitId: Long, order: Int)
}
