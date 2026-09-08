package com.habitflow.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Calendar : Screen("calendar")
    data object Statistics : Screen("statistics")
    data object Journal : Screen("journal")
    data object Settings : Screen("settings")

    data object CreateHabit : Screen("create_habit?habitId={habitId}") {
        fun new() = "create_habit?habitId=-1"
        fun edit(habitId: Long) = "create_habit?habitId=$habitId"
        const val ARG_HABIT_ID = "habitId"
    }

    data object HabitDetail : Screen("habit_detail/{habitId}") {
        fun build(habitId: Long) = "habit_detail/$habitId"
        const val ARG_HABIT_ID = "habitId"
    }

    data object AddJournalEntry : Screen("add_journal_entry/{habitId}") {
        fun build(habitId: Long) = "add_journal_entry/$habitId"
        const val ARG_HABIT_ID = "habitId"
    }

    companion object {
        val bottomNavItems = listOf(Home, Calendar, Statistics, Journal, Settings)
    }
}
