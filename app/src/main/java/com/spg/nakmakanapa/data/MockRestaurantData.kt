package com.spg.nakmakanapa.data

import com.spg.nakmakanapa.model.Restaurant

/**
 * DELETED.
 *
 * This file previously contained a hardcoded fake restaurant list that was silently
 * returned whenever the Google Places call failed. Because the legacy Places API was
 * permanently disabled for this project (REQUEST_DENIED), every request failed and
 * users only ever saw these fake entries — including a pizza photo labelled "Mamak".
 *
 * There is intentionally NO offline fallback. If the live API fails, the UI shows an
 * error with a retry button so the problem is visible instead of hidden.
 */
@Deprecated("Fake data removed. Use GooglePlacesApiService only.")
object MockRestaurantData {
    val sampleRestaurants: List<Restaurant> = emptyList()
}