package com.habitflow.app.data.local

import androidx.room.TypeConverter
import com.habitflow.app.data.local.entity.FrequencyType
import com.habitflow.app.data.local.entity.GoalType
import com.habitflow.app.data.local.entity.Mood

class Converters {
    @TypeConverter
    fun fromFrequencyType(value: FrequencyType): String = value.name
    @TypeConverter
    fun toFrequencyType(value: String): FrequencyType = FrequencyType.valueOf(value)

    @TypeConverter
    fun fromGoalType(value: GoalType): String = value.name
    @TypeConverter
    fun toGoalType(value: String): GoalType = GoalType.valueOf(value)

    @TypeConverter
    fun fromMood(value: Mood): String = value.name
    @TypeConverter
    fun toMood(value: String): Mood = Mood.valueOf(value)
}
