package com.spg.nakmakanapa.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.spg.nakmakanapa.model.PickRecord
import com.spg.nakmakanapa.model.Restaurant
import com.spg.nakmakanapa.model.SurpriseQuota
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import kotlin.math.pow
import kotlin.random.Random

private val Context.surpriseDataStore by preferencesDataStore(name = "surprise_me_prefs")

/**
 * Owns everything about "Surprise Me!" selection.
 *
 * WHY THIS EXISTS: DeepSeek was being asked to *choose* the restaurant, and I
 * measured that it is effectively deterministic — 6 identical runs returned the
 * same restaurant every time, and even "pick diversely" + temperature 1.2 gave
 * only 2 distinct results out of 3. Position #1 in the candidate list wins.
 *
 * So the app now does the choosing (weighted random, with recency penalties)
 * and DeepSeek is only asked to *write the reason* for the already-chosen place.
 * That is deterministic, cheap, and immune to quota exhaustion.
 */
class SurpriseRepository(private val context: Context) {

    companion object {
        /** Picks allowed per calendar day (Asia/Kuala_Lumpur). */
        const val DAILY_LIMIT = 8

        /** Minimum gap between two picks, to stop spam-tapping. */
        const val COOLDOWN_MS = 10_000L // 10 seconds

        /** Keep history bounded. */
        private const val MAX_HISTORY = 50
    }

    private val historyKey = stringPreferencesKey("surprise_pick_history")
    private val lastPickKey = longPreferencesKey("surprise_last_pick_at")
    private val gson = Gson()

    private val zone = ZoneId.of("Asia/Kuala_Lumpur")

    // ------------------------------------------------------------- history

    val pickHistory: Flow<List<PickRecord>> = context.surpriseDataStore.data.map { prefs ->
        decode(prefs[historyKey])
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val quota: Flow<SurpriseQuota> = context.surpriseDataStore.data
        .map { prefs ->
            // Only react to real data changes (new pick / reset).
            val lastPick = prefs[lastPickKey] ?: 0L
            val history = decode(prefs[historyKey])
            lastPick to history
        }
        .distinctUntilChanged()
        .flatMapLatest { (lastPick, history) ->
            // Emit a fresh snapshot every second so the cooldown actually
            // counts down on screen. Without this the Flow only re-emitted when
            // DataStore changed, so the timer appeared frozen.
            flow {
                while (true) {
                    val now = System.currentTimeMillis()
                    emit(
                        SurpriseQuota(
                            usedToday = history.count { isSameDay(it.pickedAt, now) },
                            dailyLimit = DAILY_LIMIT,
                            cooldownRemainingMs = (lastPick + COOLDOWN_MS - now).coerceAtLeast(0L),
                            historyCount = history.size
                        )
                    )
                    if (lastPick + COOLDOWN_MS - now <= 0L) break
                    delay(1000)
                }
            }
        }

    // ------------------------------------------------------------ picking

    /**
     * Picks a restaurant for the user. Call this BEFORE asking DeepSeek for prose.
     *
     * @param candidates already filtered by budget/radius/time-of-day.
     * @return the chosen restaurant, or null if [candidates] is empty.
     */
    suspend fun pick(candidates: List<Restaurant>, history: List<PickRecord>): Restaurant? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()

        val now = System.currentTimeMillis()
        val today = now / 86_400_000L

        // index history by placeId
        val lastPickedAt = history.associate { it.placeId to it.pickedAt }

        val weights = candidates.map { r ->
            var w = 1.0
            val last = lastPickedAt[r.id]
            if (last != null) {
                val daysAgo = (today - last / 86_400_000L).toInt()
                w *= when {
                    daysAgo <= 0 -> 0.02   // already had it today — nearly impossible
                    daysAgo <= 7 -> 0.35   // this week — deprioritised
                    else -> 0.8            // long ago — small penalty only
                }
            }
            // Slight nudge toward higher-rated places, but not enough to dominate.
            w *= 1.0 + (r.rating.coerceIn(0.0, 5.0) / 5.0) * 0.25
            w
        }

        val total = weights.sum()
        if (total <= 0.0) return candidates.random()

        var roll = Random.nextDouble() * total
        for (i in candidates.indices) {
            roll -= weights[i]
            if (roll <= 0.0) return candidates[i]
        }
        return candidates.last()
    }

    /** Records a pick so future selections avoid repeating it. */
    suspend fun recordPick(restaurant: Restaurant, reason: String?) {
        val now = System.currentTimeMillis()
        context.surpriseDataStore.edit { prefs ->
            val history = decode(prefs[historyKey]).toMutableList()
            history.add(
                PickRecord(
                    placeId = restaurant.id,
                    name = restaurant.name,
                    pickedAt = now,
                    reason = reason
                )
            )
            val trimmed = history.takeLast(MAX_HISTORY)
            prefs[historyKey] = gson.toJson(trimmed)
            prefs[lastPickKey] = now
        }
    }

    // -------------------------------------------------------------- debug

    suspend fun resetLimitAndCooldown() {
        context.surpriseDataStore.edit { prefs ->
            // Keep older history (variety still works) but clear today's count.
            prefs.remove(lastPickKey)
            val history = decode(prefs[historyKey])
                .filter { !isSameDay(it.pickedAt, System.currentTimeMillis()) }
            prefs[historyKey] = gson.toJson(history)
        }
    }

    suspend fun clearHistory() {
        context.surpriseDataStore.edit { prefs ->
            prefs.remove(historyKey)
            prefs.remove(lastPickKey)
        }
    }

    // ------------------------------------------------------------ helpers

    private fun isSameDay(a: Long, b: Long): Boolean {
        val da = Instant.ofEpochMilli(a).atZone(zone).toLocalDate()
        val db = Instant.ofEpochMilli(b).atZone(zone).toLocalDate()
        return da == db
    }

    private fun decode(json: String?): List<PickRecord> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson(json, Array<PickRecord>::class.java).toList()
        }.getOrDefault(emptyList())
    }
}