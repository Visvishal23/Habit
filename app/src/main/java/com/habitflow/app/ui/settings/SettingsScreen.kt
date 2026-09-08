package com.habitflow.app.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitflow.app.BuildConfig
import com.habitflow.app.data.local.entity.AppThemeMode
import com.habitflow.app.data.local.entity.HomeViewMode

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val backupResult by viewModel.backupResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(it) } }

    LaunchedEffect(backupResult) {
        val message = when (val r = backupResult) {
            is BackupResult.ExportSuccess -> "Backup saved."
            is BackupResult.ImportSuccess -> "Imported ${r.summary.habitsImported} habits, ${r.summary.entriesImported} journal entries."
            is BackupResult.Error -> r.message
            BackupResult.None -> null
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearBackupResult()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            item {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(20.dp))

                SettingsSection("Appearance") {
                    ThemeOptionRow(settings.themeMode, viewModel::setThemeMode)
                }

                SettingsSection("Home View") {
                    HomeViewOptionRow(settings.homeViewMode, viewModel::setHomeViewMode)
                }

                SettingsSection("Notifications") {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enable reminders")
                        Switch(checked = settings.notificationsEnabled, onCheckedChange = viewModel::setNotificationsEnabled)
                    }
                }

                SettingsSection("Data") {
                    Column {
                        SettingsActionRow("Export backup") {
                            exportLauncher.launch("habitflow_backup.json")
                        }
                        SettingsActionRow("Import backup") {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    }
                }

                SettingsSection("About") {
                    Column {
                        Text("HabitFlow", fontWeight = FontWeight.Medium)
                        Text(
                            "Version ${runCatching { BuildConfig.VERSION_NAME }.getOrDefault("1.0.0")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Your habit data is stored locally on your device. This app does not require an account or cloud synchronization.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) { content() }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun ThemeOptionRow(current: AppThemeMode, onChange: (AppThemeMode) -> Unit) {
    Column {
        listOf(AppThemeMode.SYSTEM to "System Default", AppThemeMode.LIGHT to "Light", AppThemeMode.DARK to "Dark").forEach { (mode, label) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                RadioButton(selected = current == mode, onClick = { onChange(mode) })
                Spacer(Modifier.width(6.dp))
                Text(label)
            }
        }
    }
}

@Composable
private fun HomeViewOptionRow(current: HomeViewMode, onChange: (HomeViewMode) -> Unit) {
    Column {
        listOf(HomeViewMode.LIST to "List", HomeViewMode.STREAK to "Streak").forEach { (mode, label) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                RadioButton(selected = current == mode, onClick = { onChange(mode) })
                Spacer(Modifier.width(6.dp))
                Text(label)
            }
        }
    }
}

@Composable
private fun SettingsActionRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Text(label)
        }
    }
}
