package com.spg.nakmakanapa.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.spg.nakmakanapa.BuildConfig
import com.spg.nakmakanapa.location.LocationHelper
import com.spg.nakmakanapa.model.GoogleReview
import com.spg.nakmakanapa.model.Restaurant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Live Google Places **API v1** (Places API New) client.
 *
 * NOTE: The legacy endpoint (maps.googleapis.com/maps/api/place/...) is permanently
 * disabled for this project. It returns REQUEST_DENIED. All calls must go through
 * places.googleapis.com/v1 and send the key in the X-Goog-Api-Key header.
 */
class GooglePlacesApiService {

    companion object {
        private const val TAG = "PlacesV1"
        private const val BASE = "https://places.googleapis.com/v1"

        // Field masks keep billing/cost low and guarantee the fields we render exist.
        private val SEARCH_FIELDS = listOf(
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.shortFormattedAddress",
            "places.location",
            "places.rating",
            "places.userRatingCount",
            "places.priceLevel",
            "places.photos",
            "places.currentOpeningHours",
            "places.primaryType",
            "places.types"
        ).joinToString(",")

        /**
         * primaryTypes that mean "this is a place you can eat at".
         *
         * Google's `includedTypes` filter matches ANY entry in a place's `types`
         * array, not its primary type. That is how "Uptown Sports" (a sports
         * complex that happens to contain a cafe) ended up in restaurant results: its
         * types include `restaurant` and `cafe`. Same for hotels with restaurants.
         *
         * Dropping anything whose primaryType is not food-related removes those.
         */
        private val FOOD_PRIMARY_TYPES = setOf(
            "restaurant", "cafe", "bakery", "bar", "meal_takeaway",
            "meal_delivery", "food_court", "ice_cream_shop", "coffee_shop",
            "sandwich_shop", "dessert_shop", "donut_shop", "bagel_shop",
            "breakfast_restaurant", "brunch_restaurant", "fast_food_restaurant",
            "hamburger_restaurant", "pizza_restaurant", "sandwich_restaurant",
            "seafood_restaurant", "steak_house", "sushi_restaurant",
            "vegetarian_restaurant", "vegan_restaurant", "buffet_restaurant",
            "fine_dining_restaurant", "family_restaurant", "bar_and_grill",
            "pub", "wine_bar", "snack_bar", "tea_house", "juice_shop"
        )

        /** True when primaryType looks like somewhere you can eat. */
        private fun isFoodPrimaryType(primaryType: String?): Boolean {
            if (primaryType.isNullOrBlank()) return true // unknown -> don't drop it
            if (primaryType in FOOD_PRIMARY_TYPES) return true
            return primaryType.endsWith("_restaurant") || primaryType.endsWith("_shop")
        }

        private val DETAILS_FIELDS = listOf(
            "id",
            "displayName",
            "formattedAddress",
            "location",
            "rating",
            "userRatingCount",
            "priceLevel",
            "photos",
            "regularOpeningHours",
            "currentOpeningHours",
            "priceRange",
            "types",
            "reviews"
        ).joinToString(",")
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun apiKey(custom: String?): String =
        custom?.ifBlank { null } ?: BuildConfig.MAPS_API_KEY

    // ---------------------------------------------------------------- search

    /**
     * Nearby restaurant search (Places API v1 :searchNearby).
     *
     * [maxResults] is capped at 10 on purpose: every returned place renders a
     * thumbnail, and each Place Photo fetch is a separately billed request.
     */
    suspend fun searchNearby(
        lat: Double,
        lng: Double,
        radiusMeters: Int = 8000,
        includedTypes: List<String> = listOf("restaurant"),
        maxResults: Int = 10,
        customApiKey: String? = null
    ): PlacesResult = withContext(Dispatchers.IO) {
        val key = apiKey(customApiKey)
        if (key.isBlank()) return@withContext PlacesResult.Error("Missing Google Places API key.")

        val body = JsonObject().apply {
            add("includedTypes", gson.toJsonTree(includedTypes))
            addProperty("maxResultCount", maxResults.coerceIn(1, 20))
            // NOTE: priceLevels is NOT supported on :searchNearby (returns HTTP 400),
            // so it is deliberately not sent here. Budget filtering uses searchText.
            add("locationRestriction", JsonObject().apply {
                add("circle", JsonObject().apply {
                    add("center", JsonObject().apply {
                        addProperty("latitude", lat)
                        addProperty("longitude", lng)
                    })
                    addProperty("radius", radiusMeters.toDouble())
                })
            })
        }

        post("$BASE/places:searchNearby", body.toString(), SEARCH_FIELDS, key, lat, lng)
    }

    /**
     * Dedicated "fine dining" search.
     *
     * Filtering the general list on priceLevel almost never works: only 1 of 20
     * nearby KL results is tagged EXPENSIVE, and 30% carry no priceLevel at all.
     *
     * Note: `priceLevels` is only supported on :searchText, NOT on :searchNearby
     * (that returns HTTP 400). Verified against the live API.
     */
    suspend fun searchFancy(
        lat: Double,
        lng: Double,
        radiusMeters: Int = 25000,
        maxResults: Int = 10,
        customApiKey: String? = null
    ): PlacesResult = searchByPriceLevels(
        lat = lat,
        lng = lng,
        radiusMeters = radiusMeters,
        priceLevels = listOf("PRICE_LEVEL_EXPENSIVE", "PRICE_LEVEL_VERY_EXPENSIVE"),
        maxResults = maxResults,
        keepOnly = { it.isFancy },
        customApiKey = customApiKey
    )

    /**
     * Budget search — asks Google for cheap price levels, then narrows to the
     * user's actual RM ceiling client-side.
     *
     * Why this exists: the Budget chip used to filter only the loaded list, so a
     * tight budget could show "no results" while affordable places existed but
     * were never fetched. Now we widen the net first, then apply the RM filter.
     */
    suspend fun searchBudget(
        lat: Double,
        lng: Double,
        maxPricePerPerson: Int,
        radiusMeters: Int = 8000,
        maxResults: Int = 10,
        customApiKey: String? = null
    ): PlacesResult = searchByPriceLevels(
        lat = lat,
        lng = lng,
        radiusMeters = radiusMeters,
        // Anything above MODERATE can never fit a small budget, so don't ask for it.
        priceLevels = listOf("PRICE_LEVEL_INEXPENSIVE", "PRICE_LEVEL_MODERATE"),
        maxResults = 20,
        keepOnly = { it.minPrice <= maxPricePerPerson },
        customApiKey = customApiKey,
        takeAfterFilter = maxResults
    )

    private suspend fun searchByPriceLevels(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        priceLevels: List<String>,
        maxResults: Int,
        keepOnly: (Restaurant) -> Boolean,
        customApiKey: String? = null,
        takeAfterFilter: Int? = null
    ): PlacesResult = withContext(Dispatchers.IO) {
        val key = apiKey(customApiKey)
        if (key.isBlank()) return@withContext PlacesResult.Error("Missing Google Places API key.")

        val body = JsonObject().apply {
            addProperty("textQuery", "restaurant")
            add("priceLevels", gson.toJsonTree(priceLevels))
            addProperty("maxResultCount", maxResults.coerceIn(1, 20))
            add("locationBias", JsonObject().apply {
                add("circle", JsonObject().apply {
                    add("center", JsonObject().apply {
                        addProperty("latitude", lat)
                        addProperty("longitude", lng)
                    })
                    addProperty("radius", radiusMeters.toDouble())
                })
            })
        }

        val result = post("$BASE/places:searchText", body.toString(), SEARCH_FIELDS, key, lat, lng)

        // Belt-and-braces: apply the caller's own predicate in case Google's
        // priceLevels filter is ignored or the place has no price data.
        return@withContext when (result) {
            is PlacesResult.Success -> {
                val matched = result.places.filter(keepOnly)
                    .let { list -> takeAfterFilter?.let { list.take(it) } ?: list }
                // Fall back to the unfiltered list only when nothing matched AND
                // the caller did not ask us to respect a hard limit.
                if (matched.isNotEmpty()) PlacesResult.Success(matched)
                else PlacesResult.Success(emptyList())
            }
            is PlacesResult.Error -> result
        }
    }

    /**
     * Free-text search (Places API v1 :searchText) — used for category chips
     * like "mamak", "nasi lemak", plus scenic adventure keywords.
     */
    suspend fun searchText(
        query: String,
        lat: Double,
        lng: Double,
        radiusMeters: Int = 10000,
        maxResults: Int = 10,
        customApiKey: String? = null
    ): PlacesResult = withContext(Dispatchers.IO) {
        val key = apiKey(customApiKey)
        if (key.isBlank()) return@withContext PlacesResult.Error("Missing Google Places API key.")

        val body = JsonObject().apply {
            addProperty("textQuery", query)
            addProperty("maxResultCount", maxResults.coerceIn(1, 20))
            add("locationBias", JsonObject().apply {
                add("circle", JsonObject().apply {
                    add("center", JsonObject().apply {
                        addProperty("latitude", lat)
                        addProperty("longitude", lng)
                    })
                    addProperty("radius", radiusMeters.toDouble())
                })
            })
        }

        post("$BASE/places:searchText", body.toString(), SEARCH_FIELDS, key, lat, lng)
    }

    private fun post(
        url: String,
        json: String,
        fieldMask: String,
        key: String,
        userLat: Double,
        userLng: Double
    ): PlacesResult {
        return try {
            val request = Request.Builder()
                .url(url)
                .post(json.toRequestBody(jsonMediaType))
                .addHeader("X-Goog-Api-Key", key)
                .addHeader("X-Goog-FieldMask", fieldMask)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP ${response.code}: $text")
                    return PlacesResult.Error(parseError(text, response.code))
                }

                val root = gson.fromJson(text, JsonObject::class.java)
                val arr = root.getAsJsonArray("places")
                    ?: return PlacesResult.Success(emptyList())

                val list = arr.mapNotNull { el -> mapPlace(el.asJsonObject, userLat, userLng, key) }
                Log.d(TAG, "Live places returned: ${list.size}")
                PlacesResult.Success(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}", e)
            PlacesResult.Error(e.message ?: "Network error. Check your connection.")
        }
    }

