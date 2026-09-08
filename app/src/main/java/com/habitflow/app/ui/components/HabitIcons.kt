package com.habitflow.app.ui.components

/** Built-in emoji icon set - no external icon API/service is used anywhere. */
object HabitIcons {
    val all = listOf(
        "⭐", "💧", "📚", "🏃", "🧘", "🌙", "🥗", "💪", "🚴", "🎯",
        "✍️", "🎸", "🧹", "💊", "🚭", "🌱", "☀️", "❤️", "🧠", "💰",
        "🎨", "🗣️", "🚶", "🍎", "☕", "📵", "🛏️", "🧴", "🐾", "📝"
    )

    val categoryDefaults = mapOf(
        "Health" to "❤️", "Fitness" to "💪", "Study" to "📚", "Work" to "💼",
        "Personal" to "⭐", "Finance" to "💰", "Mindfulness" to "🧘", "Other" to "⭐"
    )
}

val DefaultCategories = listOf("Health", "Fitness", "Study", "Work", "Personal", "Finance", "Mindfulness", "Other")
