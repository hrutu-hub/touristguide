package com.hs.touristguide

import com.google.ai.client.generativeai.GenerativeModel

object GeminiChatService {
    // ✅ Load your Gemini API key safely from BuildConfig (injected via local.properties)
    private val apiKey = BuildConfig.GEMINI_API_KEY

    // ✅ Choose which Gemini model you want to use
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    /**
     * Sends a prompt to Gemini and returns the model's response as plain text.
     */
    suspend fun getChatResponse(prompt: String): String {
        return try {
            val response = model.generateContent(prompt)
            response.text ?: "No response received."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
