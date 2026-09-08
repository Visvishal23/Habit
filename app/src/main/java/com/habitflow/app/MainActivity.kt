package com.habitflow.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.habitflow.app.data.local.entity.AppThemeMode
import com.habitflow.app.ui.ViewModelFactory
import com.habitflow.app.ui.navigation.HabitFlowNavGraph
import com.habitflow.app.ui.navigation.Screen
import com.habitflow.app.ui.theme.HabitFlowTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: reminders simply won't fire if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        val app = application as HabitFlowApplication
        val factory = ViewModelFactory(app)

        setContent {
            val settings by app.settingsRepository.settings.collectAsState(initial = null)
            val themeMode = settings?.themeMode ?: AppThemeMode.SYSTEM

            HabitFlowTheme(themeMode = themeMode) {
                Surface(modifier = Modifier) {
                    val currentSettings = settings
                    if (currentSettings != null) {
                        val startDestination = if (currentSettings.onboardingCompleted) Screen.Home.route else Screen.Onboarding.route
                        HabitFlowNavGraph(startDestination = startDestination, factory = factory)
                    }
                    // else: settings not loaded yet - render nothing this frame rather than
                    // flashing onboarding, without using a non-local return from this lambda.
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
