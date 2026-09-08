package com.habitflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FrequencyType { DAILY, SPECIFIC_WEEKDAYS, TIMES_PER_WEEK, CUSTOM }
enum class GoalType { SIMPLE_CHECK, TIMES_PER_PERIOD, NUMERIC_TARGET }

/**
 * Core habit definition. Scheduling and goal details are intentionally kept
 * on the same row (rather than a separate schedule table) since a habit has
 * exactly one active schedule at a time; history of *completions* is what
 * lives in [HabitCompletion].
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    /** Emoji glyph from the app's built-in icon set (see HabitIcons). No external icon API. */
    val icon: String = "⭐",
    val colorHex: String = "#3D6E5C",
    val category: String = "Other",
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    /** Comma-separated ISO day numbers (1=Mon..7=Sun), used for SPECIFIC_WEEKDAYS */
    val scheduledWeekdays: String = "1,2,3,4,5,6,7",
    /** Used for TIMES_PER_WEEK */
    val timesPerWeek: Int = 7,
    val goalType: GoalType = GoalType.SIMPLE_CHECK,
    val goalTarget: Int = 1,
    val goalUnit: String = "",
    val reminderEnabled: Boolean = false,
    val startDateEpochDay: Long,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val isArchived: Boolean = false
)
