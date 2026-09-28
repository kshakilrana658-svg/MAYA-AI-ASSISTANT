package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Service class that uses Android's SpeechRecognizer API to capture microphone audio
 * and process it into transcribed text with real-time audio amplitude monitoring.
 */
class SpeechToTextManager(
    private val context: Context,
    private val onResult: ((String) -> Unit)? = null,
    private val onPartialResult: ((String) -> Unit)? = null,
    private val onRmsChanged: ((Float) -> Unit)? = null,
    private val onError: ((String) -> Unit)? = null
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val err = "Speech recognition is not available on this device"
            _errorMessage.value = err
            onError?.invoke(err)
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _errorMessage.value = null
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize standard SpeechRecognizer -2dB..10dB into 0f..1f range
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                    _audioRms.value = normalized
                    onRmsChanged?.invoke(normalized)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _audioRms.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _audioRms.value = 0f
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error in speech recognition"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network connection issue"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition engine busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected within timeout"
                        else -> "Speech recognition paused"
                    }
                    _errorMessage.value = message
                    onError?.invoke(message)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _audioRms.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim() ?: ""
                    if (text.isNotBlank()) {
                        _transcribedText.value = text
                        onResult?.invoke(text)
                    } else {
                        val noMatchErr = "No speech detected"
                        _errorMessage.value = noMatchErr
                        onError?.invoke(noMatchErr)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partialText = matches?.firstOrNull()?.trim() ?: ""
                    if (partialText.isNotBlank()) {
                        _transcribedText.value = partialText
                        onPartialResult?.invoke(partialText)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    /**
     * Start capturing microphone audio and transcribing it to text.
     * @param languageCode BCP 47 language tag, e.g. "en-US", "bn-BD", "hi-IN", "ar"
     */
    fun startListening(languageCode: String = Locale.getDefault().toLanguageTag()) {
        if (speechRecognizer == null) {
            initializeRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            _errorMessage.value = null
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
            val err = "Could not start microphone listening: ${e.message}"
            _errorMessage.value = err
            onError?.invoke(err)
        }
    }

    /**
     * Stop capturing audio and finish processing current utterance.
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
            _audioRms.value = 0f
        } catch (_: Exception) {}
    }

    /**
     * Cancel ongoing recognition and discard recorded buffer.
     */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _isListening.value = false
            _audioRms.value = 0f
        } catch (_: Exception) {}
    }

    /**
     * Destroy speech recognizer resources.
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            _isListening.value = false
            _audioRms.value = 0f
        } catch (_: Exception) {}
    }
}
