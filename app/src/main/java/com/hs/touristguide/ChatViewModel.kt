package com.hs.touristguide.home

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hs.touristguide.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class ChatViewModel : ViewModel() {

    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaTypeOrNull()
    private val apiKey = BuildConfig.GEMINI_API_KEY

    // Correct Gemini endpoint
    private val apiUrl =
        "https://generativelanguage.googleapis.com/v1beta2/models/gemini-1:generateMessage?key=$apiKey"

    val messages = mutableStateListOf<Message>()

    // Chat context
    private val chatHistory = mutableListOf<Map<String, Any>>(
        mapOf(
            "author" to "system",
            "content" to listOf(
                mapOf(
                    "type" to "text",
                    "text" to "You are a helpful tourist guide AI giving detailed answers about locations, history, food, and travel in India."
                )
            )
        )
    )

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        messages.add(Message(userMessage, isUser = true))

        chatHistory.add(
            mapOf(
                "author" to "user",
                "content" to listOf(mapOf("type" to "text", "text" to userMessage))
            )
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestPayload = JSONObject().apply {
                    put(
                        "prompt", JSONObject().apply {
                            put(
                                "messages", JSONArray(chatHistory)
                            )
                        }
                    )
                    put("temperature", 0.7)
                }

                val requestBody = requestPayload.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        addBotMessage("❌ Network error: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            addBotMessage("❌ API error: ${response.code}")
                            return
                        }

                        val body = response.body?.string()
                        if (body.isNullOrEmpty()) {
                            addBotMessage("❌ Empty response from Gemini")
                            return
                        }

                        try {
                            val json = JSONObject(body)
                            val reply = json
                                .getJSONObject("message")
                                .getJSONArray("content")
                                .getJSONObject(0)
                                .getString("text")

                            // Add bot reply to history
                            chatHistory.add(
                                mapOf(
                                    "author" to "bot",
                                    "content" to listOf(mapOf("type" to "text", "text" to reply))
                                )
                            )

                            addBotMessage(reply)

                        } catch (e: Exception) {
                            addBotMessage("❌ Parsing error: ${e.message}")
                        }
                    }
                })
            } catch (e: Exception) {
                addBotMessage("❌ Exception: ${e.message}")
            }
        }
    }

    private fun addBotMessage(text: String) {
        viewModelScope.launch(Dispatchers.Main) {
            messages.add(Message(text, isUser = false))
        }
    }
}
