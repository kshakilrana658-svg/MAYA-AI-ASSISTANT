package com.example.model

/**
 * Maya's Core Visual States as specified in the Master Prompt & UI Architecture.
 */
enum class MayaState(
    val displayName: String,
    val subtitle: String,
    val displayNameBn: String,
    val subtitleBn: String
) {
    SLEEPING("Sleeping", "Say \"Hey Maya\" or tap to speak", "ঘুমিয়ে আছে", "বলুন \"Hey Maya\" বা \"মায়া\""),
    LISTENING("Listening...", "I'm listening to your voice...", "শুনছি...", "আমি শুনছি, বলুন..."),
    UNDERSTANDING("Understanding...", "Processing your request...", "বুঝছি...", "আপনার কথা বুঝছি..."),
    VERIFYING("Verifying You...", "Please look at the camera...", "আপনাকে যাচাই করছি...", "অনুগ্রহ করে ক্যামেরার দিকে তাকান..."),
    THINKING("Thinking...", "Reasoning with Gemini AI...", "ভাবছি...", "চিন্তা করছি..."),
    WORKING("Working...", "Executing task on your device...", "কাজ করছি...", "কাজটি সম্পন্ন করছি..."),
    RESPONDING("Speaking...", "Speaking response...", "উত্তর দিচ্ছি...", "বলছি..."),
    SUCCESS("Completed", "Task executed successfully!", "কাজ সম্পন্ন হয়েছে", "হ্যাঁ, কাজ হয়ে গেছে!"),
    ERROR("Need Attention", "Could not complete the requested action", "কাজটি সম্পন্ন করা যায়নি", "কাজটি করতে পারলাম না"),
    PAUSED("Paused", "Say \"Hey Maya\" to resume", "বিরতি", "চালু করতে বলুন \"মায়া\""),
    STOPPED("Stopped", "Say \"Hey Maya\" to start", "বন্ধ", "শুরু করতে বলুন \"মায়া\"");

    fun getLabel(isBangla: Boolean): String = if (isBangla) displayNameBn else displayName
    fun getSub(isBangla: Boolean): String = if (isBangla) subtitleBn else subtitle
}

/**
 * Conversational Context for human-like dialogue, entity resolution ("ওটা", "আগেরটা"),
 * and multi-turn clarifications.
 */
data class ConversationContext(
    val lastTopic: String? = null,
    val lastMentionedApp: String? = null,
    val lastMentionedFile: String? = null,
    val lastCreatedFolder: String? = null,
    val awaitingClarificationFor: String? = null, // e.g. "FOLDER_NAME", "CONFIRM_ACTION", "WHICH_FILE"
    val pendingActionPayload: String? = null,
    val recentUtterances: List<String> = emptyList()
)

data class UserSettings(
    val wakePhrase: String = "Hey Maya",
    val stopCommand: String = "Bye Bye",
    val voiceName: String = "Female (Maya)",
    val voiceSpeed: Float = 0.85f,
    val voicePitch: Float = 1.05f,
    val voiceStyle: String = "Friendly", // Calm, Friendly, Professional, Energetic
    val language: String = "Bangla", // Default to Bangla/English bilingual
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
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "maya"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null
)

data class ManagedFile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val size: String,
    val category: FileCategory,
    val dateModified: String,
    val path: String
)

enum class FileCategory {
    DOCUMENTS,
    DOWNLOADS,
    IMAGES,
    VIDEOS,
    ARCHIVES
}

data class TaskStep(
    val title: String,
    val isCompleted: Boolean = false,
    val isInProgress: Boolean = false
)
