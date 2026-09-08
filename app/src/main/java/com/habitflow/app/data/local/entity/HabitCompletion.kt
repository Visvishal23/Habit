package com.habitflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per (habit, calendar day). [progressValue] holds the numeric
 * amount logged that day (e.g. minutes, pages, or 1/0 for a simple check);
 * [isComplete] is the derived pass/fail against the habit's goal, stored
 * redundantly so streak queries stay index-friendly.
 */
@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId", "dateEpochDay"], unique = true)]
)
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val dateEpochDay: Long,
    val progressValue: Int = 0,
    val isComplete: Boolean = false,
    val completedAtEpochMillis: Long = System.currentTimeMillis()
)
