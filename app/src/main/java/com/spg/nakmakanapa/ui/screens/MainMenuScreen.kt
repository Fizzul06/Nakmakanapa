package com.spg.nakmakanapa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spg.nakmakanapa.model.FoodCategory
import com.spg.nakmakanapa.model.QuickFilter
import com.spg.nakmakanapa.model.Restaurant
import com.spg.nakmakanapa.ui.theme.DarkGreenHeader
import com.spg.nakmakanapa.ui.theme.LightGrayBackground
import com.spg.nakmakanapa.ui.theme.TextPrimary
import com.spg.nakmakanapa.ui.theme.TextSecondary
import com.spg.nakmakanapa.ui.theme.YellowButton

@Composable
fun MainMenuScreen(
    restaurants: List<Restaurant>,
    locationName: String,
    selectedCategory: FoodCategory?,
    isLoading: Boolean,
    errorMessage: String?,
    budgetLimit: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    searchSubmitted: String,
    searchResults: List<Restaurant>,
    searchLoading: Boolean,
    searchError: String?,
    onSelectCategory: (FoodCategory) -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
    onLocationClick: () -> Unit,
    onBudgetClick: () -> Unit,
    activeFilter: QuickFilter?,
    onQuickFilterChange: (QuickFilter) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    // Google results are shown only for a *submitted* query. While the user is
    // still typing we filter the loaded list locally and instantly (free).
    val isSearching = searchSubmitted.trim().length >= 2

    val source: List<Restaurant> = if (isSearching) searchResults else restaurants

    // Sorted closest-first, then filtered.
    // NOTE: the quick-filter chips now each issue their own Google query, so the
    // list arriving here is already the right shape. Only the text search needs
    // to narrow it further.
    val visible = remember(searchQuery, source) {
        source
            .sortedBy { it.distanceKm }
            .filter { r ->
                searchQuery.isBlank() ||
                        r.name.contains(searchQuery, ignoreCase = true) ||
                        r.address.contains(searchQuery, ignoreCase = true)
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGrayBackground),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // ---------------------------------------------------------- header
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1200&q=80")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color.Black.copy(alpha = 0.4f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Back + location row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.clickable { onLocationClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn, null,
                                    tint = YellowButton,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Location: $locationName",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        trailingIcon = {
                            IconButton(
                                onClick = { onSearchSubmit() },
                                enabled = searchQuery.trim().length >= 2
                            ) {
                                Icon(
                                    Icons.Default.Search, null,
                                    tint = if (searchQuery.trim().length >= 2) DarkGreenHeader else TextSecondary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                        placeholder = { Text("Search food or place...", color = TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = TextSecondary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(32.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = DarkGreenHeader,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }
        }

        // --------------------------------------------------- quick filters
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Budget: tapping the chip toggles the query, the pencil icon
                // opens the amount picker.
                QuickFilterCard(
                    title = "Under\nRM$budgetLimit",
                    selected = activeFilter == QuickFilter.BUDGET,
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickFilterChange(QuickFilter.BUDGET) },
                    trailingIcon = Icons.Default.Edit,
                    onTrailingClick = onBudgetClick
                )
                QuickFilterCard(
                    title = "Within\n5 KM",
                    selected = activeFilter == QuickFilter.NEARBY,
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickFilterChange(QuickFilter.NEARBY) }
                )
                QuickFilterCard(
                    title = "Fancy",
                    selected = activeFilter == QuickFilter.FANCY,
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickFilterChange(QuickFilter.FANCY) }
                )
            }
        }

        // ---------------------------------------------------- category chips
        item {
            Spacer(Modifier.height(24.dp))
            Text(
                "Trendy Categories",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        items(FoodCategory.categories.chunked(4)) { rowCats ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (cat in rowCats) {
                    Box(Modifier.weight(1f)) {
                        CategoryChip(
                            category = cat,
                            isSelected = selectedCategory?.id == cat.id,
                            onClick = { onSelectCategory(cat) }
                        )
                    }
                }
                repeat(4 - rowCats.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // ------------------------------------------------- results header
        item {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        isSearching -> "Results for \"${searchSubmitted.trim()}\""
                        selectedCategory == null || selectedCategory.id == "all" -> "Nearby Places"
                        else -> "Nearby ${selectedCategory.name}"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (isLoading || searchLoading) {
                    CircularProgressIndicator(
                        color = DarkGreenHeader,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ------------------------------------------ error / empty / list
        if (searchError != null && isSearching) {
            item { ErrorCard(message = searchError, onRetry = onRetry) }
        } else if (errorMessage != null) {
            item {
                ErrorCard(message = errorMessage, onRetry = onRetry)
            }
        } else if (!isLoading && !searchLoading && visible.isEmpty()) {
            item {
                Text(
                    text = if (isSearching)
                        "No results for \"${searchSubmitted.trim()}\" nearby.\nTry a different word or location."
                    else
                        "No restaurants found here.\nTry a different location or category.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        }

        items(visible, key = { it.id }) { restaurant ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                RestaurantCard(restaurant) { onRestaurantClick(restaurant) }
            }
        }
    }
}

@Composable
private fun QuickFilterCard(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) DarkGreenHeader else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier
            .height(76.dp)
            .clickable { onClick() }
    ) {
        Box(Modifier.fillMaxSize()) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) Color.White else TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = if (trailingIcon != null) 26.dp else 8.dp)
            )

            if (trailingIcon != null && onTrailingClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onTrailingClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = "Edit budget",
                        tint = if (selected) YellowButton else DarkGreenHeader,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: FoodCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isSelected) DarkGreenHeader else Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.name.take(2),
                color = if (isSelected) YellowButton else DarkGreenHeader,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            category.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) DarkGreenHeader else TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Could not load places",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFFB71C1C)
            )
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 13.sp, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenHeader),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun RestaurantCard(restaurant: Restaurant, onClick: () -> Unit) {
    val context = LocalContext.current
    val photo = restaurant.photoUrl

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFE0E0E0))
            ) {
                if (!photo.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photo)
                            .crossfade(true)
                            .build(),
                        contentDescription = restaurant.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        "No photo",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    restaurant.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star, null,
                        tint = YellowButton,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${restaurant.rating} (${restaurant.userRatingsTotal})",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        restaurant.distanceFormatted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkGreenHeader
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${restaurant.priceFormatted} • ${restaurant.openingHours}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}