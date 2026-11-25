package com.spg.nakmakanapa

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.spg.nakmakanapa.data.DeepSeekApiService
import com.spg.nakmakanapa.data.GooglePlacesApiService
import com.spg.nakmakanapa.data.PlaceDetailsResult
import com.spg.nakmakanapa.data.PlacesResult
import com.spg.nakmakanapa.data.SavedRepository
import com.spg.nakmakanapa.data.SurpriseRepository
import com.spg.nakmakanapa.data.UserProfileRepository
import com.spg.nakmakanapa.location.LocationHelper
import com.spg.nakmakanapa.model.DebugState
import com.spg.nakmakanapa.model.FoodCategory
import com.spg.nakmakanapa.model.GoogleReview
import com.spg.nakmakanapa.model.QuickFilter
import com.spg.nakmakanapa.model.Restaurant
import com.spg.nakmakanapa.model.SurpriseQuota
import com.spg.nakmakanapa.model.UserProfile
import com.spg.nakmakanapa.ui.components.BottomNavBar
import com.spg.nakmakanapa.ui.screens.*
import com.spg.nakmakanapa.ui.theme.DarkGreenHeader
import com.spg.nakmakanapa.ui.theme.NakMakanApaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private lateinit var savedRepository: SavedRepository
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var surpriseRepository: SurpriseRepository
    private val deepSeekApiService = DeepSeekApiService()
    private val placesApi = GooglePlacesApiService()

    private var currentLat by mutableStateOf(LocationHelper.KUALA_LUMPUR.lat)
    private var currentLng by mutableStateOf(LocationHelper.KUALA_LUMPUR.lng)
    private var currentLocationName by mutableStateOf(LocationHelper.KUALA_LUMPUR.name)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) fetchDeviceLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedRepository = SavedRepository(applicationContext)
        userProfileRepository = UserProfileRepository(applicationContext)
        surpriseRepository = SurpriseRepository(applicationContext)
        checkLocationPermissions()

        setContent {
            NakMakanApaTheme {
                val savedList by savedRepository.savedRestaurants.collectAsState(initial = emptyList())
                val savedIds = remember(savedList) { savedList.map { it.id }.toSet() }
                val profile by userProfileRepository.userProfile.collectAsState(initial = UserProfile())
                val surpriseQuota by surpriseRepository.quota.collectAsState(initial = SurpriseQuota(0, SurpriseRepository.DAILY_LIMIT, 0, 0))

                var route by remember { mutableStateOf("landing") }

                // Navigation stack so the Android back button / gesture walks back
                // through screens instead of finishing the activity.
                val backStack = remember { mutableStateListOf<String>() }

                val navigateTo: (String) -> Unit = { newRoute ->
                    if (newRoute != route) {
                        backStack.add(route)
                        route = newRoute
                    }
                }

                // Bottom-nav tabs are siblings, not a hierarchy. Switching tabs must
                // NOT stack up. Previously every tab tap pushed onto backStack, so
                // back replayed Home → Adventure → Home … before finally landing on
                // the start screen.
                val navigateTab: (String) -> Unit = { newRoute ->
                    if (newRoute != route) {
                        backStack.clear()
                        route = newRoute
                    }
                }

                val navigateBack: () -> Boolean = {
                    if (backStack.isNotEmpty()) {
                        route = backStack.removeAt(backStack.lastIndex)
                        true
                    } else false
                }

                BackHandler(enabled = route != "landing") {
                    if (!navigateBack()) route = "landing"
                }
                var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
                var aiReason by remember { mutableStateOf<String?>(null) }
                var aiSummary by remember { mutableStateOf<String?>(null) }
                var reviews by remember { mutableStateOf<List<GoogleReview>>(emptyList()) }

                var isSurpriseLoading by remember { mutableStateOf(false) }
                // Non-null when the last Surprise Me attempt found nothing that
                // matched the user's criteria. Shows an explanatory dialog.
                var surpriseEmptyReason by remember { mutableStateOf<String?>(null) }
                var showLocationDialog by remember { mutableStateOf(false) }
                var showBudgetDialog by remember { mutableStateOf(false) }
                var budgetLimit by remember { mutableStateOf(15) }

                // -------- home + category state
                var selectedCategory by remember { mutableStateOf<FoodCategory?>(null) }
                // Which quick-filter chip is active. Each one drives its own query.
                var quickFilter by remember { mutableStateOf<QuickFilter?>(null) }
                var homeResults by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
                var homeLoading by remember { mutableStateOf(true) }
                var homeError by remember { mutableStateOf<String?>(null) }

                // -------- search across Google (not just the loaded 10)
                // Billed per request, so this is fired deliberately (Enter / search
                // button), never on a typing pause. One keystroke sequence = 1 call.
                var searchQuery by remember { mutableStateOf("") }
                var submittedQuery by remember { mutableStateOf("") }
                var searchResults by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
                var searchLoading by remember { mutableStateOf(false) }
                var searchError by remember { mutableStateOf<String?>(null) }

                // Session cache: repeating a search costs nothing.
                val searchCache = remember { mutableStateMapOf<String, List<Restaurant>>() }

                // -------- adventure state
                var adventureResults by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
                var adventureLoading by remember { mutableStateOf(false) }
                var adventureError by remember { mutableStateOf<String?>(null) }

                // Reload trigger for the retry button
                var reloadToken by remember { mutableStateOf(0) }

                // -------------------------------------------- HOME loading
                LaunchedEffect(currentLat, currentLng, selectedCategory, quickFilter, budgetLimit, reloadToken) {
                    homeLoading = true
                    homeError = null
                    val cat = selectedCategory

                    // Each chip issues its own query so results aren't limited to
                    // whatever happened to be in the initial 10.
                    val result = when (quickFilter) {
                        QuickFilter.FANCY -> placesApi.searchFancy(
                            lat = currentLat,
                            lng = currentLng
                        )

                        QuickFilter.BUDGET -> placesApi.searchBudget(
                            lat = currentLat,
                            lng = currentLng,
                            maxPricePerPerson = budgetLimit
                        )

                        QuickFilter.NEARBY -> placesApi.searchNearby(
                            lat = currentLat,
                            lng = currentLng,
                            radiusMeters = 5000,
                            includedTypes = cat?.includedTypes ?: listOf("restaurant")
                        )

                        null -> if (cat == null || cat.id == "all") {
                            placesApi.searchNearby(
                                lat = currentLat,
                                lng = currentLng,
                                radiusMeters = 8000,
                                includedTypes = listOf("restaurant")
                            )
                        } else if (cat.searchQuery != null) {
                            placesApi.searchText(
                                query = cat.searchQuery,
                                lat = currentLat,
                                lng = currentLng,
                                radiusMeters = 10000
                            )
                        } else {
                            placesApi.searchNearby(
                                lat = currentLat,
                                lng = currentLng,
                                radiusMeters = 8000,
                                includedTypes = cat.includedTypes
                            )
                        }
                    }

                    when (result) {
                        is PlacesResult.Success -> {
                            homeResults = result.places
                            if (result.places.isEmpty()) {
                                homeError = when (quickFilter) {
                                    QuickFilter.FANCY -> "No fine dining spots nearby. Fancy restaurants are sparse — try a wider area or a bigger city."
                                    QuickFilter.BUDGET -> "Nothing under RM$budgetLimit nearby. Try raising your budget."
                                    QuickFilter.NEARBY -> "No restaurants within 5 km. Try a different area."
                                    null -> "Google returned 0 places for this area and category."
                                }
                            }
                        }
                        is PlacesResult.Error -> {
                            homeResults = emptyList()
                            homeError = result.message
                        }
                    }
                    homeLoading = false
                }

                // ------------------------------------- SEARCH (on submit only)
                // Fires only when `submittedQuery` changes — i.e. the user pressed
                // Enter / the search icon. Typing alone costs nothing.
                LaunchedEffect(submittedQuery, currentLat, currentLng) {
                    val q = submittedQuery.trim()
                    if (q.length < 2) {
                        searchResults = emptyList()
                        searchError = null
                        searchLoading = false
                        return@LaunchedEffect
                    }

                    // Session cache hit — no API call.
                    val cacheKey = "$q|$currentLat|$currentLng"
                    searchCache[cacheKey]?.let {
                        searchResults = it
                        searchError = null
                        searchLoading = false
                        return@LaunchedEffect
                    }

                    searchLoading = true
                    searchError = null

                    when (val r = placesApi.searchText(
                        query = q,
                        lat = currentLat,
                        lng = currentLng,
                        radiusMeters = 10000,
                        maxResults = 10
                    )) {
                        is PlacesResult.Success -> {
                            searchResults = r.places
                            searchCache[cacheKey] = r.places
                        }
                        is PlacesResult.Error -> {
                            searchResults = emptyList()
                            searchError = r.message
                        }
                    }
                    searchLoading = false
                }

                // ---------------------------------------- ADVENTURE loading
                // Only fetch once per location, NOT on every tab switch.
                // Previously "route" was a key, so each visit re-ran a billed Text Search.
                LaunchedEffect(currentLat, currentLng, reloadToken) {
                    adventureLoading = true
                    adventureError = null

                    val scenic = placesApi.searchText(
                        query = "scenic countryside restaurant viewpoint hill forest",
                        lat = currentLat,
                        lng = currentLng,
                        radiusMeters = 40000,
                        maxResults = 10
                    )

                    when (scenic) {
                        is PlacesResult.Success -> adventureResults = scenic.places.map {
                            it.copy(isAdventure = true)
                        }
                        is PlacesResult.Error -> {
                            adventureResults = emptyList()
                            adventureError = scenic.message
                        }
                    }
                    adventureLoading = false
                }

                // Open details + ask DeepSeek to summarise real Google reviews
                val openRestaurant: (Restaurant, String?) -> Unit = { rest, reason ->
                    selectedRestaurant = rest
                    aiReason = reason
                    aiSummary = null
                    reviews = emptyList()
                    // Push onto the stack so Back returns to whichever list we
                    // came from. Assigning route directly meant Back had nothing to
                    // pop and fell through to the landing screen.
                    navigateTo("detail")

                    lifecycleScope.launch {
                        when (val d = placesApi.fetchPlaceDetails(rest.id)) {
                            is PlaceDetailsResult.Success -> {
                                reviews = d.reviews
                                selectedRestaurant = rest.copy(
                                    photoUrl = d.fullPhotoUrl ?: rest.photoUrl,
                                    photoUrls = d.photoUrls,
                                    imageResUrl = d.fullPhotoUrl ?: rest.photoUrl ?: "",
                                    openingHours = d.formattedHours.ifBlank { rest.openingHours }
                                )
                                if (d.reviews.isNotEmpty()) {
                                    aiSummary = deepSeekApiService.summarizeGoogleReviews(rest.name, d.reviews)
                                }
                            }
                            is PlaceDetailsResult.Error -> {
                                reviews = emptyList()
                            }
                        }
                    }
                }

                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                when (route) {
                                    "landing" -> LandingScreen(
                                        onJomMakanClick = { route = "home" }
                                    )

                                    "home" -> MainMenuScreen(
                                        restaurants = homeResults,
                                        locationName = currentLocationName,
                                        selectedCategory = selectedCategory,
                                        isLoading = homeLoading,
                                        errorMessage = homeError,
                                        budgetLimit = budgetLimit,
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        onSearchSubmit = { submittedQuery = searchQuery },
                                        searchSubmitted = submittedQuery,
                                        searchResults = searchResults,
                                        searchLoading = searchLoading,
                                        searchError = searchError,
                                        onSelectCategory = {
                                            quickFilter = null
                                            selectedCategory = it
                                        },
                                        onRestaurantClick = { openRestaurant(it, null) },
                                        onLocationClick = { showLocationDialog = true },
                                        onBudgetClick = { showBudgetDialog = true },
                                        activeFilter = quickFilter,
                                        onQuickFilterChange = { filter ->
                                            quickFilter = if (quickFilter == filter) null else filter
                                        },
                                        onBack = { navigateBack() },
                                        onRetry = { reloadToken++ }
                                    )

                                    "adventure" -> AdventureScreen(
                                        adventureRestaurants = adventureResults,
                                        isLoading = adventureLoading,
                                        errorMessage = adventureError,
                                        onAdventureClick = { openRestaurant(it, null) },
                                        onBack = { navigateBack() },
                                        onRetry = { reloadToken++ }
                                    )

                                    "saved" -> SavedListScreen(
                                        savedRestaurants = savedList,
                                        onRemoveSaved = { id ->
                                            lifecycleScope.launch { savedRepository.removeSaved(id) }
                                        },
                                        onRestaurantClick = { openRestaurant(it, null) },
                                        onBack = { navigateBack() }
                                    )

                                    "profile" -> ProfileScreen(
                                        userProfile = profile,
                                        debugState = DebugState(
                                            surpriseUsedToday = surpriseQuota.usedToday,
                                            surpriseDailyLimit = surpriseQuota.dailyLimit,
                                            cooldownActive = surpriseQuota.hasCooldown,
                                            pickHistoryCount = surpriseQuota.historyCount
                                        ),
                                        onResetSurpriseLimit = {
                                            lifecycleScope.launch {
                                                surpriseRepository.resetLimitAndCooldown()
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Surprise Me limit reset",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onClearPickHistory = {
                                            lifecycleScope.launch {
                                                surpriseRepository.clearHistory()
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Pick history cleared",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onUpdateProfile = { n, b, r, d, m ->
                                            lifecycleScope.launch {
                                                userProfileRepository.updateProfile(n, b, r, d, m)
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Profile updated",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )

                                    "detail" -> selectedRestaurant?.let { rest ->
                                        RestaurantDetailScreen(
                                            restaurant = rest,
                                            isSaved = savedIds.contains(rest.id),
                                            onToggleSave = {
                                                lifecycleScope.launch {
                                                    savedRepository.toggleSave(rest)
                                                }
                                            },
                                            aiReasonNote = aiReason,
                                            aiReviewSummary = aiSummary,
                                            googleReviews = reviews
                                        )
                                    } ?: run { navigateTo("home") }
                                }
                            }

                            if (route in listOf("home", "adventure", "saved", "profile", "detail")) {
                                BottomNavBar(
                                    currentRoute = route,
                                    onNavigate = { navigateTab(it) },
                                    onSurpriseMeClick = {
                                        if (surpriseQuota.hasCooldown) {
                                            Toast.makeText(
                                                applicationContext,
                                                "Just a moment — ${surpriseQuota.cooldownSeconds}s cooldown",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@BottomNavBar
                                        }
                                        if (surpriseQuota.exhausted) {
                                            Toast.makeText(
                                                applicationContext,
                                                "Daily limit reached (${surpriseQuota.dailyLimit}). Try tomorrow!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@BottomNavBar
                                        }
                                        if (homeResults.isEmpty()) {
                                            Toast.makeText(
                                                applicationContext,
                                                "Load some places first",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@BottomNavBar
                                        }

                                        isSurpriseLoading = true
                                        lifecycleScope.launch {
                                            // 1) Filter to what the user can actually afford AND
                                            //    what is plausibly open right now.
                                            //    NOTE: no `.ifEmpty { homeResults }` fallback —
                                            //    that silently ignored the user's criteria and is
                                            //    how an unrelated venue surfaced at 2am.
                                            val pool = homeResults.filter {
                                                it.distanceKm <= profile.maxRadiusKm &&
                                                        it.minPrice <= profile.maxBudget &&
                                                        it.isOpenNow != false
                                            }

                                            if (pool.isEmpty()) {
                                                isSurpriseLoading = false
                                                surpriseEmptyReason = buildNoMatchMessage(
                                                    allCount = homeResults.size,
                                                    profile = profile
                                                )
                                                return@launch
                                            }

                                            // 2) Deciding is done HERE, not by DeepSeek.
                                            val history = surpriseRepository.pickHistory.first()
                                            val chosen = surpriseRepository.pick(pool, history)

                                            if (chosen == null) {
                                                isSurpriseLoading = false
                                                surpriseEmptyReason = buildNoMatchMessage(
                                                    allCount = homeResults.size,
                                                    profile = profile
                                                )
                                                return@launch
                                            }

                                            // 3) DeepSeek only writes the pitch.
                                            val recentNames = history.takeLast(3).map { it.name }
                                            val reason = deepSeekApiService.writeSurpriseReason(
                                                chosen = chosen,
                                                userProfile = profile,
                                                recentPicks = recentNames
                                            )

                                            surpriseRepository.recordPick(chosen, reason)
                                            isSurpriseLoading = false
                                            openRestaurant(chosen, reason)
                                        }
                                    }
                                )
                            }
                        }

                        if (isSurpriseLoading) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkGreenHeader),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Column(
                                        Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = Color.White)
                                        Spacer(Modifier.height(20.dp))
                                        Text(
                                            "DeepSeek AI is picking a surprise restaurant...",
                                            color = Color.White,
                                            fontFamily = FontFamily.Cursive,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Under RM${profile.maxBudget} & within ${profile.maxRadiusKm.toInt()} km",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // ------------------------------------ NO-MATCH dialog
                        if (surpriseEmptyReason != null) {
                            AlertDialog(
                                onDismissRequest = { surpriseEmptyReason = null },
                                title = { Text("No surprise today", fontWeight = FontWeight.Bold) },
                                text = { Text(surpriseEmptyReason!!, fontSize = 13.sp) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        surpriseEmptyReason = null
                                        showBudgetDialog = true
                                    }) { Text("Change budget") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { surpriseEmptyReason = null }) { Text("OK") }
                                }
                            )
                        }

                        // -------------------------------------- BUDGET dialog
                        if (showBudgetDialog) {
                            var budgetInput by remember { mutableStateOf(budgetLimit.toString()) }

                            AlertDialog(
                                onDismissRequest = { showBudgetDialog = false },
                                title = { Text("Set Budget", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column {
                                        Text(
                                            "Show only places whose starting price is at or below your limit.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        OutlinedTextField(
                                            value = budgetInput,
                                            onValueChange = { budgetInput = it.filter { c -> c.isDigit() } },
                                            label = { Text("Max price per person (RM)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(5, 10, 15, 30, 50).forEach { preset ->
                                                AssistChip(
                                                    onClick = { budgetInput = preset.toString() },
                                                    label = { Text("RM$preset", fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        budgetLimit = budgetInput.toIntOrNull()?.coerceIn(1, 999) ?: budgetLimit
                                        showBudgetDialog = false
                                    }) { Text("Apply") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") }
                                }
                            )
                        }

                        if (showLocationDialog) {
                            AlertDialog(
                                onDismissRequest = { showLocationDialog = false },
                                title = { Text("Choose Location", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column {
                                        Text(
                                            "Emulators default to the USA. Pick a Malaysian city:",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.height(220.dp)
                                        ) {
                                            items(LocationHelper.PRESET_LOCATIONS) { city ->
                                                val sel = currentLocationName == city.name
                                                Row(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            currentLat = city.lat
                                                            currentLng = city.lng
                                                            currentLocationName = city.name
                                                            showLocationDialog = false
                                                        }
                                                        .padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.LocationOn, null,
                                                            tint = DarkGreenHeader
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            city.name,
                                                            fontSize = 15.sp,
                                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                    if (sel) Icon(
                                                        Icons.Default.Check, null,
                                                        tint = DarkGreenHeader
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        fetchDeviceLocation()
                                        showLocationDialog = false
                                    }) { Text("Use Device GPS") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showLocationDialog = false }) { Text("Close") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Explains why Surprise Me found nothing, and what to change.
     * Without this the button just looked broken.
     */
    private fun buildNoMatchMessage(allCount: Int, profile: UserProfile): String {
        val zone = ZoneId.of("Asia/Kuala_Lumpur")
        val now = ZonedDateTime.now(zone)
        val hour = now.hour
        val isLate = hour >= 22 || hour <= 5

        val reasons = buildList {
            add("No restaurant matched all your criteria right now.")
            add("")
            add("• Budget: under RM${profile.maxBudget} per person")
            add("• Distance: within ${profile.maxRadiusKm.toInt()} km")
            add("• Must be open at ${now.format(DateTimeFormatter.ofPattern("h:mm a"))}")
            add("")
            when {
                isLate -> add("It is late, so most places nearby are closed. Try raising your budget or distance, or search again in the morning.")
                profile.maxBudget < 15 -> add("Your budget is quite tight for this area. Try increasing it a little.")
                else -> add("Try a wider distance, a higher budget, or search in a different area.")
            }
            if (allCount == 0) {
                add("")
                add("(Google returned no places at all for this location.)")
            }
        }

        return reasons.joinToString("\n")
    }

    private fun checkLocationPermissions() {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) fetchDeviceLocation()
        else locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun fetchDeviceLocation() {
        try {
            LocationHelper.getFusedLocationClient(this).lastLocation
                .addOnSuccessListener { loc ->
                    if (loc == null) return@addOnSuccessListener
                    if (LocationHelper.isLocationInMalaysia(loc.latitude, loc.longitude)) {
                        currentLat = loc.latitude
                        currentLng = loc.longitude
                        currentLocationName = "Current Device GPS"
                    } else {
                        // Emulator default (Mountain View) -> fall back to KL
                        currentLat = LocationHelper.KUALA_LUMPUR.lat
                        currentLng = LocationHelper.KUALA_LUMPUR.lng
                        currentLocationName = LocationHelper.KUALA_LUMPUR.name
                    }
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}