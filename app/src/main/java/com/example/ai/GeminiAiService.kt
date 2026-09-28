package com.example.ai

import com.example.BuildConfig
import com.example.model.ConversationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object GeminiConfig {
    const val MODEL_NAME = "gemini-2.5-flash"
    const val TIMEOUT_SECONDS = 60L
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
}

sealed class ParsedIntent {
    object StopTask : ParsedIntent()
    data class AskClarification(val question: String) : ParsedIntent()
    data class OpenApp(val appTarget: String) : ParsedIntent()
    data class SearchWeb(val query: String) : ParsedIntent()
    data class OpenWebsite(val url: String) : ParsedIntent()
    data class ChangeWakePhrase(val newPhrase: String) : ParsedIntent()
    data class ChangeStopCommand(val newCommand: String) : ParsedIntent()
    data class ChangeVoice(val voiceName: String) : ParsedIntent()
    data class ChangeLanguage(val language: String) : ParsedIntent()
    data class CreateProject(val title: String) : ParsedIntent()
    data class FileAction(val action: String, val target: String) : ParsedIntent()
    data class GeneralAnswer(val answer: String) : ParsedIntent()
}

data class ProcessResult(
    val intent: ParsedIntent,
    val responseMessage: String,
    val updatedContext: ConversationContext
)

/**
 * Controlled structured response schema returned by Gemini.
 * Directly maps to existing project intents (ParsedIntent) and ProcessResult.
 */
data class GeminiControlledOutput(
    val type: String = "CHAT", // "CHAT" or "ACTION"
    val intent: String? = null,
    val target: String? = null,
    val response: String
)

sealed class GeminiResult<out T> {
    data class Success<T>(val data: T) : GeminiResult<T>()
    data class Failure(
        val code: Int? = null,
        val errorType: GeminiErrorType,
        val message: String,
        val cause: Throwable? = null
    ) : GeminiResult<Nothing>()
}

enum class GeminiErrorType {
    BAD_REQUEST_400,
    UNAUTHORIZED_401,
    FORBIDDEN_403,
    RATE_LIMIT_429,
    SERVER_ERROR_5XX,
    NETWORK_ERROR,
    TIMEOUT,
    EMPTY_RESPONSE,
    UNKNOWN_ERROR
}

class GeminiAiService {

