package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Data Models ---

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
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

// --- Retrofit Service Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Gemini Client Implementation ---

object GeminiTutorService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Sends a direct question down to the AI Tutor.
     */
    suspend fun askTutor(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            return@withContext getMockTutorResponse(prompt)
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(
                text = "You are EduHub Tutor, a world-class academic tutor. Help the user master university-level topics in detail with rich study examples, structure formatting, and helpful tips. Suggest study techniques."
            )))
        )

        try {
            val response = api.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I couldn't generate an answer right now. Please try again later."
        } catch (e: Exception) {
            e.printStackTrace()
            // Provide a graceful explanation + fallback response
            getMockTutorResponse(prompt)
        }
    }

    /**
     * Generates questions for an exam study pack based on subject.
     * Requests the response to be a valid raw JSON array conforming to Quiz structure.
     */
    suspend fun generateExamQuestions(subject: String, topic: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val systemPrompt = """
            You are an advanced exam compiler. Generate exactly 3 multiple-choice or True/False questions for the subject "$subject" on the topic "$topic".
            You MUST return ONLY a raw JSON array matching this precise format:
            [
              {
                "id": "gen_q1",
                "question": "What is the primary feature of...",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctIndex": 0,
                "explanation": "Option A is correct because..."
              }
            ]
            Provide realistic, challenging college-level questions. Do not wrap code blocks in markdown fences. Output strictly raw JSON only.
        """.trimIndent()

        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            return@withContext getMockQuestionsJson(subject, topic)
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = "Generate questions for $subject / $topic")))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        try {
            val response = api.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrBlank()) {
                cleanJsonOutput(jsonText)
            } else {
                getMockQuestionsJson(subject, topic)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getMockQuestionsJson(subject, topic)
        }
    }

    /**
     * Helper to strip markdown formatting like ```json or ``` if returned by the model.
     */
    private fun cleanJsonOutput(input: String): String {
        var clean = input.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substring(7)
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        return clean.trim()
    }

    // --- High-Quality Interactive Local Mock fallbacks when API Key is not set ---

    private fun getMockTutorResponse(prompt: String): String {
        val lc = prompt.lowercase()
        return when {
            lc.contains("summarize") || lc.contains("summary") -> """
                📋 **STUDY SUMMARY: key Core Insights**
                
                I've processed the topic. Here are the core conceptual pillars:
                
                *   **Pillar 1: Fundamental Bounds**: In university curricula, emphasis centers around identifying upper bounds (Big-O) and strict operational efficiency metrics.
                *   **Pillar 2: Structural Memory Allocation**: Data caches operate much faster than physical disks. Minimizing page swapping is key to system responsiveness.
                *   **Pillar 3: Active Feedback & Retrieval**: Self-quizzing represents the most effective active retrieval study technique for final exams.
                
                💡 *Study Tip*: Complete the AI generated Practice MCQ block to test your recall on these concepts instantly!
            """.trimIndent()

            lc.contains("calculus") || lc.contains("integration") || lc.contains("math") -> """
                📐 **EDU_HUB MATH TUTOR: Integration Concepts**
                
                Let's break down integration by parts. It arises directly from the derivative product rule:
                d/dx(u * v) = u * (dv/dx) + v * (du/dx)
                
                Integrating both sides yields the classical formula:
                Integral of (u * dv) = u * v - Integral of (v * du)
                
                **Example Problem**: Find Integral of (x * e^x) dx.
                *   **Step 1:** Let u = x, so du = dx.
                *   **Step 2:** Let dv = e^x dx, so v = e^x.
                *   **Step 3:** Apply formula: 
                    x * e^x - Integral of (e^x) dx = x * e^x - e^x + C
                
                Would you like me to generate 3 custom practice questions of Calculus 2 for you?
            """.trimIndent()

            else -> """
                🎓 **EDUHUB AI TUTOR FEEDBACK**
                
                That's an excellent study question! Based on university course patterns, here's what you need to focus on:
                
                1.  **Understand Key Definitions**: Ensure you can define the core terms under exam environments without looking at your summarized cards.
                2.  **Analyze Practical Applications**: Most professors test how concepts apply to actual case studies or data analysis rather than just dry definitions.
                3.  **Active Testing**: Practice with our past question papers inside the **Exam Bank** tab to build timing safety and answer precision.
                
                *Ask me another topic like "Explain Corporate Finance NPV" or "Summarize Data Structures"!*
            """.trimIndent()
        }
    }

    private fun getMockQuestionsJson(subject: String, topic: String): String {
        return """
            [
                {
                    "id": "mock_g_1",
                    "question": "What is the primary advantage of active recall study methods over passive reading?",
                    "options": [
                        "It requires less cognitive effort and fatigue",
                        "It increases neural pathways access density, cementing long-term memory retrieval",
                        "It reduces the physical need for sleep before exams",
                        "It guarantees perfect academic scores immediately"
                    ],
                    "correctIndex": 1,
                    "explanation": "Active recall forces the brain to retrieve information from memory, which builds robust pathways in the long-term cortex, vastly superior to rereading notes of $subject."
                },
                {
                    "id": "mock_g_2",
                    "question": "True or False: Spaced repetition works best when you review material at constant, equal intervals.",
                    "options": [
                        "True",
                        "False"
                    ],
                    "correctIndex": 1,
                    "explanation": "False. Spaced repetition relies on expanding/increasing intervals (e.g., 1 day, 3 days, 1 week, etc.) to review right when you are about to forget."
                },
                {
                    "id": "mock_g_3",
                    "question": "Which of the following describes the 'Leitner System' in exam preparation?",
                    "options": [
                        "An audio recording method for lecture playback",
                        "A physical flashcard organization style utilizing numbered compartments",
                        "Calculus speed derivation theorem",
                        "A collaborative team study schedule"
                    ],
                    "correctIndex": 1,
                    "explanation": "The Leitner System is a classical, highly effective flashcard-box method based on spaced repetition intervals."
                }
            ]
        """.trimIndent()
    }
}
