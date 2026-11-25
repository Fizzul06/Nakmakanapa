package com.spg.nakmakanapa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.spg.nakmakanapa.ui.theme.DarkGreenHeader
import com.spg.nakmakanapa.ui.theme.LightGrayBackground
import com.spg.nakmakanapa.ui.theme.TextPrimary
import com.spg.nakmakanapa.ui.theme.TextSecondary

@Composable
fun SavedListScreen(
    savedRestaurants: List<Restaurant>,
    onRemoveSaved: (String) -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGrayBackground)
    ) {
        // Dark Green Curved Header Box with Status Bar Padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(DarkGreenHeader)
                .statusBarsPadding()
                .padding(vertical = 16.dp)
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

                Text(
                    text = "Saved List",
                    color = Color.White,
                    fontFamily = FontFamily.Cursive,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (savedRestaurants.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved restaurants yet.\nClick the bookmark button on any restaurant to save it here!",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(savedRestaurants) { restaurant ->
                    SavedRestaurantItemBox(
                        restaurant = restaurant,
                        onClick = { onRestaurantClick(restaurant) },
                        onRemoveClick = { onRemoveSaved(restaurant.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedRestaurantItemBox(
    restaurant: Restaurant,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val context = LocalContext.current
    val photoUrl = restaurant.photoUrl?.ifBlank { null } ?: restaurant.imageResUrl

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Half: Information
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = restaurant.name,
                    fontFamily = FontFamily.Cursive,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Distance: ${restaurant.distanceFormatted.uppercase()}",
                    fontFamily = FontFamily.Cursive,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = restaurant.openingHours,
                    fontFamily = FontFamily.Cursive,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextPrimary
                )
            }

            // Right Half: Photo with Bookmark Remove Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (photoUrl.isNotBlank()) photoUrl else "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=80")
                        .crossfade(true)
                        .build(),
                    contentDescription = restaurant.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Remove Bookmark",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}