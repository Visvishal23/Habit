package com.habitflow.app.data.local.entity

/** Not a Room entity - preferences live in DataStore. Kept here as the shared model. */
enum class AppThemeMode { LIGHT, DARK, SYSTEM }
enum class HomeViewMode { LIST, STREAK }

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val homeViewMode: HomeViewMode = HomeViewMode.LIST,
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false
)
