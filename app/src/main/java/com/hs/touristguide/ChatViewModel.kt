package com.hs.touristguide.home

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hs.touristguide.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ChatViewModel : ViewModel() {

    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaTypeOrNull()
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val apiUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=AIzaSyB1HbIZc1Tp5HMlgnrgep-DV-BiMDv6hdM\n"

    val messages = mutableStateListOf<Message>()

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // Add user's message to the UI
        messages.add(Message(userMessage, isUser = true))

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Create JSON payload for Gemini API
                val requestBodyJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                        })
                    })
                }

                val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        addBotMessage("❌ Network error: ${e.localizedMessage}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            addBotMessage("❌ API error: ${response.code}")
                            return
                        }

                        val responseBody = response.body?.string()
                        if (responseBody.isNullOrEmpty()) {
                            addBotMessage("❌ Empty response from Gemini API")
                            return
                        }

                        try {
                            val json = JSONObject(responseBody)
                            val replyText = json
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")

                            addBotMessage(replyText)
                        } catch (e: Exception) {
                            addBotMessage("❌ Parsing error: ${e.localizedMessage}")
                        }
                    }
                })

            } catch (e: Exception) {
                addBotMessage("❌ Exception: ${e.localizedMessage}")
            }
        }
    }

    private fun addBotMessage(text: String) {
        viewModelScope.launch(Dispatchers.Main) {
            messages.add(Message(text, isUser = false))
        }
    }
}
