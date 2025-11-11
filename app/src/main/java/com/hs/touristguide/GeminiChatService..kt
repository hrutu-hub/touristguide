package com.hs.touristguide

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object GeminiChatService {

    // ✅ Load safely from BuildConfig
    private val apiKey = BuildConfig.GEMINI_API_KEY

    // ✅ Use a single Gemini model instance
    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash", // You can change to "gemini-1.5-pro" if needed
            apiKey = apiKey
        )
    }

    /**
     * Sends a prompt to Gemini and returns a plain text response.
     * Handles retries, 503 errors, and network issues gracefully.
     */
    suspend fun getChatResponse(prompt: String): String = withContext(Dispatchers.IO) {
        var lastError: String? = null

        for (attempt in 1..3) {
            try {
                val response: GenerateContentResponse = model.generateContent(prompt)
                val text = response.text

                if (!text.isNullOrBlank()) {
                    return@withContext text.trim()
                } else {
                    lastError = "Empty response from model."
                }

            } catch (e: Exception) {
                val message = e.message ?: "Unknown error"
                lastError = message

                // Handle 503 or network error — wait & retry
                if (message.contains("503") || message.contains("UNAVAILABLE", ignoreCase = true)) {
                    val delayTime = attempt * 1500L
                    delay(delayTime)
                    continue
                }

                // Other errors — return early
                return@withContext "Error: $message"
            }
        }

        // All attempts failed
        return@withContext "Service temporarily unavailable. Please try again later. ($lastError)"
    }
}
