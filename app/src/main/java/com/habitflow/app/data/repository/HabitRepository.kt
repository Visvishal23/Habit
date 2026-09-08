package com.habitflow.app.data.repository

import com.habitflow.app.data.local.dao.HabitCompletionDao
import com.habitflow.app.data.local.dao.HabitDao
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.data.local.entity.HabitCompletion
import com.habitflow.app.domain.StreakCalculator
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class HabitRepository(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao
) {
    fun observeActiveHabits(): Flow<List<Habit>> = habitDao.observeActiveHabits()
    fun observeArchivedHabits(): Flow<List<Habit>> = habitDao.observeArchivedHabits()
    fun observeHabit(habitId: Long): Flow<Habit?> = habitDao.observeHabit(habitId)
    fun observeCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> =
        completionDao.observeForHabit(habitId)
    fun observeCompletionsForDay(day: LocalDate): Flow<List<HabitCompletion>> =
        completionDao.observeForDay(day.toEpochDay())
    fun observeCompletionsInRange(start: LocalDate, end: LocalDate): Flow<List<HabitCompletion>> =
        completionDao.observeInRange(start.toEpochDay(), end.toEpochDay())
    fun observeAllCompletions(): Flow<List<HabitCompletion>> = completionDao.observeAll()

    suspend fun createHabit(habit: Habit): Long = habitDao.insert(habit)
    suspend fun updateHabit(habit: Habit) = habitDao.update(habit)
    suspend fun deleteHabit(habit: Habit) = habitDao.delete(habit)
    suspend fun setArchived(habitId: Long, archived: Boolean) = habitDao.setArchived(habitId, archived)
    suspend fun reorder(habitId: Long, order: Int) = habitDao.setSortOrder(habitId, order)
    suspend fun getHabit(habitId: Long): Habit? = habitDao.getHabit(habitId)

    /** Toggles today's (or any day's) completion, respecting a habit's numeric goal. */
    suspend fun toggleCompletion(habit: Habit, date: LocalDate) {
        val existing = completionDao.getForHabitOnDay(habit.id, date.toEpochDay())
        if (existing != null && existing.isComplete) {
            completionDao.deleteForHabitOnDay(habit.id, date.toEpochDay())
        } else {
            completionDao.upsert(
                HabitCompletion(
                    habitId = habit.id,
                    dateEpochDay = date.toEpochDay(),
                    progressValue = habit.goalTarget,
                    isComplete = true
                )
            )
        }
    }

    /** Sets an exact progress value for numeric-goal habits (e.g. "10 minutes"). */
    suspend fun setProgress(habit: Habit, date: LocalDate, value: Int) {
        val isComplete = value >= habit.goalTarget
        completionDao.upsert(
            HabitCompletion(
                habitId = habit.id,
                dateEpochDay = date.toEpochDay(),
                progressValue = value,
                isComplete = isComplete
            )
        )
    }

    /** Lets the user manually correct a past day's status; stats are recalculated live from Flow. */
    suspend fun setHistoricalStatus(habit: Habit, date: LocalDate, complete: Boolean) {
        if (complete) {
            completionDao.upsert(
                HabitCompletion(
                    habitId = habit.id,
                    dateEpochDay = date.toEpochDay(),
                    progressValue = habit.goalTarget,
                    isComplete = true
                )
            )
        } else {
            completionDao.deleteForHabitOnDay(habit.id, date.toEpochDay())
        }
    }

    suspend fun computeStats(habit: Habit) =
        StreakCalculator.computeStats(habit, completionDao.getForHabit(habit.id))
}
