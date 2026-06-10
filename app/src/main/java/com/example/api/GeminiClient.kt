package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiService::class.java)
    }
}

suspend fun callGemini(
    prompt: String,
    systemInstruction: String? = null,
    temperature: Float = 0.5f
): String {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        return "Configure your API Key in the AI Studio Secrets panel to activate your cognitive reflection mirror."
    }

    val request = GeminiRequest(
        contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
        generationConfig = GeminiGenerationConfig(temperature = temperature),
        systemInstruction = systemInstruction?.let { GeminiContent(parts = listOf(GeminiPart(it))) }
    )

    return try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        val textAnswer = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        textAnswer ?: "Gemini completed without writing any insights."
    } catch (e: Exception) {
        "Failure during cognitive mirroring: ${e.localizedMessage ?: e.message}"
    }
}
