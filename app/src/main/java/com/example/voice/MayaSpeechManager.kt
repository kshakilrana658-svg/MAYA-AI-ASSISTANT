package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class MayaSpeechManager(
    private val context: Context,
    private val onVoiceResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onError: (String) -> Unit
) {
    private val speechToTextManager = SpeechToTextManager(
        context = context,
        onResult = onVoiceResult,
        onRmsChanged = onRmsChanged,
        onError = onError
    )

    private var textToSpeech: TextToSpeech? = null

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    val isListening: StateFlow<Boolean> = speechToTextManager.isListening

    private var currentRate: Float = 0.85f
    private var currentPitch: Float = 1.05f

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(currentRate)
                textToSpeech?.setPitch(currentPitch)
                _isTtsReady.value = true
            }
        }
    }

    fun updateVoiceSettings(speed: Float, pitch: Float, language: String) {
        currentRate = speed
        currentPitch = pitch
        textToSpeech?.setSpeechRate(speed)
        textToSpeech?.setPitch(pitch)
        when (language.lowercase()) {
            "bangla" -> textToSpeech?.language = Locale.forLanguageTag("bn-BD")
            "hindi" -> textToSpeech?.language = Locale.forLanguageTag("hi-IN")
            "arabic" -> textToSpeech?.language = Locale.forLanguageTag("ar")
            else -> textToSpeech?.language = Locale.US
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!_isTtsReady.value || text.isBlank()) {
            onDone?.invoke()
            return
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onDone?.invoke()
            }
            override fun onError(utteranceId: String?) {
                onDone?.invoke()
            }
        })

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MAYA_UTTERANCE_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    fun startListening(languageCode: String = "en-US") {
        speechToTextManager.startListening(languageCode)
    }

    fun stopListening() {
        speechToTextManager.stopListening()
    }

    fun destroy() {
        try {
            speechToTextManager.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }
}
