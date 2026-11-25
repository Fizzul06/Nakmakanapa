package com.spg.nakmakanapa.model

/**
 * Snapshot of Surprise Me quota/history, used only by the hidden debug dialog.
 * Kept as a plain data class so ProfileScreen stays free of repository logic.
 */
data class DebugState(
    val surpriseUsedToday: Int = 0,
    val surpriseDailyLimit: Int = 0,
    val cooldownActive: Boolean = false,
    val pickHistoryCount: Int = 0
)