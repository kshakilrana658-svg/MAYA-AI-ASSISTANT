package com.example.db

import com.example.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val settingsDao: SettingsDao) {
    val settingsFlow: Flow<UserSettings> = settingsDao.getSettingsFlow().map { entity ->
        entity?.toUserSettings() ?: UserSettings()
    }

    suspend fun getSettings(): UserSettings {
        return settingsDao.getSettings()?.toUserSettings() ?: UserSettings()
    }

    suspend fun saveSettings(settings: UserSettings) {
        settingsDao.saveSettings(UserSettingsEntity.fromUserSettings(settings))
    }
}
