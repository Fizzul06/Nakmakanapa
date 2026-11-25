package com.spg.nakmakanapa.model

data class UserProfile(
    val userId: String = "123456789",
    val username: String = "Tuan",
    val maxBudget: Int = 20,
    val maxRadiusKm: Double = 10.0,
    val preferredCategories: List<String> = listOf("Mamak", "Malay", "Spicy", "Fast Food"),
    val dietaryRestrictions: String = "Halal",
    val customMoodPrompt: String = "Hungry for something delicious and satisfying!"
)