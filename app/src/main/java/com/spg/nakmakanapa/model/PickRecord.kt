package com.spg.nakmakanapa.model

/** One past "Surprise Me!" pick, used to avoid repeats and power the debug screen. */
data class PickRecord(
    val placeId: String,
    val name: String,
    val pickedAt: Long,
    val reason: String? = null
)

/** Current state of the Surprise Me quota. */
data class SurpriseQuota(
    val usedToday: Int,
    val dailyLimit: Int,
    val cooldownRemainingMs: Long,
    val historyCount: Int
) {
    val hasCooldown: Boolean get() = cooldownRemainingMs > 0
    val exhausted: Boolean get() = usedToday >= dailyLimit
    val remaining: Int get() = (dailyLimit - usedToday).coerceAtLeast(0)
    val cooldownSeconds: Int get() = (cooldownRemainingMs / 1000).toInt()
}