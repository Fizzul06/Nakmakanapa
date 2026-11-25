package com.spg.nakmakanapa.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.spg.nakmakanapa.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userProfileDataStore by preferencesDataStore(name = "user_profile_prefs")

class UserProfileRepository(private val context: Context) {

    private val userIdKey = stringPreferencesKey("user_id")
    private val usernameKey = stringPreferencesKey("username")
    private val maxBudgetKey = intPreferencesKey("max_budget")
    private val maxRadiusKey = doublePreferencesKey("max_radius")
    private val dietaryKey = stringPreferencesKey("dietary")
    private val customMoodKey = stringPreferencesKey("custom_mood")

    val userProfile: Flow<UserProfile> = context.userProfileDataStore.data.map { prefs ->
        UserProfile(
            userId = prefs[userIdKey] ?: "123456789",
            username = prefs[usernameKey] ?: "Tuan",
            maxBudget = prefs[maxBudgetKey] ?: 20,
            maxRadiusKm = prefs[maxRadiusKey] ?: 10.0,
            dietaryRestrictions = prefs[dietaryKey] ?: "Halal",
            customMoodPrompt = prefs[customMoodKey] ?: "Hungry for something local and delicious!"
        )
    }

    suspend fun updateProfile(
        username: String,
        maxBudget: Int,
        maxRadiusKm: Double,
        dietary: String,
        customMood: String
    ) {
        context.userProfileDataStore.edit { prefs ->
            prefs[usernameKey] = username
            prefs[maxBudgetKey] = maxBudget
            prefs[maxRadiusKey] = maxRadiusKm
            prefs[dietaryKey] = dietary
            prefs[customMoodKey] = customMood
        }
    }
}