    // --------------------------------------------------------------- details

    /** Place details (Places API v1 GET /places/{placeId}) including real reviews. */
    suspend fun fetchPlaceDetails(
        placeId: String,
        customApiKey: String? = null
    ): PlaceDetailsResult = withContext(Dispatchers.IO) {
        val key = apiKey(customApiKey)
        if (key.isBlank() || placeId.isBlank()) {
            return@withContext PlaceDetailsResult.Error("Missing place id or API key.")
        }

        try {
            // Place IDs from v1 are bare ("ChIJ..."); pass our own "place_" prefix guard.
            val id = placeId.removePrefix("places/")
            val request = Request.Builder()
                .url("$BASE/places/$id")
                .get()
                .addHeader("X-Goog-Api-Key", key)
                .addHeader("X-Goog-FieldMask", DETAILS_FIELDS)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Details HTTP ${response.code}: $text")
                    return@withContext PlaceDetailsResult.Error(parseError(text, response.code))
                }

                val obj = gson.fromJson(text, JsonObject::class.java)

                val reviews = mutableListOf<GoogleReview>()
                obj.getAsJsonArray("reviews")?.forEach { rEl ->
                    val r = rEl.asJsonObject
                    val text = r.getAsJsonObject("originalText")?.get("text")?.asString
                        ?: r.getAsJsonObject("text")?.get("text")?.asString
                        ?: ""
                    val author = r.getAsJsonObject("authorAttribution")?.get("displayName")?.asString
                        ?: "Google User"
                    val rating = r.get("rating")?.asDouble ?: 0.0
                    val when_ = r.get("relativePublishTimeDescription")?.asString ?: "Recently"
                    if (text.isNotBlank()) reviews.add(GoogleReview(author, rating, text, when_))
                }

                // Take up to 3 photos, ordered so the hero slot gets one whose
                // aspect ratio won't be sliced badly by a 16:9 crop. Google
                // returns mixed orientations — portrait shots in a landscape box
                // were the cause of weird framing on roadside stalls.
                val photos = obj.getAsJsonArray("photos")
                    ?.mapNotNull { it.asJsonObject }
                    ?.sortedByDescending { photo ->
                        val w = photo.get("widthPx")?.asInt ?: 0
                        val h = photo.get("heightPx")?.asInt ?: 0
                        if (w <= 0 || h <= 0) 0.0
                        else {
                            val ratio = w.toDouble() / h.toDouble()
                            // Distance from 16:9 (1.78) — closer is better.
                            1.0 / (1.0 + Math.abs(ratio - 1.78))
                        }
                    }
                    ?.mapNotNull { photo ->
                        photo.get("name")?.asString?.let { photoMediaUrl(it, key, 1200) }
                    }
                    ?.take(3)
                    ?: emptyList()

                val photoUrl = photos.firstOrNull()

                // Google always returns weekdayDescriptions starting at Monday,
                // so firstOrNull() made the card always show "Monday: ..."
                // regardless of the real day. Pick today's entry instead.
                val hours = obj.getAsJsonObject("regularOpeningHours")
                    ?.getAsJsonArray("weekdayDescriptions")
                    ?.let { arr -> pickTodayHours(arr.mapNotNull { it.asString }) }
                    ?: obj.getAsJsonObject("currentOpeningHours")
                        ?.getAsJsonArray("weekdayDescriptions")
                        ?.let { arr -> pickTodayHours(arr.mapNotNull { it.asString }) }
                    ?: "Hours unavailable"

                PlaceDetailsResult.Success(reviews, hours, photoUrl, photos)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Details network error: ${e.message}", e)
            PlaceDetailsResult.Error(e.message ?: "Network error.")
        }
    }

    // ---------------------------------------------------------------- mapping

    private fun mapPlace(obj: JsonObject, userLat: Double, userLng: Double, key: String): Restaurant? {
        val id = obj.get("id")?.asString ?: return null
        val name = obj.getAsJsonObject("displayName")?.get("text")?.asString ?: "Unknown place"

        // Drop non-food places that only matched because Google's includedTypes
        // filter checks the whole `types` array (e.g. a sports complex containing
        // a cafe, or a hotel with a restaurant inside).
        val primaryType = obj.get("primaryType")?.asString
        if (!isFoodPrimaryType(primaryType)) {
            Log.d(TAG, "Dropping non-food place '$name' (primaryType=$primaryType)")
            return null
        }

        val loc = obj.getAsJsonObject("location")
        val lat = loc?.get("latitude")?.asDouble ?: userLat
        val lng = loc?.get("longitude")?.asDouble ?: userLng

        val address = obj.get("shortFormattedAddress")?.asString
            ?: obj.get("formattedAddress")?.asString
            ?: ""

        val rating = obj.get("rating")?.asDouble ?: 0.0
        val ratingCount = obj.get("userRatingCount")?.asInt ?: 0

        val priceLevel = obj.get("priceLevel")?.asString
        val (minP, maxP) = priceLevel.toPriceRange()

        val openNow = obj.getAsJsonObject("currentOpeningHours")?.get("openNow")?.asBoolean
        // null means Google does not know. Never report unknown as open — that
        // previously caused places to show as "Open now" at 11pm.
        val hoursText = when (openNow) {
            true -> "Open now"
            false -> "Closed now"
            null -> "Hours unavailable"
        }

        val types = obj.getAsJsonArray("types")?.mapNotNull { it.asString } ?: emptyList()

        // Thumbnail only. 400px is plenty for a 120dp list image, and every
        // Place Photo fetch is a separately billed request, so we keep it small.
        // The full-resolution photo is fetched separately by fetchPlaceDetails().
        val thumbnailUrl = obj.getAsJsonArray("photos")
            ?.firstOrNull()
            ?.asJsonObject
            ?.get("name")
            ?.asString
            ?.let { photoMediaUrl(it, key, 400) }

        val category = when {
            minP >= 40 -> "Fancy"
            maxP <= 20 -> "Budget"
            "cafe" in types || "coffee_shop" in types -> "Cafe"
            else -> "Local Food"
        }

        return Restaurant(
            id = id,
            name = name,
            photoUrl = thumbnailUrl,
            imageResUrl = thumbnailUrl ?: "",
            distanceKm = LocationHelper.calculateDistanceKm(userLat, userLng, lat, lng),
            minPrice = minP,
            maxPrice = maxP,
            openingHours = hoursText,
            isOpenNow = openNow,
            latitude = lat,
            longitude = lng,
            category = category,
            isAdventure = false,
            rating = rating,
            userRatingsTotal = ratingCount,
            address = address,
            priceLevel = priceLevel
        )
    }

    private fun String?.toPriceRange(): Pair<Int, Int> = when (this) {
        "PRICE_LEVEL_FREE" -> 0 to 5
        "PRICE_LEVEL_INEXPENSIVE" -> 1 to 15
        "PRICE_LEVEL_MODERATE" -> 15 to 35
        "PRICE_LEVEL_EXPENSIVE" -> 40 to 90
        "PRICE_LEVEL_VERY_EXPENSIVE" -> 100 to 250
        else -> 5 to 20
    }

    /**
     * Builds a working image URL for a Places v1 photo resource name.
     * The key goes in the query string here (the /media endpoint does not
     * accept X-Goog-Api-Key for <img> loading).
     */
    fun photoMediaUrl(photoName: String, key: String, maxWidthPx: Int): String =
        "$BASE/$photoName/media?maxWidthPx=$maxWidthPx&key=$key"

    /**
     * Picks today's entry out of Google's weekdayDescriptions array.
     *
     * Google always orders that array Monday-first regardless of the current
     * day, and each entry looks like "Monday: 9:00 AM – 10:00 PM". We match the
     * prefix against today's local day name in Malaysia and strip it, returning
     * just the hours (e.g. "9:00 AM – 10:00 PM").
     */
    private fun pickTodayHours(descriptions: List<String>): String? {
        if (descriptions.isEmpty()) return null

        val today = LocalDate.now(ZoneId.of("Asia/Kuala_Lumpur"))
            .dayOfWeek
            .getDisplayName(TextStyle.FULL, Locale.ENGLISH)

        val match = descriptions.firstOrNull { it.startsWith("$today:", ignoreCase = true) }
            ?: return null

        return match.substringAfter(':', match).trim()
    }

    private fun parseError(text: String, code: Int): String = try {
        val obj = gson.fromJson(text, JsonObject::class.java)
        val msg = obj.getAsJsonObject("error")?.get("message")?.asString
        msg ?: "HTTP $code"
    } catch (_: Exception) {
        "HTTP $code"
    }
}

/** Result wrapper so callers never silently receive fake data. */
sealed interface PlacesResult {
    data class Success(val places: List<Restaurant>) : PlacesResult
    data class Error(val message: String) : PlacesResult
}

sealed interface PlaceDetailsResult {
    data class Success(
        val reviews: List<GoogleReview>,
        val formattedHours: String,
        val fullPhotoUrl: String?,
        /** Up to 3 photo URLs, hero-optimised first. */
        val photoUrls: List<String> = emptyList()
    ) : PlaceDetailsResult
    data class Error(val message: String) : PlaceDetailsResult
}