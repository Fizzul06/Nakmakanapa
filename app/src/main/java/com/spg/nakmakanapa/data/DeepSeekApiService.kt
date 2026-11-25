package com.spg.nakmakanapa.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.spg.nakmakanapa.BuildConfig
import com.spg.nakmakanapa.model.GoogleReview
import com.spg.nakmakanapa.model.Restaurant
import com.spg.nakmakanapa.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class DeepSeekResult(
    val selectedRestaurant: Restaurant,
    val aiReason: String
)

class DeepSeekApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Writes the rationale for a restaurant the APP already picked.
     *
     * We no longer ask the model to choose: measured over repeated runs it
     * returned the same restaurant 6/6 times, and "pick diversely" + temp 1.2
     * still gave only 2 distinct results out of 3. Selection now happens in
     * [SurpriseRepository.pick] via weighted random.
     */
    suspend fun writeSurpriseReason(
        chosen: Restaurant,
        userProfile: UserProfile,
        recentPicks: List<String>,
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.ifBlank { null } ?: BuildConfig.DEEPSEEK_API_KEY

        val timeContext = buildTimeContext(chosen)

        if (apiKey.isBlank() || apiKey.contains("placeholder", ignoreCase = true)) {
            return@withContext "${chosen.name} is your surprise pick — ${chosen.distanceFormatted} away, " +
                    "${chosen.priceFormatted.lowercase()}, ${chosen.openingHours.lowercase()}. Enjoy!"
        }

        try {
            val systemPrompt = """You are a fun food guide AI for 'Nak Makan Apa?'.
The app has ALREADY chosen the restaurant. Do NOT change the choice. Your only job is to explain why it is a good pick right now.

LANGUAGE: Reply ONLY in English. The whole app is in English. No Malay, no Manglish, no Mandarin. Local dish names may stay as-is, but every sentence must be English.

Rules:
- 2 short sentences, warm and playful but not cringe.
- Never invent facts: only use the details given to you (price, distance, hours, status, rating, cuisine).
- Do not claim the place is open if its status says otherwise.
- If recently picked places are listed, you may mention that this one is something new.

Reply with plain prose only. No JSON, no markdown."""

            val context = buildString {
                appendLine("Restaurant: ${chosen.name}")
                appendLine("Cuisine/category: ${chosen.category}")
                appendLine("Distance: ${chosen.distanceFormatted}")
                appendLine("Price: ${chosen.priceFormatted}")
                appendLine("Status: ${timeContext.statusLine}")
                appendLine("Rating: ${chosen.rating} from ${chosen.userRatingsTotal} reviews")
                if (chosen.address.isNotBlank()) appendLine("Area: ${chosen.address}")
                appendLine(timeContext.clockLine)
                appendLine("Meal slot now: ${timeContext.slot}")
                if (recentPicks.isNotEmpty()) {
                    appendLine("Recently eaten (avoid mentioning as the pick): ${recentPicks.joinToString(", ")}")
                }
                appendLine("User: ${userProfile.username}, budget RM${userProfile.maxBudget}, mood: ${userProfile.customMoodPrompt}")
            }

            val payload = JsonObject().apply {
                addProperty("model", "deepseek-chat")
                addProperty("max_tokens", 120)
                addProperty("temperature", 0.9)
                add("messages", gson.toJsonTree(
                    listOf(
                        mapOf("role" to "system", "content" to systemPrompt),
                        mapOf("role" to "user", "content" to context)
                    )
                ))
            }

            val request = Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .post(gson.toJson(payload).toRequestBody(jsonMediaType))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = gson.fromJson(body, JsonObject::class.java)
                        val choices = json.getAsJsonArray("choices")
                        if (choices != null && choices.size() > 0) {
                            val text = choices[0].asJsonObject
                                .getAsJsonObject("message")
                                .get("content").asString
                                .trim()
                                .trim('"')
                            if (text.isNotBlank()) return@withContext text
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        "${chosen.name} is your surprise pick — ${chosen.distanceFormatted} away and ${chosen.priceFormatted.lowercase()}."
    }

    private data class TimeContext(
        val clockLine: String,
        val slot: String,
        val statusLine: String
    )

    private fun buildTimeContext(r: Restaurant): TimeContext {
        val zone = ZoneId.of("Asia/Kuala_Lumpur")
        val now = ZonedDateTime.now(zone)
        val fmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy 'at' h:mm a")

        val slot = when (now.hour) {
            in 5..10 -> "breakfast"
            in 11..14 -> "lunch"
            in 15..17 -> "afternoon tea"
            in 18..21 -> "dinner"
            in 22..23, in 0..1 -> "late supper"
            else -> "late night"
        }

        val statusLine = when (r.isOpenNow) {
            true -> "OPEN right now"
            false -> "CLOSED right now"
            null -> "opening hours unknown"
        }

        return TimeContext(
            clockLine = "Current local time: ${now.format(fmt)} ($zone)",
            slot = slot,
            statusLine = statusLine
        )
    }

    /**
     * Feeds actual Google Places user reviews to DeepSeek AI to generate a 2-bullet summary!
     */
    suspend fun summarizeGoogleReviews(
        restaurantName: String,
        reviews: List<GoogleReview>,
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.ifBlank { null } ?: BuildConfig.DEEPSEEK_API_KEY

        if (reviews.isEmpty() || apiKey.isBlank() || apiKey.contains("placeholder", ignoreCase = true)) {
            return@withContext "• Popular local dining spot with positive customer feedback!\n• Known for delicious Malaysian flavors, good portions, and friendly service."
        }

        try {
            val reviewTexts = reviews.map { "${it.authorName} (${it.rating}⭐): ${it.text}" }
            val systemPrompt = "You are an expert foodie AI. Analyse these Google Places reviews for restaurant '$restaurantName' and summarise customer sentiment in 2 short bullet points (what people love, and tips for visiting). LANGUAGE: reply ONLY in English — the app is English-only. Do not use Malay or Manglish; keep local dish names as-is but write the sentences in English."

            val userContent = "Google Reviews: ${gson.toJson(reviewTexts)}"

            val payload = JsonObject().apply {
                addProperty("model", "deepseek-chat")
                addProperty("max_tokens", 140)
                addProperty("temperature", 0.6)
                
                val messages = gson.toJsonTree(
                    listOf(
                        mapOf("role" to "system", "content" to systemPrompt),
                        mapOf("role" to "user", "content" to userContent)
                    )
                )
                add("messages", messages)
            }

            val request = Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .post(gson.toJson(payload).toRequestBody(jsonMediaType))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val jsonResp = gson.fromJson(bodyString, JsonObject::class.java)
                        val choices = jsonResp.getAsJsonArray("choices")
                        if (choices != null && choices.size() > 0) {
                            val summary = choices[0].asJsonObject
                                .getAsJsonObject("message")
                                .get("content").asString.trim()

                            if (summary.isNotBlank()) {
                                return@withContext summary
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        "• Customers rave about authentic local taste and generous portions!\n• Recommended for quick dining and friendly hospitality."
    }
}