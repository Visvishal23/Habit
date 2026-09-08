package com.habitflow.app.domain

import com.habitflow.app.data.local.entity.FrequencyType
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.data.local.entity.HabitCompletion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields

data class HabitStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCompletions: Int,
    val completionRate: Float, // 0f..1f over scheduled days since start
    val missedDays: Int,
    val weeklyConsistency: Float, // this week's completions / scheduled-so-far
    val monthlyConsistency: Float
)

/**
 * All streak math lives here so both the habit-detail screen and the
 * statistics screen agree on the same numbers. A day only counts toward a
 * streak if the habit was actually *scheduled* on it - unscheduled days are
 * skipped rather than treated as misses.
 */
object StreakCalculator {

    fun isScheduledOn(habit: Habit, date: LocalDate): Boolean {
        if (date.toEpochDay() < habit.startDateEpochDay) return false
        return when (habit.frequencyType) {
            FrequencyType.DAILY -> true
            FrequencyType.SPECIFIC_WEEKDAYS -> {
                val isoDay = date.dayOfWeek.value // 1=Mon..7=Sun
                habit.scheduledWeekdays.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(isoDay)
            }
            // TIMES_PER_WEEK / CUSTOM don't lock specific days; every day is
            // eligible and we judge success at the week/period level instead.
            FrequencyType.TIMES_PER_WEEK, FrequencyType.CUSTOM -> true
        }
    }

    fun computeStats(
        habit: Habit,
        completions: List<HabitCompletion>,
        today: LocalDate = LocalDate.now()
    ): HabitStats {
        val completedDays = completions.filter { it.isComplete }.map { it.dateEpochDay }.toSet()
        val startDate = LocalDate.ofEpochDay(habit.startDateEpochDay)

        // --- Current & best streak (walk backwards/forwards day by day over scheduled days only) ---
        var bestStreak = 0
        var runningStreak = 0
        var date = startDate
        var currentStreak = 0
        var scheduledCount = 0
        var completedCount = 0

        while (!date.isAfter(today)) {
            if (isScheduledOn(habit, date)) {
                scheduledCount++
                if (completedDays.contains(date.toEpochDay())) {
                    runningStreak++
                    completedCount++
                    if (runningStreak > bestStreak) bestStreak = runningStreak
                } else {
                    runningStreak = 0
                }
            }
            date = date.plusDays(1)
        }

        // Current streak = the run ending today (or yesterday if today isn't done yet but was scheduled)
        currentStreak = run {
            var streak = 0
            var d = today
            while (!d.isBefore(startDate)) {
                if (isScheduledOn(habit, d)) {
                    if (completedDays.contains(d.toEpochDay())) {
                        streak++
                    } else if (d == today) {
                        // today not yet completed: don't break the streak, just don't count it
                        d = d.minusDays(1)
                        continue
                    } else {
                        break
                    }
                }
                d = d.minusDays(1)
            }
            streak
        }

        val missedDays = scheduledCount - completedCount
        val completionRate = if (scheduledCount > 0) completedCount.toFloat() / scheduledCount else 0f

        val weekFields = WeekFields.ISO
        val weekStart = today.with(weekFields.dayOfWeek(), 1L)
        val (weekScheduled, weekCompleted) = countRange(habit, completedDays, weekStart, today)
        val weeklyConsistency = if (weekScheduled > 0) weekCompleted.toFloat() / weekScheduled else 0f

        val monthStart = today.withDayOfMonth(1)
        val (monthScheduled, monthCompleted) = countRange(habit, completedDays, monthStart, today)
        val monthlyConsistency = if (monthScheduled > 0) monthCompleted.toFloat() / monthScheduled else 0f

        return HabitStats(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalCompletions = completedCount,
            completionRate = completionRate,
            missedDays = missedDays,
            weeklyConsistency = weeklyConsistency,
            monthlyConsistency = monthlyConsistency
        )
    }

    private fun countRange(
        habit: Habit,
        completedDays: Set<Long>,
        start: LocalDate,
        end: LocalDate
    ): Pair<Int, Int> {
        var scheduled = 0
        var completed = 0
        var d = start
        while (!d.isAfter(end)) {
            if (isScheduledOn(habit, d)) {
                scheduled++
                if (completedDays.contains(d.toEpochDay())) completed++
            }
            d = d.plusDays(1)
        }
        return scheduled to completed
    }

    /** Overall app-level progress for "today", e.g. "4 of 5 habits completed". */
    fun todayProgress(habits: List<Habit>, todaysCompletions: Map<Long, HabitCompletion>, today: LocalDate = LocalDate.now()): Pair<Int, Int> {
        val scheduledToday = habits.filter { !it.isArchived && isScheduledOn(it, today) }
        val doneCount = scheduledToday.count { todaysCompletions[it.id]?.isComplete == true }
        return doneCount to scheduledToday.size
    }
}
