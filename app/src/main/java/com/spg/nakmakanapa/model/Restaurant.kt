package com.spg.nakmakanapa.model

import java.util.Locale

data class GoogleReview(
    val authorName: String,
    val rating: Double,
    val text: String,
    val relativeTime: String
)

data class Restaurant(
    val id: String,
    val name: String,
    val photoUrl: String? = null,
    /**
     * Every photo Google returned, as ready-to-load URLs, ordered best-first for
     * the hero slot (closest to a 16:9 landscape first). [photoUrl] is always
     * [photoUrls].firstOrNull().
     */
    val photoUrls: List<String> = emptyList(),
    val imageResUrl: String = "",
    val distanceKm: Double = 0.0,
    val minPrice: Int = 1,
    val maxPrice: Int = 20,
    val openingHours: String = "Hours unavailable",
    /**
     * Tri-state open status from Google:
     *  true  = open now
     *  false = closed now
     *  null  = Google does not know
     *
     * Never treat null as open. It used to default to true, which made places
     * appear "Open now" at 11pm when Google simply had no hours data.
     */
    val isOpenNow: Boolean? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String = "Mamak",
    val isAdventure: Boolean = false,
    val isSaved: Boolean = false,
    /**
     * Raw Google Places price level, e.g. "PRICE_LEVEL_INEXPENSIVE".
     * Null when Google has no price data for this place (about 30% of Malaysian
     * restaurants). Do not derive "fancy" from minPrice — it can be missing.
     */
    val priceLevel: String? = null,
    val rating: Double = 4.5,
    val userRatingsTotal: Int = 120,
    val aiReason: String? = null,
    val address: String = "Malaysia",
    val reviews: List<GoogleReview> = emptyList(),
    val reviewsSummary: String? = null
) {
    val priceFormatted: String
        get() = "RM$minPrice-$maxPrice per person"

    /** True when Google explicitly labels this place expensive. */
    val isFancy: Boolean
        get() = priceLevel == "PRICE_LEVEL_EXPENSIVE" ||
                priceLevel == "PRICE_LEVEL_VERY_EXPENSIVE"

    val distanceFormatted: String
        get() = if (distanceKm >= 10.0) "${distanceKm.toInt()} km" else String.format(Locale.US, "%.1f km", distanceKm)
}