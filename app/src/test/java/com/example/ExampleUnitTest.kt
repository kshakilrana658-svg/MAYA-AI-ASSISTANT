package com.example

import com.example.ai.GeminiAiService
import com.example.ai.ParsedIntent
import com.example.model.ConversationContext
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    private val geminiService = GeminiAiService()

    @Test
    fun testParseControlledOutput_validChat() {
        val json = """
            {
              "type": "CHAT",
              "intent": null,
              "target": null,
              "response": "ঠিক আছে।"
            }
        """.trimIndent()

        val result = geminiService.parseControlledOutput(json)
        assertEquals("CHAT", result.type)
        assertNull(result.intent)
        assertNull(result.target)
        assertEquals("ঠিক আছে।", result.response)

        val (intent, response) = geminiService.mapControlledToIntent(
            result,
            ConversationContext(),
            "হ্যালো",
            isBangla = true
        )
        assertTrue(intent is ParsedIntent.GeneralAnswer)
        assertEquals("ঠিক আছে।", response)
    }

    @Test
    fun testParseControlledOutput_validActionOpenApp() {
        val json = """
            {
              "type": "ACTION",
              "intent": "OPEN_APP",
              "target": "Chrome",
              "response": "Chrome খুলছি..."
            }
        """.trimIndent()

        val result = geminiService.parseControlledOutput(json)
        assertEquals("ACTION", result.type)
        assertEquals("OPEN_APP", result.intent)
        assertEquals("Chrome", result.target)
        assertEquals("Chrome খুলছি...", result.response)

        val (intent, response, ctx) = geminiService.mapControlledToIntent(
            result,
            ConversationContext(),
            "ক্রোম ওপেন করো",
            isBangla = true
        )
        assertTrue(intent is ParsedIntent.OpenApp)
        assertEquals("Chrome", (intent as ParsedIntent.OpenApp).appTarget)
        assertEquals("Chrome", ctx.lastMentionedApp)
        assertEquals("Chrome খুলছি...", response)
    }

    @Test
    fun testParseControlledOutput_malformedJsonNeverCrashes() {
        val malformed = "This is some arbitrary text without any valid json { half broken"
        val result = geminiService.parseControlledOutput(malformed)

        assertEquals("CHAT", result.type)
        assertNull(result.intent)
        assertEquals(malformed, result.response)

        val (intent, response) = geminiService.mapControlledToIntent(
            result,
            ConversationContext(),
            "Random text",
            isBangla = false
        )
        assertTrue(intent is ParsedIntent.GeneralAnswer)
        assertEquals(malformed, response)
    }

    @Test
    fun testMapControlledToIntent_unknownInventedIntentRejected() {
        val json = """
            {
              "type": "ACTION",
              "intent": "UNAUTHORIZED_SYS_RESET",
              "target": "all",
              "response": "Executing system reset"
            }
        """.trimIndent()

        val result = geminiService.parseControlledOutput(json)
        val (intent, response) = geminiService.mapControlledToIntent(
            result,
            ConversationContext(),
            "Reset system",
            isBangla = false
        )
        // Must reject unknown intent and safely degrade to GeneralAnswer
        assertTrue(intent is ParsedIntent.GeneralAnswer)
        assertEquals("Executing system reset", response)
    }

    @Test
    fun testParseControlledOutput_markdownFencedJson() {
        val fenced = """
            ```json
            {
              "type": "CHAT",
              "intent": null,
              "target": null,
              "response": "I am Maya, your voice assistant."
            }
            ```
        """.trimIndent()

        val result = geminiService.parseControlledOutput(fenced)
        assertEquals("CHAT", result.type)
        assertEquals("I am Maya, your voice assistant.", result.response)
    }

    @Test
    fun testParseControlledOutput_invalidTypeFallsBackToChat() {
        val json = """
            {
              "type": "UNKNOWN",
              "response": "Hello"
            }
        """.trimIndent()

        val result = geminiService.parseControlledOutput(json)
        assertEquals("CHAT", result.type)
        assertNull(result.intent)
        assertEquals("Hello", result.response)
    }

    @Test
    fun testParseControlledOutput_missingResponseSafeFallback() {
        val json = """
            {
              "type": "CHAT",
              "response": ""
            }
        """.trimIndent()

        val result = geminiService.parseControlledOutput(json)
        assertEquals("CHAT", result.type)
        assertTrue(result.response.isNotBlank())
    }

    @Test
    fun testParseControlledOutput_actionWithoutIntentFallsBackToChat() {
        val json = """
            {
              "type": "ACTION",
              "target": "YouTube",
              "response": "Opening"
            }
        """.trimIndent()

        val result = geminiService.parseControlledOutput(json)
        assertEquals("CHAT", result.type)
        assertNull(result.intent)
        assertEquals("Opening", result.response)
    }

    @Test
    fun testParseControlledOutput_actionWithInvalidIntentFallsBackToChat() {
        val json = """
            {
              "type": "ACTION",
              "intent": "DO_SOMETHING",
              "target": "YouTube",
              "response": "Opening"
            }
        """.trimIndent()

        val result = geminiService.parseControlledOutput(json)
        assertEquals("CHAT", result.type)
        assertNull(result.intent)
        assertEquals("Opening", result.response)
    }

    @Test
    fun testParseControlledOutput_actionWithoutTargetDoesNotInventTarget() {
        val json = """
            {
              "type": "ACTION",
              "intent": "OPEN_WEBSITE",
              "response": "Opening website"
            }
        """.trimIndent()

        val result = geminiService.parseControlledOutput(json)
        assertEquals("ACTION", result.type)
        assertEquals("OPEN_WEBSITE", result.intent)
        assertNull(result.target) // Parser must never invent a URL or target
        assertEquals("Opening website", result.response)
    }

    @Test
    fun testParseControlledOutput_malformedJsonSafeFallback() {
        val malformed = "{ type: ACTION, intent: OPEN_APP"
        val result = geminiService.parseControlledOutput(malformed)
        assertEquals("CHAT", result.type)
        assertNull(result.intent)
        assertTrue(result.response.isNotBlank())
    }

    @Test
    fun testParseControlledOutput_emptyInputSafeFallback() {
        val result = geminiService.parseControlledOutput("")
        assertEquals("CHAT", result.type)
        assertNull(result.intent)
        assertTrue(result.response.isNotBlank())
    }

    @Test
    fun testUserSettingsEntity_toAndFromMapping() {
        val original = com.example.model.UserSettings(
            wakePhrase = "মায়া",
            stopCommand = "থামো",
            language = "Bangla",
            voiceSpeed = 0.9f,
            isLoggedIn = true,
            userName = "Test User"
        )
        val entity = com.example.db.UserSettingsEntity.fromUserSettings(original)
        assertEquals(1, entity.id)
        assertEquals("মায়া", entity.wakePhrase)
        assertEquals("থামো", entity.stopCommand)

        val restored = entity.toUserSettings()
        assertEquals(original.wakePhrase, restored.wakePhrase)
        assertEquals(original.stopCommand, restored.stopCommand)
        assertEquals(original.language, restored.language)
        assertEquals(original.voiceSpeed, restored.voiceSpeed, 0.001f)
        assertEquals(original.userName, restored.userName)
        assertEquals(original.isLoggedIn, restored.isLoggedIn)
    }

    @Test
    fun testZipAction_createsValidArchiveTarget() {
        val safeTarget = "my_backup.zip".trim().ifBlank { "archive" }.removeSuffix(".zip")
        assertEquals("my_backup", safeTarget)

        val emptyTarget = "".trim().ifBlank { "archive" }.removeSuffix(".zip")
        assertEquals("archive", emptyTarget)
    }
}
