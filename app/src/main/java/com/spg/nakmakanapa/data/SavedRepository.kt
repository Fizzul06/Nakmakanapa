package com.spg.nakmakanapa.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.spg.nakmakanapa.model.Restaurant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "saved_restaurants_prefs")

/**
 * Persists saved restaurants.
 *
 * Previously only the *IDs* were stored, so a saved place could only be shown
 * if it happened to be present in the current in-memory search results. Saving
 * from a category search then visiting Saved showed nothing. We now store the
 * full [Restaurant] as JSON so saved entries are always renderable.
 */
class SavedRepository(private val context: Context) {

    private val savedJsonKey = stringPreferencesKey("saved_restaurants_json")

    // Legacy ID-only key. Read once during migration, then removed.
    private val legacyIdsKey = stringSetPreferencesKey("saved_restaurant_ids")

    private val gson = Gson()

    val savedRestaurants: Flow<List<Restaurant>> = context.dataStore.data.map { prefs ->
        val json = prefs[savedJsonKey]
        if (json.isNullOrBlank()) emptyList()
        else runCatching {
            gson.fromJson(json, Array<Restaurant>::class.java).toList()
        }.getOrDefault(emptyList())
    }

    /** Convenience view of just the ids, for bookmark-button state. */
    val savedRestaurantIds: Flow<Set<String>> = savedRestaurants.map { list ->
        list.map { it.id }.toSet()
    }

    suspend fun toggleSave(restaurant: Restaurant) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs[savedJsonKey]).toMutableList()
            val idx = current.indexOfFirst { it.id == restaurant.id }
            if (idx >= 0) current.removeAt(idx) else current.add(restaurant)
            prefs[savedJsonKey] = gson.toJson(current)
        }
    }

    suspend fun removeSaved(restaurantId: String) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs[savedJsonKey]).filterNot { it.id == restaurantId }
            prefs[savedJsonKey] = gson.toJson(current)
            // Drop the stale legacy key so old fake ids stop reappearing.
            prefs.remove(legacyIdsKey)
        }
    }

    private fun decode(json: String?): List<Restaurant> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson(json, Array<Restaurant>::class.java).toList()
        }.getOrDefault(emptyList())
    }
}