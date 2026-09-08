package com.habitflow.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflow.app.data.local.entity.AppThemeMode
import com.habitflow.app.data.local.entity.HomeViewMode
import com.habitflow.app.data.local.entity.UserSettings
import com.habitflow.app.data.repository.BackupRepository
import com.habitflow.app.data.repository.ImportSummary
import com.habitflow.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BackupResult {
    data object None : BackupResult()
    data object ExportSuccess : BackupResult()
    data class ImportSuccess(val summary: ImportSummary) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _backupResult = MutableStateFlow<BackupResult>(BackupResult.None)
    val backupResult: StateFlow<BackupResult> = _backupResult

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setHomeViewMode(mode: HomeViewMode) {
        viewModelScope.launch { settingsRepository.setHomeViewMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.exportToUri(uri)
            _backupResult.value = result.fold(
                onSuccess = { BackupResult.ExportSuccess },
                onFailure = { BackupResult.Error(it.message ?: "Export failed") }
            )
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.importFromUri(uri)
            _backupResult.value = result.fold(
                onSuccess = { BackupResult.ImportSuccess(it) },
                onFailure = { BackupResult.Error(it.message ?: "Import failed") }
            )
        }
    }

    fun clearBackupResult() { _backupResult.value = BackupResult.None }
}