    companion object {
        const val MAYA_SYSTEM_INSTRUCTION = """You are Maya, an intelligent Android Voice AI Assistant. You assist users with spoken queries and voice guidance.

You MUST respond strictly with a valid JSON object matching this schema:
{
  "type": "CHAT" or "ACTION",
  "intent": null or one of the valid project intents,
  "target": null or target argument string,
  "response": "Brief spoken reply for user in natural language"
}

Allowed Intent Names for "intent" (NEVER invent new intents):
- "OPEN_APP": when user requests to open an app (target: app name, e.g. "Chrome", "YouTube", "Settings")
- "SEARCH_WEB": when user requests to search the web (target: query string)
- "OPEN_WEBSITE": when user requests to open a specific website URL (target: URL)
- "FILE_ACTION": when user requests file tasks like folder or zip (target: e.g. "CREATE_ZIP:backup" or "CREATE_FOLDER:FolderName")
- "CREATE_PROJECT": when user asks to generate a project (target: project title)
- "STOP_TASK": when user asks to stop, cancel, or quit
- "ASK_CLARIFICATION": when user request is ambiguous and needs clarification
- "CHANGE_LANGUAGE": when user asks to change language (target: "Bangla" or "English")
- "CHANGE_WAKE_PHRASE": when user asks to change wake phrase (target: new phrase)
- "CHANGE_STOP_COMMAND": when user asks to change stop command (target: new command)
- null: for general questions, conversation, knowledge, or explanations ("type": "CHAT")

Operational Rules:
1. Strict JSON: Output ONLY the raw JSON object. Never include markdown code fences or extra text.
2. Honesty: Never make up or hallucinate information. If unknown, state that you do not know.
3. No Sensor/Telemetry Hallucinations: Never invent weather forecasts, battery percentages, or device status.
4. No Direct Action Execution: You are a reasoning model; you do NOT execute Android actions yourself. Native Android code executes and verifies all actions. Never claim an action succeeded.
5. Brevity for Voice: Keep "response" concise, conversational, and direct (1 to 2 sentences max).
6. Language: If the user speaks in Bangla (বাংলা), write "response" in natural Bangla. If in English, write "response" in English."""

        val ALLOWED_TYPES = setOf("CHAT", "ACTION")

        val ALLOWED_INTENTS = setOf(
            "OPEN_APP",
            "SEARCH_WEB",
            "OPEN_WEBSITE",
            "FILE_ACTION",
            "CREATE_PROJECT",
            "STOP_TASK",
            "ASK_CLARIFICATION",
            "CHANGE_LANGUAGE",
            "CHANGE_WAKE_PHRASE",
            "CHANGE_STOP_COMMAND"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(GeminiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(GeminiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(GeminiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }

    fun isApiKeyConfigured(): Boolean {
        val key = apiKey
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "null"
    }

    /**
     * Send direct prompt to Gemini API with optional recent conversation context, timeout, and error handling.
     * API key is sent strictly via HTTP header "x-goog-api-key" (NEVER in URL).
     * API key is never exposed in logs, UI, or error messages.
     * Respects context boundaries: limited history, no credentials, works normally if context is absent.
     */
    suspend fun generateResponse(
        prompt: String,
        context: ConversationContext? = null
    ): GeminiResult<String> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext GeminiResult.Failure(
                code = 401,
                errorType = GeminiErrorType.UNAUTHORIZED_401,
                message = "Gemini API key is not configured. Please set GEMINI_API_KEY in the AI Studio Secrets panel."
            )
        }

        try {
            // Strictly use endpoint WITHOUT query parameter key
            val endpoint = "${GeminiConfig.BASE_URL}/models/${GeminiConfig.MODEL_NAME}:generateContent"

            val systemInstruction = MAYA_SYSTEM_INSTRUCTION
            val contextSection = formatRecentContext(context)
            val fullPromptText = if (contextSection.isNotBlank()) {
                "$systemInstruction$contextSection\n\nUser Question/Utterance: $prompt"
            } else {
                "$systemInstruction\n\nUser Question/Utterance: $prompt"
            }

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val partObj = JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", fullPromptText)
                            })
                        })
                    }
                    put(partObj)
                }
                put("contents", contents)

                // API-level structured JSON output configuration with safe output limit
                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("maxOutputTokens", 350)
                    put("temperature", 0.2)

                    val responseSchema = JSONObject().apply {
                        put("type", "OBJECT")
                        val properties = JSONObject().apply {
                            put("type", JSONObject().apply {
                                put("type", "STRING")
                                put("enum", JSONArray().apply {
                                    put("CHAT")
                                    put("ACTION")
                                })
                            })
                            put("intent", JSONObject().apply {
                                put("type", "STRING")
                                put("nullable", true)
                                put("enum", JSONArray().apply {
                                    put("OPEN_APP")
                                    put("SEARCH_WEB")
                                    put("OPEN_WEBSITE")
                                    put("FILE_ACTION")
                                    put("CREATE_PROJECT")
                                    put("STOP_TASK")
                                    put("ASK_CLARIFICATION")
                                    put("CHANGE_LANGUAGE")
                                    put("CHANGE_WAKE_PHRASE")
                                    put("CHANGE_STOP_COMMAND")
                                })
                            })
                            put("target", JSONObject().apply {
                                put("type", "STRING")
                                put("nullable", true)
                            })
                            put("response", JSONObject().apply {
                                put("type", "STRING")
                            })
                        }
                        put("properties", properties)
                        put("required", JSONArray().apply {
                            put("type")
                            put("response")
                        })
                    }
                    put("responseSchema", responseSchema)
                }
                put("generationConfig", generationConfig)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()

                when (val code = response.code) {
                    200 -> parseSuccessfulResponse(bodyString)
                    400 -> {
                        val detail = extractApiErrorMessage(bodyString)
                        GeminiResult.Failure(
                            code = 400,
                            errorType = GeminiErrorType.BAD_REQUEST_400,
                            message = "Bad Request (HTTP 400): $detail"
                        )
                    }
                    401 -> {
                        GeminiResult.Failure(
                            code = 401,
                            errorType = GeminiErrorType.UNAUTHORIZED_401,
                            message = "Unauthorized (HTTP 401): Invalid or missing Gemini API key. Please check your GEMINI_API_KEY in AI Studio Secrets."
                        )
                    }
                    403 -> {
                        val detail = extractApiErrorMessage(bodyString)
                        GeminiResult.Failure(
                            code = 403,
                            errorType = GeminiErrorType.FORBIDDEN_403,
                            message = "Forbidden (HTTP 403): API key does not have permission for '${GeminiConfig.MODEL_NAME}'. $detail"
                        )
                    }
                    429 -> {
                        val detail = extractApiErrorMessage(bodyString)
                        GeminiResult.Failure(
                            code = 429,
                            errorType = GeminiErrorType.RATE_LIMIT_429,
                            message = "Rate Limit Exceeded (HTTP 429): Quota limit reached for Gemini API. Please wait a moment. $detail"
                        )
                    }
                    in 500..599 -> {
                        GeminiResult.Failure(
                            code = code,
                            errorType = GeminiErrorType.SERVER_ERROR_5XX,
                            message = "Gemini Server Error (HTTP $code): Google generative AI services are temporarily unavailable. Please retry shortly."
                        )
                    }
                    else -> {
                        val detail = extractApiErrorMessage(bodyString)
                        GeminiResult.Failure(
                            code = code,
                            errorType = GeminiErrorType.UNKNOWN_ERROR,
                            message = "Gemini API request failed with HTTP $code: $detail"
                        )
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            GeminiResult.Failure(
                errorType = GeminiErrorType.TIMEOUT,
                message = "Gemini connection timed out. Please check your internet connection.",
                cause = e
            )
        } catch (e: IOException) {
            val safeMessage = sanitizeKey(e.message ?: "Network connection failed")
            GeminiResult.Failure(
                errorType = GeminiErrorType.NETWORK_ERROR,
                message = "Network error connecting to Gemini API: $safeMessage",
                cause = e
            )
        } catch (e: Exception) {
            val safeMessage = sanitizeKey(e.message ?: "Unexpected error")
            GeminiResult.Failure(
                errorType = GeminiErrorType.UNKNOWN_ERROR,
                message = "Unexpected error during Gemini execution: $safeMessage",
                cause = e
            )
        }
    }

    /**
     * Safely parse Gemini 200 HTTP response.
     * Prevents crashes by avoiding unsafe indexing like candidates[0] or parts[0].
     * Gracefully handles:
     * 1. Missing candidates
     * 2. Missing content
     * 3. Missing parts
     * 4. Missing/empty text
     * 5. Empty response body
     * 6. Malformed JSON
     * 7. Blocked responses (blockReason or safety finishReason)
     * 8. API error payloads
     */
    private fun parseSuccessfulResponse(bodyString: String?): GeminiResult<String> {
        if (bodyString.isNullOrBlank()) {
            return GeminiResult.Failure(
                code = 200,
                errorType = GeminiErrorType.EMPTY_RESPONSE,
                message = "Empty response: Gemini returned an empty body."
            )
        }

        val json = try {
            JSONObject(bodyString)
        } catch (e: Exception) {
            return GeminiResult.Failure(
                code = 200,
                errorType = GeminiErrorType.UNKNOWN_ERROR,
                message = "Malformed JSON received from Gemini API: ${e.message}",
                cause = e
            )
        }

        // 1. Check prompt-level block feedback
        val promptFeedback = json.optJSONObject("promptFeedback")
        val blockReason = promptFeedback?.optString("blockReason")
        if (!blockReason.isNullOrBlank()) {
            return GeminiResult.Failure(
                code = 200,
                errorType = GeminiErrorType.BAD_REQUEST_400,
                message = "Gemini blocked prompt execution: $blockReason"
            )
        }

        // 2. Check candidates array existence and length
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return GeminiResult.Failure(
                code = 200,
                errorType = GeminiErrorType.EMPTY_RESPONSE,
                message = "Empty response: No candidates found in Gemini output."
            )
        }

        var lastFinishReason: String? = null
        var isBlockedBySafety = false

        // 3. Iterate candidates safely without assuming index 0
        for (i in 0 until candidates.length()) {
            val candidate = candidates.optJSONObject(i) ?: continue

            val finishReason = candidate.optString("finishReason")
            if (finishReason.isNotBlank()) {
                lastFinishReason = finishReason
                if (finishReason.equals("SAFETY", ignoreCase = true) ||
                    finishReason.equals("BLOCKED", ignoreCase = true) ||
                    finishReason.equals("PROHIBITED_CONTENT", ignoreCase = true) ||
                    finishReason.equals("SPII", ignoreCase = true)
                ) {
                    isBlockedBySafety = true
                }
            }

            // Safe content extraction
            val content = candidate.optJSONObject("content") ?: continue

            // Safe parts extraction
            val parts = content.optJSONArray("parts") ?: continue
            if (parts.length() == 0) continue

            // Safe text extraction across all parts without assuming index 0
            val textBuilder = StringBuilder()
            for (j in 0 until parts.length()) {
                val part = parts.optJSONObject(j) ?: continue
                val text = part.optString("text", "")
                if (text.isNotBlank()) {
                    textBuilder.append(text)
                }
            }

            val extractedText = textBuilder.toString().trim()
            if (extractedText.isNotEmpty()) {
                return GeminiResult.Success(extractedText)
            }
        }

        // 4. If no text could be extracted from any candidate or part
        val failureMessage = when {
            isBlockedBySafety -> "Gemini response was blocked by safety policy (finishReason: $lastFinishReason)."
            !lastFinishReason.isNullOrBlank() -> "No valid text generated by Gemini (finishReason: $lastFinishReason)."
            else -> "Empty response: Candidate content or text parts were missing or blank."
        }

        return GeminiResult.Failure(
            code = 200,
            errorType = GeminiErrorType.EMPTY_RESPONSE,
            message = failureMessage
        )
    }

    private fun extractApiErrorMessage(bodyString: String?): String {
        if (bodyString.isNullOrBlank()) return "No response details provided."
        return try {
            val json = JSONObject(bodyString)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message")
            if (!message.isNullOrBlank()) {
                sanitizeKey(message)
            } else {
                "Unknown format in error payload."
            }
        } catch (_: Exception) {
            "Unable to parse API error details."
        }
    }

    private fun sanitizeKey(text: String): String {
        val currentKey = apiKey
        return if (currentKey.isNotBlank()) text.replace(currentKey, "[REDACTED]") else text
    }

    /**
     * Safely constructs a limited, non-sensitive conversation context block for Gemini.
     * Rules strictly obeyed:
     * 1. Limited recent items: only the last 3-4 utterances from recentUtterances.
     * 2. Never dumps entire or old conversation history.
     * 3. Sensitive data exclusion: strips potential PINs, passwords, and sensitive keys.
     * 4. Safe fallback: returns empty string if context is null or empty, allowing Gemini to operate normally.
     * 5. Android action execution: leaves all execution strictly to native Android code.
     */
    private fun formatRecentContext(context: ConversationContext?): String {
        if (context == null) return ""

        val contextParts = mutableListOf<String>()

        // 1. Referenced entities (app, topic, file)
        context.lastMentionedApp?.let { app ->
            val clean = sanitizeContextItem(app)
            if (clean.isNotBlank()) contextParts.add("- Referenced App: $clean")
        }
        context.lastTopic?.let { topic ->
            val clean = sanitizeContextItem(topic)
            if (clean.isNotBlank()) contextParts.add("- Active Topic: $clean")
        }
        context.lastMentionedFile?.let { file ->
            val clean = sanitizeContextItem(file)
            if (clean.isNotBlank()) contextParts.add("- Referenced File: $clean")
        }

        // 2. Only the last 3-4 clean utterances (strictly limited to prevent history bloat)
        val recent = context.recentUtterances
            .takeLast(4)
            .mapNotNull { utterance ->
                val clean = sanitizeContextItem(utterance)
                if (clean.isNotBlank()) clean else null
            }

        if (recent.isNotEmpty()) {
            contextParts.add("- Recent dialogue: " + recent.joinToString(" -> ") { "\"$it\"" })
        }

        if (contextParts.isEmpty()) return ""

        return "\n\n[Recent Conversation Context]\n" + contextParts.joinToString("\n")
    }

    private fun sanitizeContextItem(text: String): String {
        // Redact potential PINs/passwords (sequences of 4 to 8 digits)
        var sanitized = text.replace(Regex("""\b\d{4,8}\b"""), "[REDACTED_NUM]")
        // Redact any occurrences of API key
        sanitized = sanitizeKey(sanitized)
        return sanitized.trim().take(120)
    }

    /**
     * Parse natural language command into typed ParsedIntent.
     * Uses fast on-device rules for system commands (launch, search, files, settings),
     * and queries Gemini for conversational reasoning or semantic understanding.
     */
    suspend fun processCommand(
        prompt: String,
        wakePhrase: String,
        stopCommand: String,
        context: ConversationContext,
        isBangla: Boolean = false
    ): ProcessResult = withContext(Dispatchers.IO) {
        val trimmed = prompt.trim()
        val wake = wakePhrase.trim().lowercase()
        val cleanPrompt = when {
            wake.isNotEmpty() && trimmed.lowercase().startsWith(wake) -> {
                trimmed.substring(wake.length).trim().removePrefix(",").trim()
            }
            trimmed.lowercase().startsWith("hey maya") -> {
                trimmed.substring(8).trim().removePrefix(",").trim()
            }
            trimmed.lowercase().startsWith("maya") -> {
                trimmed.substring(4).trim().removePrefix(",").trim()
            }
            trimmed.lowercase().startsWith("মায়া") -> {
                trimmed.substring(4).trim().removePrefix(",").trim()
            }
            else -> trimmed
        }
        val lower = cleanPrompt.lowercase()

        // 1. Direct Stop / Barge-in commands
        if (lower == stopCommand.lowercase() ||
            lower == "stop" || lower == "থামো" || lower == "বন্ধ করো" || lower == "বিদায়"
        ) {
            val reply = if (isBangla) "ঠিক আছে, আমি থামছি। দরকার হলে আবার বলুন \"${wakePhrase}\"।" else "Task stopped. Call me again anytime."
            return@withContext ProcessResult(
                intent = ParsedIntent.StopTask,
                responseMessage = reply,
                updatedContext = context.copy(
                    awaitingClarificationFor = null,
                    recentUtterances = (context.recentUtterances + trimmed).takeLast(4)
                )
            )
        }

        // 2. Settings modification commands (Wake phrase, Stop command, Language)
        if (lower.contains("change wake phrase to") || lower.contains("ওয়েক ফ্রেজ পরিবর্তন")) {
            val newPhrase = cleanPrompt.substringAfter("to").trim().ifBlank { "Hey Maya" }
            return@withContext ProcessResult(
                intent = ParsedIntent.ChangeWakePhrase(newPhrase),
                responseMessage = if (isBangla) "ওয়েক ফ্রেজ \"$newPhrase\" এ পরিবর্তন করা হয়েছে।" else "Wake phrase updated to \"$newPhrase\"",
                updatedContext = context.copy(recentUtterances = (context.recentUtterances + trimmed).takeLast(4))
            )
        }

        if (lower.contains("change stop command to") || lower.contains("থামার কমান্ড পরিবর্তন")) {
            val newCmd = cleanPrompt.substringAfter("to").trim().ifBlank { "Bye Bye" }
            return@withContext ProcessResult(
                intent = ParsedIntent.ChangeStopCommand(newCmd),
                responseMessage = if (isBangla) "স্টপ কমান্ড \"$newCmd\" এ পরিবর্তন করা হয়েছে।" else "Stop command updated to \"$newCmd\"",
                updatedContext = context.copy(recentUtterances = (context.recentUtterances + trimmed).takeLast(4))
            )
        }

        if (lower.contains("change language to") || lower.contains("ভাষা পরিবর্তন")) {
            val lang = if (lower.contains("bangla") || lower.contains("বাংলা")) "Bangla" else "English"
            return@withContext ProcessResult(
                intent = ParsedIntent.ChangeLanguage(lang),
                responseMessage = if (lang == "Bangla") "ভাষা বাংলায় পরিবর্তন করা হয়েছে।" else "Language changed to English.",
                updatedContext = context.copy(recentUtterances = (context.recentUtterances + trimmed).takeLast(4))
            )
        }

        // 3. Application Launch
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.contains("খুলুন") || lower.contains("খোলো")) {
            val app = extractAppName(cleanPrompt)
            if (app.isNotBlank()) {
                val reply = if (isBangla) "$app ওপেন করছি..." else "Opening $app..."
                return@withContext ProcessResult(
                    intent = ParsedIntent.OpenApp(app),
                    responseMessage = reply,
                    updatedContext = context.copy(
                        lastMentionedApp = app,
                        recentUtterances = (context.recentUtterances + trimmed).takeLast(4)
                    )
                )
            }
        }

        // 4. Multi-step Project Pipeline
        if (lower.contains("create website") || lower.contains("make website") ||
            lower.contains("ওয়েবসাইট তৈরি করো") || lower.contains("ওয়েবসাইট বানাও") || lower.contains("প্রজেক্ট তৈরি")
        ) {
            val reply = if (isBangla) "ওয়েবসাইট তৈরির প্রজেক্ট শুরু করছি..." else "Starting website generation pipeline..."
            return@withContext ProcessResult(
                intent = ParsedIntent.CreateProject("Personal Portfolio Website"),
                responseMessage = reply,
                updatedContext = context.copy(
                    lastTopic = "Personal Portfolio Website",
                    recentUtterances = (context.recentUtterances + trimmed).takeLast(4)
                )
            )
        }

        // 5. Web & YouTube searches
        if (lower.startsWith("search youtube for ") || lower.contains("ইউটিউবে সার্চ করো")) {
            val query = cleanPrompt.removePrefix("search youtube for ").replace("ইউটিউবে সার্চ করো", "").trim()
            return@withContext ProcessResult(
                intent = ParsedIntent.OpenWebsite("https://www.youtube.com/results?search_query=${query.replace(" ", "+")}"),
                responseMessage = if (isBangla) "ইউটিউবে \"$query\" অনুসন্ধান করছি..." else "Searching YouTube for: $query",
                updatedContext = context.copy(
                    lastTopic = query,
                    recentUtterances = (context.recentUtterances + trimmed).takeLast(4)
                )
            )
        }

        if (lower.startsWith("search for ") || lower.startsWith("search ") || lower.contains("সার্চ করো")) {
            val query = cleanPrompt.removePrefix("search for ").removePrefix("search ").replace("সার্চ করো", "").trim()
            return@withContext ProcessResult(
                intent = ParsedIntent.SearchWeb(query),
                responseMessage = if (isBangla) "ওয়েবে \"$query\" অনুসন্ধান করছি..." else "Searching web for: $query",
                updatedContext = context.copy(
                    lastTopic = query,
                    recentUtterances = (context.recentUtterances + trimmed).takeLast(4)
                )
            )
        }

        // 6. File operations: ZIP, Folder
        if (lower.contains("zip") || lower.contains("compress") || lower.contains("জিপ করো")) {
            return@withContext ProcessResult(
                intent = ParsedIntent.FileAction("CREATE_ZIP", "maya_backup"),
                responseMessage = if (isBangla) "ফাইলগুলো দিয়ে জিপ আর্কাইভ তৈরি করছি..." else "Creating ZIP archive...",
                updatedContext = context.copy(
                    lastMentionedFile = "maya_backup.zip",
                    recentUtterances = (context.recentUtterances + trimmed).takeLast(4)
                )
            )
        }

        if (lower.contains("create folder") || lower.contains("নতুন ফোল্ডার") || lower.contains("ফোল্ডার বানাও")) {
            val folderName = cleanPrompt.substringAfter("folder", "").substringAfter("ফোল্ডার", "")
                .trim().ifBlank { "New_Folder" }
            return@withContext ProcessResult(
                intent = ParsedIntent.FileAction("CREATE_FOLDER", folderName),
                responseMessage = if (isBangla) "\"$folderName\" ফোল্ডার তৈরি করছি..." else "Creating folder: $folderName",
                updatedContext = context.copy(
                    lastCreatedFolder = folderName,
                    recentUtterances = (context.recentUtterances + trimmed).takeLast(4)
                )
            )
        }

        // 7. Conversational intelligence via Gemini API with context and clean local fallback
        when (val geminiResult = generateResponse(cleanPrompt, context)) {
            is GeminiResult.Success -> {
                val controlled = parseControlledOutput(geminiResult.data)
                val (parsedIntent, responseText, updatedContext) = mapControlledToIntent(
                    controlled = controlled,
                    context = context,
                    userUtterance = trimmed,
                    isBangla = isBangla
                )
                ProcessResult(
                    intent = parsedIntent,
                    responseMessage = responseText,
                    updatedContext = updatedContext
                )
            }
            is GeminiResult.Failure -> {
                // If local fallback can handle query, provide it
                val fallbackAnswer = getLocalFallbackAnswer(lower, isBangla)
                if (fallbackAnswer != null) {
                    ProcessResult(
                        intent = ParsedIntent.GeneralAnswer(fallbackAnswer),
                        responseMessage = fallbackAnswer,
                        updatedContext = context.copy(recentUtterances = (context.recentUtterances + trimmed).takeLast(4))
                    )
                } else {
                    // Clearly state why Gemini failed for debugging
                    val errorExplanation = if (!isApiKeyConfigured()) {
                        if (isBangla) {
                            "জেমিনি এআই সার্ভিস কনফিগার করা নেই। অনুগ্রহ করে AI Studio Secrets প্যানেলে GEMINI_API_KEY সেট করুন।"
                        } else {
                            "Gemini AI is not configured. Please set GEMINI_API_KEY in the AI Studio Secrets panel."
                        }
                    } else {
                        if (isBangla) {
                            "এআই সার্ভিস সমস্যা: ${geminiResult.message}"
                        } else {
                            "AI service error: ${geminiResult.message}"
                        }
                    }
                    ProcessResult(
                        intent = ParsedIntent.GeneralAnswer(errorExplanation),
                        responseMessage = errorExplanation,
                        updatedContext = context.copy(recentUtterances = (context.recentUtterances + trimmed).takeLast(4))
                    )
                }
            }
        }
    }

    private fun getLocalFallbackAnswer(lower: String, isBangla: Boolean): String? {
        if (lower.contains("time") || lower.contains("কয়টা বাজে") || lower.contains("সময় কত")) {
            val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            val currentTime = timeFormat.format(java.util.Date())
            return if (isBangla) "এখন সময় $currentTime" else "The current time is $currentTime"
        }
        if (lower.contains("date") || lower.contains("তারিখ কত") || lower.contains("আজকে কত তারিখ")) {
            val dateFormat = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.getDefault())
            val currentDate = dateFormat.format(java.util.Date())
            return if (isBangla) "আজকের তারিখ $currentDate" else "Today is $currentDate"
        }
        if (lower.contains("who are you") || lower.contains("তোমার নাম কি") || lower.contains("তুমি কে")) {
            return if (isBangla) "আমি মায়া, আপনার ব্যক্তিগত ভয়েস এআই অ্যাসিস্ট্যান্ট।"
            else "I am Maya, your personal voice AI assistant."
        }
        if (lower.contains("how are you") || lower.contains("কেমন আছো") || lower.contains("কী অবস্থা")) {
            return if (isBangla) "আমি ভালো আছি! আপনাকে কীভাবে সাহায্য করতে পারি?"
            else "I am doing great! How can I help you today?"
        }
        return null
    }

    private fun extractAppName(text: String): String {
        return when {
            text.contains("chrome") || text.contains("ক্রোম") -> "Chrome"
            text.contains("youtube") || text.contains("ইউটিউব") -> "YouTube"
            text.contains("whatsapp") || text.contains("হোয়াটসঅ্যাপ") -> "WhatsApp"
            text.contains("settings") || text.contains("সেটিংস") -> "Settings"
            text.contains("camera") || text.contains("ক্যামেরা") -> "Camera"
            text.contains("gallery") || text.contains("photo") || text.contains("গ্যালারি") -> "Gallery"
            text.contains("maps") || text.contains("ম্যাপ") -> "Maps"
            text.contains("gmail") || text.contains("ইমেইল") -> "Gmail"
            text.contains("play store") || text.contains("প্লে স্টোর") -> "Play Store"
            text.contains("calculator") || text.contains("ক্যালকুলেটর") -> "Calculator"
            text.contains("clock") || text.contains("alarm") || text.contains("ঘড়ি") -> "Clock"
            text.contains("files") || text.contains("ফাইলস") -> "Files"
            else -> {
                text.removePrefix("open ").removePrefix("launch ")
                    .removePrefix("খোলো ").removePrefix("খুলুন ").trim()
                    .replaceFirstChar { it.uppercase() }
            }
        }
    }

    /**
     * Safely constructs a fallback GeminiControlledOutput of type CHAT.
     * Guaranteed crash-safe, non-null, and non-blank response.
     */
    fun safeChatFallback(fallbackText: String? = null): GeminiControlledOutput {
        val clean = fallbackText?.trim()?.takeIf { it.isNotBlank() } ?: "ঠিক আছে।"
        return GeminiControlledOutput(
            type = "CHAT",
            intent = null,
            target = null,
            response = clean
        )
    }

    /**
     * Safely parse Gemini's controlled output into GeminiControlledOutput with strict validation:
     * 1. Allowed type: strictly "CHAT" or "ACTION" (case-sensitive). Invalid -> safe CHAT fallback.
     * 2. Allowed intent: only the 10 project-allowed intents. For ACTION, missing/invalid intent -> safe CHAT fallback.
     * 3. Response: must be non-null and non-blank. Missing/blank -> safe CHAT fallback.
     * 4. Target: Parser NEVER invents or defaults targets (no default URLs, apps, or folders).
     * 5. Crash-proof: malformed JSON, empty input, or unquoted text safely downgraded to CHAT.
     */
    fun parseControlledOutput(raw: String): GeminiControlledOutput {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return safeChatFallback()
        }

        try {
            // Strip markdown code fences if present
            val cleaned = if (trimmed.contains("```")) {
                val inner = trimmed.substringAfter("```json")
                    .substringAfter("```")
                inner.substringBeforeLast("```").trim()
            } else {
                trimmed
            }

            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            if (start == -1 || end <= start) {
                return safeChatFallback(cleaned)
            }

            val jsonStr = cleaned.substring(start, end + 1)

            // Robust string field extractor (works across Android runtime and host JVM environments)
            fun extractStringField(key: String): String? {
                val regex = Regex("\"$key\"\\s*:\\s*(?:\"((?:\\\\\"|[^\"])*)\"|null|([^,}\\s]+))")
                val match = regex.find(jsonStr) ?: return null
                val stringVal = match.groupValues[1]
                val rawVal = match.groupValues[2]
                return when {
                    stringVal.isNotEmpty() -> stringVal.replace("\\\"", "\"").replace("\\n", "\n")
                    rawVal.equals("null", ignoreCase = true) -> null
                    rawVal.isNotEmpty() -> rawVal
                    else -> null
                }
            }

            var rawType: String? = null
            var rawIntent: String? = null
            var rawTarget: String? = null
            var rawResponse: String? = null

            // First attempt native JSONObject (standard Android runtime)
            try {
                val json = JSONObject(jsonStr)
                if (json.has("type") && !json.isNull("type")) {
                    val t = json.optString("type")
                    if (t.isNotBlank()) rawType = t
                }
                if (json.has("intent") && !json.isNull("intent")) {
                    val i = json.optString("intent")
                    if (i.isNotBlank() && !i.equals("null", ignoreCase = true)) rawIntent = i
                }
                if (json.has("target") && !json.isNull("target")) {
                    val tg = json.optString("target")
                    if (tg.isNotBlank() && !tg.equals("null", ignoreCase = true)) rawTarget = tg
                }
                if (json.has("response") && !json.isNull("response")) {
                    val r = json.optString("response")
                    if (r.isNotBlank()) rawResponse = r
                }
            } catch (e: org.json.JSONException) {
                // Malformed JSON inside brackets on Android runtime -> safe fallback
                return safeChatFallback(cleaned)
            } catch (_: Throwable) {
                // Host JVM stub fallback or unexpected error
            }

            // Fallback to regex extractor if JSONObject was empty/stubbed
            if (rawType.isNullOrBlank()) {
                rawType = extractStringField("type")
            }
            if (rawResponse.isNullOrBlank()) {
                rawResponse = extractStringField("response")
            }
            if (rawIntent == null) {
                rawIntent = extractStringField("intent")
            }
            if (rawTarget == null) {
                rawTarget = extractStringField("target")
            }

            // 1. Validate response: must be non-null and non-blank
            val finalResponse = rawResponse?.trim()?.takeIf { it.isNotBlank() }
            if (finalResponse == null) {
                return safeChatFallback()
            }

            // 2. Validate type: strictly "CHAT" or "ACTION" (case-sensitive)
            val cleanType = rawType?.trim()
            if (cleanType == null || cleanType !in ALLOWED_TYPES) {
                return safeChatFallback(finalResponse)
            }

            // 3. Validate ACTION vs CHAT
            if (cleanType == "ACTION") {
                val cleanIntent = rawIntent?.trim()
                // ACTION requires a valid, whitelisted intent. Missing, blank, or invalid -> downgrade to CHAT
                if (cleanIntent.isNullOrBlank() || cleanIntent !in ALLOWED_INTENTS) {
                    return safeChatFallback(finalResponse)
                }

                // Never invent or default targets: preserve exact target if non-blank, otherwise null
                val cleanTarget = rawTarget?.trim()?.takeIf {
                    it.isNotBlank() && !it.equals("null", ignoreCase = true)
                }

                return GeminiControlledOutput(
                    type = "ACTION",
                    intent = cleanIntent,
                    target = cleanTarget,
                    response = finalResponse
                )
            } else {
                // Type is CHAT: intent must be allowed or null; target never executes action
                val cleanIntent = rawIntent?.trim()?.takeIf {
                    it.isNotBlank() && !it.equals("null", ignoreCase = true) && it in ALLOWED_INTENTS
                }
                val cleanTarget = rawTarget?.trim()?.takeIf {
                    it.isNotBlank() && !it.equals("null", ignoreCase = true)
                }

                return GeminiControlledOutput(
                    type = "CHAT",
                    intent = cleanIntent,
                    target = cleanTarget,
                    response = finalResponse
                )
            }
        } catch (_: Exception) {
            // Crash-proof: never throws an exception out of parsing
            return safeChatFallback(trimmed)
        }
    }

    /**
     * Strictly maps GeminiControlledOutput to existing project ParsedIntent.
     * Operational guarantees:
     * 1. Only ACTION type with a whitelisted project intent can trigger an action.
     * 2. Gemini cannot invent new intents; unrecognized intents degrade safely to GeneralAnswer.
     * 3. Arbitrary Gemini text never executes Android actions.
     * 4. Actual execution and success verification are strictly performed by native Android code.
     */
    fun mapControlledToIntent(
        controlled: GeminiControlledOutput,
        context: ConversationContext,
        userUtterance: String,
        isBangla: Boolean
    ): Triple<ParsedIntent, String, ConversationContext> {
        val updatedRecent = (context.recentUtterances + userUtterance).takeLast(4)

        // Strict rule: arbitrary text or non-ACTION never executes Android action
        if (controlled.type != "ACTION" || controlled.intent.isNullOrBlank()) {
            return Triple(
                ParsedIntent.GeneralAnswer(controlled.response),
                controlled.response,
                context.copy(recentUtterances = updatedRecent)
            )
        }

        return when (controlled.intent.trim().uppercase()) {
            "OPEN_APP" -> {
                val app = controlled.target?.takeIf { it.isNotBlank() }
                if (app != null) {
                    Triple(
                        ParsedIntent.OpenApp(app),
                        controlled.response,
                        context.copy(lastMentionedApp = app, recentUtterances = updatedRecent)
                    )
                } else {
                    Triple(
                        ParsedIntent.GeneralAnswer(controlled.response),
                        controlled.response,
                        context.copy(recentUtterances = updatedRecent)
                    )
                }
            }

            "SEARCH_WEB" -> {
                val query = controlled.target?.takeIf { it.isNotBlank() } ?: userUtterance
                Triple(
                    ParsedIntent.SearchWeb(query),
                    controlled.response,
                    context.copy(lastTopic = query, recentUtterances = updatedRecent)
                )
            }

            "OPEN_WEBSITE" -> {
                val url = controlled.target?.takeIf { it.isNotBlank() } ?: "https://www.google.com"
                Triple(
                    ParsedIntent.OpenWebsite(url),
                    controlled.response,
                    context.copy(recentUtterances = updatedRecent)
                )
            }

            "FILE_ACTION" -> {
                val rawTarget = controlled.target.orEmpty()
                val actionType = if (rawTarget.contains(":")) rawTarget.substringBefore(":") else "CREATE_FOLDER"
                val targetName = if (rawTarget.contains(":")) rawTarget.substringAfter(":") else rawTarget.ifBlank { "New_Folder" }
                Triple(
                    ParsedIntent.FileAction(actionType, targetName),
                    controlled.response,
                    context.copy(
                        lastCreatedFolder = if (actionType == "CREATE_FOLDER") targetName else context.lastCreatedFolder,
                        lastMentionedFile = if (actionType == "CREATE_ZIP") "$targetName.zip" else context.lastMentionedFile,
                        recentUtterances = updatedRecent
                    )
                )
            }

            "CREATE_PROJECT" -> {
                val title = controlled.target?.takeIf { it.isNotBlank() } ?: "Personal Portfolio Website"
                Triple(
                    ParsedIntent.CreateProject(title),
                    controlled.response,
                    context.copy(lastTopic = title, recentUtterances = updatedRecent)
                )
            }

            "STOP_TASK" -> {
                Triple(
                    ParsedIntent.StopTask,
                    controlled.response,
                    context.copy(awaitingClarificationFor = null, recentUtterances = updatedRecent)
                )
            }

            "ASK_CLARIFICATION" -> {
                Triple(
                    ParsedIntent.AskClarification(controlled.response),
                    controlled.response,
                    context.copy(recentUtterances = updatedRecent)
                )
            }

            "CHANGE_LANGUAGE" -> {
                val lang = if (controlled.target?.contains("bangla", ignoreCase = true) == true ||
                    controlled.target?.contains("বাংলা", ignoreCase = true) == true) "Bangla" else "English"
                Triple(
                    ParsedIntent.ChangeLanguage(lang),
                    controlled.response,
                    context.copy(recentUtterances = updatedRecent)
                )
            }

            "CHANGE_WAKE_PHRASE" -> {
                val phrase = controlled.target?.takeIf { it.isNotBlank() } ?: "Hey Maya"
                Triple(
                    ParsedIntent.ChangeWakePhrase(phrase),
                    controlled.response,
                    context.copy(recentUtterances = updatedRecent)
                )
            }

            "CHANGE_STOP_COMMAND" -> {
                val cmd = controlled.target?.takeIf { it.isNotBlank() } ?: "Bye Bye"
                Triple(
                    ParsedIntent.ChangeStopCommand(cmd),
                    controlled.response,
                    context.copy(recentUtterances = updatedRecent)
                )
            }

            // Unknown or invented intent: reject action execution and treat safely as GeneralAnswer
            else -> {
                Triple(
                    ParsedIntent.GeneralAnswer(controlled.response),
                    controlled.response,
                    context.copy(recentUtterances = updatedRecent)
                )
            }
        }
    }
}
