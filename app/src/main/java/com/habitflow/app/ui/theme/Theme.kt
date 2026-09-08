package com.habitflow.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.habitflow.app.data.local.entity.AppThemeMode

private val LightColors = lightColorScheme(
    primary = SagePrimaryLight,
    primaryContainer = SagePrimaryContainerLight,
    onPrimaryContainer = SageOnPrimaryContainerLight,
    secondary = SageSecondaryLight,
    background = SageBackgroundLight,
    surface = SageSurfaceLight,
    surfaceVariant = SageSurfaceVariantLight,
    error = SageErrorLight
)

private val DarkColors = darkColorScheme(
    primary = SagePrimaryDark,
    primaryContainer = SagePrimaryContainerDark,
    onPrimaryContainer = SageOnPrimaryContainerDark,
    secondary = SageSecondaryDark,
    background = SageBackgroundDark,
    surface = SageSurfaceDark,
    surfaceVariant = SageSurfaceVariantDark,
    error = SageErrorDark
)

@Composable
fun HabitFlowTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    val context = LocalContext.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamicColor && dynamicAvailable && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && dynamicAvailable && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HabitFlowTypography,
        content = content
    )
}
