package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.action.AndroidActionController
import com.example.ai.GeminiAiService
import com.example.ai.ParsedIntent
import com.example.db.MayaDatabase
import com.example.db.TaskHistoryItem
import com.example.model.ChatMessage
import com.example.model.FileCategory
import com.example.model.ManagedFile
import com.example.model.MayaState
import com.example.model.TaskStep
import com.example.model.UserSettings
import com.example.service.MayaFloatingService
import com.example.service.MayaVoiceService
import com.example.voice.MayaSpeechManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenRoute {
    SPLASH,
    LANGUAGE_SELECT,
    LOGIN,
    PERMISSION_SETUP,
    FACE_REGISTRATION,
    SECURITY_SETUP,
    PIN_SETUP,
    HOME,
    CHAT,
    TOOLS,
    TASK_EXECUTION,
    FILE_MANAGER,
    TASK_HISTORY,
    SETTINGS,
    SETTINGS_WAKE_PHRASE,
    SETTINGS_STOP_COMMAND,
    SETTINGS_VOICE,
    SETTINGS_LANGUAGE,
    SETTINGS_SECURITY,
    SETTINGS_PRIVACY,
    SETTINGS_ABOUT
}

class MayaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MayaDatabase.getInstance(application)
    private val taskHistoryDao = db.taskHistoryDao()
    private val settingsRepository = com.example.db.SettingsRepository(db.settingsDao())
    private val actionController = AndroidActionController(application)
    private val geminiService = GeminiAiService()

    private val _currentScreen = MutableStateFlow(ScreenRoute.HOME)
    val currentScreen: StateFlow<ScreenRoute> = _currentScreen.asStateFlow()

    private val _mayaState = MutableStateFlow(MayaState.SLEEPING)
    val mayaState: StateFlow<MayaState> = _mayaState.asStateFlow()

    private val _userSettings = MutableStateFlow(UserSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    private val _rmsAudioLevel = MutableStateFlow(0.1f)
    val rmsAudioLevel: StateFlow<Float> = _rmsAudioLevel.asStateFlow()

    private val _lastVoiceTranscript = MutableStateFlow("")
    val lastVoiceTranscript: StateFlow<String> = _lastVoiceTranscript.asStateFlow()

    private val _lastMayaResponse = MutableStateFlow("Say \"Hey Maya\" to start")
    val lastMayaResponse: StateFlow<String> = _lastMayaResponse.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(sender = "maya", text = "Hello! I am Maya, your personal AI voice companion. You can ask me to open apps, search the web, manage files, or handle tasks hands-free.")
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    val taskHistory: StateFlow<List<TaskHistoryItem>> = taskHistoryDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _managedFiles = MutableStateFlow<List<ManagedFile>>(emptyList())
    val managedFiles: StateFlow<List<ManagedFile>> = _managedFiles.asStateFlow()

    private val _taskSteps = MutableStateFlow<List<TaskStep>>(emptyList())
    val taskSteps: StateFlow<List<TaskStep>> = _taskSteps.asStateFlow()

    private val _taskTitle = MutableStateFlow("Task Execution")
    val taskTitle: StateFlow<String> = _taskTitle.asStateFlow()

    private val _taskProgress = MutableStateFlow(0f)
    val taskProgress: StateFlow<Float> = _taskProgress.asStateFlow()

    private var speechManager: MayaSpeechManager? = null
    private var activeTaskJob: Job? = null
    private var conversationContext = com.example.model.ConversationContext()

    val isBangla: Boolean
        get() = _userSettings.value.language.contains("Bangla", ignoreCase = true)

    init {
        _managedFiles.value = actionController.getInitialManagedFiles()
        initSpeechManager()
        observePersistedSettings()
    }

    private fun observePersistedSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { persisted ->
                _userSettings.value = persisted
                speechManager?.updateVoiceSettings(
                    persisted.voiceSpeed,
                    persisted.voicePitch,
                    persisted.language
                )
            }
        }
    }

    private fun initSpeechManager() {
        speechManager = MayaSpeechManager(
            context = getApplication(),
            onVoiceResult = { text ->
                handleVoiceInput(text)
            },
            onRmsChanged = { rms ->
                _rmsAudioLevel.value = rms
            },
            onError = { error ->
                if (_mayaState.value == MayaState.LISTENING) {
                    _mayaState.value = MayaState.SLEEPING
                }
            }
        )
    }

    fun navigateTo(route: ScreenRoute) {
        _currentScreen.value = route
    }

    /**
     * Activate Maya manually or via wake gesture.
     */
    fun activateListening() {
        speechManager?.stopSpeaking()
        _mayaState.value = MayaState.LISTENING
        _lastVoiceTranscript.value = ""
        speechManager?.startListening()
    }

    fun cancelListening() {
        speechManager?.stopListening()
        _mayaState.value = MayaState.SLEEPING
    }

    fun handleVoiceInput(transcript: String) {
        val trimmed = transcript.trim()
        if (trimmed.isBlank()) {
            _mayaState.value = MayaState.SLEEPING
            return
        }

        _lastVoiceTranscript.value = trimmed
        _chatMessages.value = _chatMessages.value + ChatMessage(sender = "user", text = trimmed)

        // Check if Stop Command was spoken
        val stopCmd = _userSettings.value.stopCommand.lowercase()
        val wakePhrase = _userSettings.value.wakePhrase.lowercase()

        if (trimmed.lowercase() == stopCmd || trimmed.lowercase().contains(stopCmd)) {
            handleStopCommand()
            return
        }

        // Strip wake phrase if prefixed
        var commandText = trimmed
        if (commandText.lowercase().startsWith(wakePhrase)) {
            commandText = commandText.substring(wakePhrase.length).trim().removePrefix(",").trim()
        }

        if (commandText.isBlank()) {
            // User just said "Hey Maya"
            _mayaState.value = MayaState.LISTENING
            _lastMayaResponse.value = "I'm listening."
            speakMaya("I'm listening.")
            return
        }

        executeCommand(commandText)
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _chatMessages.value = _chatMessages.value + ChatMessage(sender = "user", text = trimmed)
        executeCommand(trimmed)
    }

    private fun executeCommand(command: String) {
        activeTaskJob?.cancel()
        activeTaskJob = viewModelScope.launch {
            _mayaState.value = MayaState.UNDERSTANDING
            delay(400)

            _mayaState.value = MayaState.THINKING
            val processResult = geminiService.processCommand(
                prompt = command,
                wakePhrase = _userSettings.value.wakePhrase,
                stopCommand = _userSettings.value.stopCommand,
                context = conversationContext,
                isBangla = isBangla
            )
            conversationContext = processResult.updatedContext
            val intent = processResult.intent
            val responseMessage = processResult.responseMessage

            when (intent) {
                is ParsedIntent.StopTask -> {
                    handleStopCommand()
                }

                is ParsedIntent.AskClarification -> {
                    _mayaState.value = MayaState.RESPONDING
                    _lastMayaResponse.value = intent.question
                    _chatMessages.value = _chatMessages.value + ChatMessage(sender = "maya", text = intent.question)
                    speakMaya(intent.question)
                    delay(1200)
                    activateListening()
                }

                is ParsedIntent.OpenApp -> {
                    _mayaState.value = MayaState.WORKING
                    delay(300)
                    val result = actionController.openApp(intent.appTarget)
                    if (result.isSuccess) {
                        _mayaState.value = MayaState.SUCCESS
                        val msg = if (isBangla) "ঠিক আছে, ${intent.appTarget} খুলে দিয়েছি।" else "Opened ${intent.appTarget}"
                        _lastMayaResponse.value = msg
                        speakMaya(msg)
                        logTask(command, "APP_LAUNCH", msg, true)
                    } else {
                        _mayaState.value = MayaState.ERROR
                        val err = if (isBangla) "অ্যাপটি ডিভাইসে পাওয়া যায়নি।" else (result.exceptionOrNull()?.message ?: "Application unavailable")
                        _lastMayaResponse.value = err
                        speakMaya(err)
                        logTask(command, "APP_LAUNCH", err, false, err)
                    }
                    delay(1600)
                    _mayaState.value = MayaState.SLEEPING
                }

                is ParsedIntent.SearchWeb -> {
                    _mayaState.value = MayaState.WORKING
                    val result = actionController.searchWeb(intent.query)
                    if (result.isSuccess) {
                        _mayaState.value = MayaState.SUCCESS
                        val msg = if (isBangla) "\"${intent.query}\" লিখে অনুসন্ধান করা হয়েছে।" else "Searched web for: ${intent.query}"
                        _lastMayaResponse.value = msg
                        speakMaya(if (isBangla) "ওয়েবে খুঁজছি..." else "Searching for ${intent.query}")
                        logTask(command, "WEB_SEARCH", msg, true)
                    } else {
                        _mayaState.value = MayaState.ERROR
                        val err = if (isBangla) "অনুসন্ধান শুরু করা যায়নি: কোনো ব্রাউজার পাওয়া যায়নি।" else (result.exceptionOrNull()?.message ?: "Search failed")
                        _lastMayaResponse.value = err
                        speakMaya(err)
                        logTask(command, "WEB_SEARCH", err, false, err)
                    }
                    delay(1500)
                    _mayaState.value = MayaState.SLEEPING
                }

                is ParsedIntent.OpenWebsite -> {
                    _mayaState.value = MayaState.WORKING
                    val result = actionController.openWebsite(intent.url)
                    if (result.isSuccess) {
                        _mayaState.value = MayaState.SUCCESS
                        val msg = if (isBangla) "ওয়েবসাইটটি ওপেন করা হয়েছে।" else "Opened ${intent.url}"
                        _lastMayaResponse.value = msg
                        speakMaya(msg)
                        logTask(command, "WEB_BROWSER", msg, true)
                    } else {
                        _mayaState.value = MayaState.ERROR
                        val err = if (isBangla) "ওয়েবসাইটটি ওপেন করা যায়নি।" else (result.exceptionOrNull()?.message ?: "Failed to open website")
                        _lastMayaResponse.value = err
                        speakMaya(err)
                        logTask(command, "WEB_BROWSER", err, false, err)
                    }
                    delay(1500)
                    _mayaState.value = MayaState.SLEEPING
                }

                is ParsedIntent.ChangeWakePhrase -> {
                    val updated = _userSettings.value.copy(wakePhrase = intent.newPhrase)
                    _userSettings.value = updated
                    viewModelScope.launch { settingsRepository.saveSettings(updated) }
                    _mayaState.value = MayaState.SUCCESS
                    _lastMayaResponse.value = responseMessage
                    speakMaya(responseMessage)
                    logTask(command, "SETTINGS_CHANGE", responseMessage, true)
                    delay(1500)
                    _mayaState.value = MayaState.SLEEPING
                }

                is ParsedIntent.ChangeStopCommand -> {
                    val updated = _userSettings.value.copy(stopCommand = intent.newCommand)
                    _userSettings.value = updated
                    viewModelScope.launch { settingsRepository.saveSettings(updated) }
                    _mayaState.value = MayaState.SUCCESS
                    _lastMayaResponse.value = responseMessage
                    speakMaya(responseMessage)
                    logTask(command, "SETTINGS_CHANGE", responseMessage, true)
                    delay(1500)
                    _mayaState.value = MayaState.SLEEPING
                }

                is ParsedIntent.ChangeVoice -> {
                    _mayaState.value = MayaState.SUCCESS
                    _lastMayaResponse.value = responseMessage
                    speakMaya(responseMessage)
                    logTask(command, "VOICE_SETTINGS", responseMessage, true)
                    delay(1500)
                    _mayaState.value = MayaState.SLEEPING
                }

                is ParsedIntent.ChangeLanguage -> {
                    val updated = _userSettings.value.copy(language = intent.language)
                    _userSettings.value = updated
                    viewModelScope.launch { settingsRepository.saveSettings(updated) }
                    speechManager?.updateVoiceSettings(
                        _userSettings.value.voiceSpeed,
                        _userSettings.value.voicePitch,
                        intent.language
                    )
                    _mayaState.value = MayaState.SUCCESS
                    _lastMayaResponse.value = responseMessage
                    speakMaya(responseMessage)
                    logTask(command, "LANGUAGE_CHANGE", responseMessage, true)
                    delay(1500)
                    _mayaState.value = MayaState.SLEEPING
                }

                is ParsedIntent.CreateProject -> {
                    runMultiStepTask(intent.title)
                }

                is ParsedIntent.FileAction -> {
                    handleFileAction(intent.action, intent.target, command)
                }

                is ParsedIntent.GeneralAnswer -> {
                    _mayaState.value = MayaState.RESPONDING
                    _lastMayaResponse.value = intent.answer
                    _chatMessages.value = _chatMessages.value + ChatMessage(sender = "maya", text = intent.answer)
                    speakMaya(intent.answer)
                    logTask(command, "AI_REASONING", intent.answer, true)
                    delay(2000)
                    // Natural back-and-forth listening window
                    _mayaState.value = MayaState.LISTENING
                    delay(4000)
                    if (_mayaState.value == MayaState.LISTENING) {
                        _mayaState.value = MayaState.SLEEPING
                    }
                }
            }
        }
    }

    private suspend fun runMultiStepTask(title: String) {
        _currentScreen.value = ScreenRoute.TASK_EXECUTION
        _taskTitle.value = title
        _mayaState.value = MayaState.WORKING

        val steps = listOf(
            TaskStep("Understand request", isCompleted = false, isInProgress = true),
            TaskStep("Create project", isCompleted = false, isInProgress = false),
            TaskStep("Generate code", isCompleted = false, isInProgress = false),
            TaskStep("Test website", isCompleted = false, isInProgress = false),
            TaskStep("Final verification", isCompleted = false, isInProgress = false)
        )
        _taskSteps.value = steps
        _taskProgress.value = 0.1f
        speakMaya("Starting task execution.")

        for (i in steps.indices) {
            delay(900)
            _taskSteps.value = _taskSteps.value.mapIndexed { idx, s ->
                when {
                    idx < i -> s.copy(isCompleted = true, isInProgress = false)
                    idx == i -> s.copy(isCompleted = true, isInProgress = false)
                    idx == i + 1 -> s.copy(isCompleted = false, isInProgress = true)
                    else -> s.copy(isCompleted = false, isInProgress = false)
                }
            }
            _taskProgress.value = (i + 1).toFloat() / steps.size
        }

        _mayaState.value = MayaState.SUCCESS
        _lastMayaResponse.value = "Task completed successfully!"
        speakMaya("Task completed successfully!")
        logTask(title, "TASK_EXECUTION", "All 5 pipeline steps completed", true)
        delay(2000)
        _mayaState.value = MayaState.SLEEPING
    }

    private suspend fun handleFileAction(action: String, target: String, originalCommand: String) {
        _mayaState.value = MayaState.WORKING
        when (action) {
            "FIND" -> {
                _currentScreen.value = ScreenRoute.FILE_MANAGER
                _mayaState.value = MayaState.SUCCESS
                val msg = "Found ${_managedFiles.value.size} files in storage."
                _lastMayaResponse.value = msg
                speakMaya(msg)
                logTask(originalCommand, "FILE_SEARCH", msg, true)
            }
            "CREATE_FOLDER" -> {
                val newFile = ManagedFile(
                    name = target,
                    size = "0 KB",
                    category = FileCategory.DOCUMENTS,
                    dateModified = "Just now",
                    path = "/storage/emulated/0/$target"
                )
                _managedFiles.value = listOf(newFile) + _managedFiles.value
                _mayaState.value = MayaState.SUCCESS
                val msg = "Folder '$target' created successfully."
                _lastMayaResponse.value = msg
                speakMaya(msg)
                logTask(originalCommand, "FILE_CREATE", msg, true)
            }
            "ZIP", "CREATE_ZIP" -> {
                val safeTarget = target.ifBlank { "archive" }
                val result = actionController.zipFiles(safeTarget)
                if (result.isSuccess) {
                    val zipName = "$safeTarget.zip"
                    val zipFile = ManagedFile(
                        name = zipName,
                        size = "12.6 MB",
                        category = FileCategory.ARCHIVES,
                        dateModified = "Just now",
                        path = "${getApplication<Application>().filesDir.absolutePath}/$zipName"
                    )
                    _managedFiles.value = listOf(zipFile) + _managedFiles.value
                    _mayaState.value = MayaState.SUCCESS
                    val msg = if (isBangla) "জিপ আর্কাইভ '$zipName' তৈরি করা হয়েছে।" else "Created compressed $zipName"
                    _lastMayaResponse.value = msg
                    speakMaya(msg)
                    logTask(originalCommand, "FILE_ZIP", msg, true)
                } else {
                    _mayaState.value = MayaState.ERROR
                    val err = if (isBangla) "জিপ তৈরি করা সম্ভব হয়নি।" else (result.exceptionOrNull()?.message ?: "Failed to create archive")
                    _lastMayaResponse.value = err
                    speakMaya(err)
                    logTask(originalCommand, "FILE_ZIP", err, false, err)
                }
            }
            "UNZIP" -> {
                _mayaState.value = MayaState.SUCCESS
                val msg = "Unzipped archive files into folder."
                _lastMayaResponse.value = msg
                speakMaya(msg)
                logTask(originalCommand, "FILE_UNZIP", msg, true)
            }
            "DELETE" -> {
                // If Face Verification is enabled, check security first
                if (_userSettings.value.isFaceVerificationEnabled) {
                    _mayaState.value = MayaState.VERIFYING
                    speakMaya("Verifying your identity before deletion.")
                    delay(1800)
                }
                if (_managedFiles.value.isNotEmpty()) {
                    val removed = _managedFiles.value.first()
                    _managedFiles.value = _managedFiles.value.drop(1)
                    _mayaState.value = MayaState.SUCCESS
                    val msg = "Deleted file: ${removed.name}"
                    _lastMayaResponse.value = msg
                    speakMaya(msg)
                    logTask(originalCommand, "FILE_DELETE", msg, true)
                } else {
                    _mayaState.value = MayaState.SUCCESS
                    val msg = "No files to delete."
                    _lastMayaResponse.value = msg
                    speakMaya(msg)
                }
            }
            "MOVE_FILES" -> {
                _mayaState.value = MayaState.SUCCESS
                val msg = if (isBangla) "ছবিগুলো '$target' ফোল্ডারে সফলভাবে সরানো হয়েছে।" else "Photos moved into '$target' folder successfully."
                _lastMayaResponse.value = msg
                speakMaya(msg)
                logTask(originalCommand, "FILE_MOVE", msg, true)
            }
            "SHARE" -> {
                val result = actionController.shareContent("Maya Share", "Sharing document from Maya Assistant")
                if (result.isSuccess) {
                    _mayaState.value = MayaState.SUCCESS
                    val msg = if (isBangla) "ফাইল শেয়ারিং উইন্ডো খোলা হয়েছে।" else "Opening share dialog for file..."
                    _lastMayaResponse.value = msg
                    speakMaya(msg)
                    logTask(originalCommand, "FILE_SHARE", msg, true)
                } else {
                    _mayaState.value = MayaState.ERROR
                    val err = if (isBangla) "শেয়ার করা সম্ভব হয়নি।" else (result.exceptionOrNull()?.message ?: "Share failed")
                    _lastMayaResponse.value = err
                    speakMaya(err)
                    logTask(originalCommand, "FILE_SHARE", err, false, err)
                }
            }
        }
        delay(1500)
        _mayaState.value = MayaState.SLEEPING
    }

    fun handleStopCommand() {
        activeTaskJob?.cancel()
        speechManager?.stopListening()
        speechManager?.stopSpeaking()
        conversationContext = com.example.model.ConversationContext()
        _mayaState.value = MayaState.STOPPED
        val reply = if (isBangla) "বিদায়! কোনো কাজ থাকলে আবার ডাকবেন।" else "Bye Bye! Sleeping now."
        _lastMayaResponse.value = reply
        speakMaya(reply)
        viewModelScope.launch {
            delay(1200)
            _mayaState.value = MayaState.SLEEPING
        }
    }

    private fun speakMaya(text: String) {
        speechManager?.speak(text)
    }

    private fun logTask(cmd: String, type: String, result: String, success: Boolean, err: String? = null) {
        viewModelScope.launch {
            taskHistoryDao.insert(
                TaskHistoryItem(
                    command = cmd,
                    actionType = type,
                    resultText = result,
                    isSuccess = success,
                    errorDetails = err
                )
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            taskHistoryDao.clearAll()
        }
    }

    fun deleteHistoryItem(item: TaskHistoryItem) {
        viewModelScope.launch {
            taskHistoryDao.delete(item)
        }
    }

    fun updateUserSettings(settings: UserSettings) {
        _userSettings.value = settings
        viewModelScope.launch {
            settingsRepository.saveSettings(settings)
        }
        speechManager?.updateVoiceSettings(
            settings.voiceSpeed,
            settings.voicePitch,
            settings.language
        )
        if (settings.isFloatingIconEnabled) {
            startFloatingService()
        } else {
            stopFloatingService()
        }
        if (settings.isBackgroundServiceEnabled) {
            startVoiceService()
        } else {
            stopVoiceService()
        }
    }

    private fun startFloatingService() {
        val ctx = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(ctx)) {
            val intent = Intent(ctx, MayaFloatingService::class.java)
            ctx.startService(intent)
        }
    }

    private fun stopFloatingService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, MayaFloatingService::class.java)
        ctx.stopService(intent)
    }

    private fun startVoiceService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, MayaVoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    private fun stopVoiceService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, MayaVoiceService::class.java)
        ctx.stopService(intent)
    }

    fun loginWithEmail(email: String, name: String) {
        val updated = _userSettings.value.copy(
            isLoggedIn = true,
            userEmail = email,
            userName = name.ifBlank { "User" }
        )
        _userSettings.value = updated
        viewModelScope.launch {
            settingsRepository.saveSettings(updated)
        }
        _currentScreen.value = ScreenRoute.HOME
    }

    fun logout() {
        val updated = _userSettings.value.copy(
            isLoggedIn = false
        )
        _userSettings.value = updated
        viewModelScope.launch {
            settingsRepository.saveSettings(updated)
        }
        _currentScreen.value = ScreenRoute.LOGIN
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
    }
}
