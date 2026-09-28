package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.UserSettings

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val wakePhrase: String = "Hey Maya",
    val stopCommand: String = "Bye Bye",
    val voiceName: String = "Female (Maya)",
    val voiceSpeed: Float = 0.85f,
    val voicePitch: Float = 1.05f,
    val voiceStyle: String = "Friendly",
    val language: String = "Bangla",
    val isFaceVerificationEnabled: Boolean = false,
    val isPinSecurityEnabled: Boolean = false,
    val pinCode: String = "1234",
    val isFloatingIconEnabled: Boolean = false,
    val isBackgroundServiceEnabled: Boolean = true,
    val themeMode: String = "dark",
    val userName: String = "User",
    val userEmail: String = "maya.user@aistudio.com",
    val isLoggedIn: Boolean = true,
    val isOnboardingCompleted: Boolean = true
) {
    fun toUserSettings(): UserSettings = UserSettings(
        wakePhrase = wakePhrase,
        stopCommand = stopCommand,
        voiceName = voiceName,
        voiceSpeed = voiceSpeed,
        voicePitch = voicePitch,
        voiceStyle = voiceStyle,
        language = language,
        isFaceVerificationEnabled = isFaceVerificationEnabled,
        isPinSecurityEnabled = isPinSecurityEnabled,
        pinCode = pinCode,
        isFloatingIconEnabled = isFloatingIconEnabled,
        isBackgroundServiceEnabled = isBackgroundServiceEnabled,
        themeMode = themeMode,
        userName = userName,
        userEmail = userEmail,
        isLoggedIn = isLoggedIn,
        isOnboardingCompleted = isOnboardingCompleted
    )

    companion object {
        fun fromUserSettings(settings: UserSettings): UserSettingsEntity = UserSettingsEntity(
            id = 1,
            wakePhrase = settings.wakePhrase,
            stopCommand = settings.stopCommand,
            voiceName = settings.voiceName,
            voiceSpeed = settings.voiceSpeed,
            voicePitch = settings.voicePitch,
            voiceStyle = settings.voiceStyle,
            language = settings.language,
            isFaceVerificationEnabled = settings.isFaceVerificationEnabled,
            isPinSecurityEnabled = settings.isPinSecurityEnabled,
            pinCode = settings.pinCode,
            isFloatingIconEnabled = settings.isFloatingIconEnabled,
            isBackgroundServiceEnabled = settings.isBackgroundServiceEnabled,
            themeMode = settings.themeMode,
            userName = settings.userName,
            userEmail = settings.userEmail,
            isLoggedIn = settings.isLoggedIn,
            isOnboardingCompleted = settings.isOnboardingCompleted
        )
    }
}
