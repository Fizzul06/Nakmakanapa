package com.spg.nakmakanapa.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spg.nakmakanapa.model.GoogleReview
import com.spg.nakmakanapa.model.Restaurant
import com.spg.nakmakanapa.ui.theme.*

@Composable
fun RestaurantDetailScreen(
    restaurant: Restaurant,
    isSaved: Boolean,
    onToggleSave: (String) -> Unit,
    aiReasonNote: String? = null,
    aiReviewSummary: String? = null,
    googleReviews: List<GoogleReview> = emptyList()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val photoUrl = restaurant.photoUrl?.ifBlank { null } ?: restaurant.imageResUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGrayBackground)
            .verticalScroll(scrollState)
    ) {
        // Top Dark Green Header Box with Status Bar Padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .statusBarsPadding()
                .padding(top = 8.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(DarkGreenHeader),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = restaurant.name,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Photo & Badges
        val gallery = remember(photoUrl, restaurant.photoUrls) {
            // Prefer the ranked list; fall back to the single hero, then a stock image.
            when {
                restaurant.photoUrls.isNotEmpty() -> restaurant.photoUrls
                photoUrl.isNotBlank() -> listOf(photoUrl)
                else -> listOf("https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=80")
            }
        }
        var heroIndex by remember(restaurant.id) { mutableIntStateOf(0) }
        val pagerState = rememberPagerState(pageCount = { gallery.size })

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(gallery[page])
                        .crossfade(true)
                        .build(),
                    contentDescription = "${restaurant.name} photo ${page + 1}",
                    // Fit (not Crop) would letterbox; Crop is right, but we now
                    // bias toward a landscape photo so the crop cuts less away.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Locked to the bottom-scrim so the badges stay readable on any photo.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                        )
                    )
            )

            if (restaurant.isAdventure) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AdventureGreenBadge)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Adventure Mode",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Page dots + tap-to-cycle, shown only when there is more than one photo.
            if (gallery.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(gallery.size) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == heroIndex) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == heroIndex) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }

        // Keep heroIndex in sync with pager position.
        LaunchedEffect(pagerState.currentPage) {
            heroIndex = pagerState.currentPage
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DeepSeek AI Rationale Card (If selected via Surprise Me)
        val activeAiReason = aiReasonNote ?: restaurant.aiReason
        if (!activeAiReason.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "DeepSeek AI",
                        tint = DarkGreenHeader,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Why DeepSeek AI Chose This",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkGreenHeader
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeAiReason,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // DeepSeek AI Review Summary Box
        val activeReviewSummary = aiReviewSummary ?: restaurant.reviewsSummary
        if (!activeReviewSummary.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Summary",
                        tint = YellowButton,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Google Reviews Summary (DeepSeek AI)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeReviewSummary,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Main Info Container
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGreenHeader),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // View Route Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(YellowButton)
                        .clickable {
                            // "geo:0,0?q=..." only drops a map PIN — it does not start
                            // navigation. "google.navigation:q=" asks the navigation app
                            // to route to the destination immediately, which is what a
                            // "View Route" button should do.
                            val navUri = Uri.parse(
                                "google.navigation:q=${restaurant.latitude},${restaurant.longitude}"
                            )
                            val navIntent = Intent(Intent.ACTION_VIEW, navUri)
                                .setPackage("com.google.android.apps.maps")

                            // Waze uses its own scheme; if Maps is missing, fall back.
                            val wazeUri = Uri.parse(
                                "waze://?ll=${restaurant.latitude},${restaurant.longitude}&navigate=yes"
                            )
                            val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri)

                            val chooser = Intent.createChooser(navIntent, "Navigate to ${restaurant.name}")
                                .apply {
                                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(wazeIntent))
                                }

                            runCatching { context.startActivity(chooser) }
                                .onFailure {
                                    // No navigation app at all -> plain geo pin as last resort.
                                    val fallback = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(
                                            "geo:${restaurant.latitude},${restaurant.longitude}" +
                                                    "?q=${Uri.encode(restaurant.name)}"
                                        )
                                    )
                                    runCatching { context.startActivity(fallback) }
                                }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View Route",
                        color = Color.Black,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save is its own control. It used to be jammed inside the
                // View Route button, which made both targets hard to hit.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isSaved) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (isSaved) AccentYellow else Color.White.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .clickable { onToggleSave(restaurant.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isSaved) "Remove from saved" else "Save restaurant",
                            tint = if (isSaved) AccentYellow else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSaved) "Saved" else "Save",
                            color = if (isSaved) AccentYellow else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = restaurant.distanceFormatted,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = AccentYellow,
                    thickness = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = restaurant.priceFormatted,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = AccentYellow,
                    thickness = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = restaurant.openingHours,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Real Google User Reviews Section
        val activeReviews = googleReviews.ifEmpty { restaurant.reviews }
        if (activeReviews.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RateReview,
                        contentDescription = "Reviews",
                        tint = DarkGreenHeader,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Reviews (${restaurant.rating}⭐ • ${restaurant.userRatingsTotal} reviews)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                for (review in activeReviews) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = review.authorName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = YellowButton,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${review.rating}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = review.text,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = review.relativeTime,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}