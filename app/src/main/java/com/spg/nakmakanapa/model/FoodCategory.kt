package com.spg.nakmakanapa.model

/**
 * A food category chip on the Home screen.
 *
 * [includedTypes] maps to Google Places API v1 `includedTypes`
 * (e.g. "restaurant", "cafe", "bakery", "bar").
 * [searchQuery] is used with :searchText for cuisine-specific chips
 * (e.g. "mamak", "nasi lemak") because Places has no "mamak" type.
 */
data class FoodCategory(
    val id: String,
    val name: String,
    val includedTypes: List<String>,
    val searchQuery: String?
) {
    companion object {
        /**
         * Google Places (New) Table A types relevant to food:
         * restaurant, cafe, bakery, bar, meal_takeaway, meal_delivery, food_court,
         * fast_food_restaurant, hamburger_restaurant
         */
        private const val RESTAURANT = "restaurant"
        private const val CAFE = "cafe"
        private const val BAKERY = "bakery"
        private const val FAST_FOOD = "fast_food_restaurant"
        private const val HAMBURGER = "hamburger_restaurant"

        val categories: List<FoodCategory> = listOf(
            FoodCategory("all", "All", listOf(RESTAURANT), null),
            FoodCategory("mamak", "Mamak", listOf(RESTAURANT), "mamak roti canai nasi kandar"),
            FoodCategory("nasi_lemak", "Nasi Lemak", listOf(RESTAURANT), "nasi lemak"),
            FoodCategory("fast_food", "Fast Food", listOf(FAST_FOOD, HAMBURGER), "fast food"),
            FoodCategory("cafe", "Cafes", listOf(CAFE), "cafe coffee"),
            FoodCategory("seafood", "Seafood", listOf(RESTAURANT), "seafood ikan bakar"),
            FoodCategory("burger", "Burgers", listOf(HAMBURGER, FAST_FOOD), "burger"),
            FoodCategory("japanese", "Japanese", listOf(RESTAURANT), "japanese sushi ramen"),
            FoodCategory("dessert", "Desserts", listOf(BAKERY, CAFE), "dessert ice cream"),
            FoodCategory("chinese", "Chinese", listOf(RESTAURANT), "chinese dim sum"),
            FoodCategory("western", "Western", listOf(RESTAURANT), "western steak pasta")
        )
    }
}