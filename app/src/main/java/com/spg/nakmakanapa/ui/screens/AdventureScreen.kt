package com.spg.nakmakanapa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spg.nakmakanapa.model.Restaurant
import com.spg.nakmakanapa.ui.theme.AdventureGreenBadge
import com.spg.nakmakanapa.ui.theme.DarkGreenHeader
import com.spg.nakmakanapa.ui.theme.LightGrayBackground
import com.spg.nakmakanapa.ui.theme.TextPrimary
import com.spg.nakmakanapa.ui.theme.TextSecondary

@Composable
fun AdventureScreen(
    adventureRestaurants: List<Restaurant>,
    isLoading: Boolean,
    errorMessage: String?,
    onAdventureClick: (Restaurant) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    // Closest first, matching Home.
    val sorted = remember(adventureRestaurants) {
        adventureRestaurants.sortedBy { it.distanceKm }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGrayBackground),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(DarkGreenHeader)
                    .statusBarsPadding()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .padding(start = 12.dp)
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

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Explore, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Adventure Routes",
                            color = Color.White,
                            fontFamily = FontFamily.Cursive,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(48.dp))
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = DarkGreenHeader, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Scenic Journey Priority", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text(
                            "Live scenic driving & riding destinations — countryside, forest roads and hill lookouts.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DarkGreenHeader)
                }
            }
        }

        if (errorMessage != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Could not load routes", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                        Spacer(Modifier.height(4.dp))
                        Text(errorMessage, fontSize = 13.sp, color = TextPrimary)
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
        }

        if (!isLoading && errorMessage == null && adventureRestaurants.isEmpty()) {
            item {
                Text(
                    "No adventure destinations found near you.\nTry another location.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        }

        items(sorted, key = { it.id }) { restaurant ->
            Box(Modifier.padding(horizontal = 16.dp)) {
                AdventureCard(restaurant) { onAdventureClick(restaurant) }
            }
        }
    }
}

@Composable
private fun AdventureCard(restaurant: Restaurant, onClick: () -> Unit) {
    val context = LocalContext.current
    val photo = restaurant.photoUrl

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
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
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Box(
                    Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AdventureGreenBadge)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Adventure Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Column(Modifier.padding(16.dp)) {
                Text(
                    restaurant.name,
                    fontFamily = FontFamily.Cursive,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text("Distance: ${restaurant.distanceFormatted}", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${restaurant.priceFormatted} • ${restaurant.openingHours}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkGreenHeader
                )
                if (restaurant.address.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(restaurant.address, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}