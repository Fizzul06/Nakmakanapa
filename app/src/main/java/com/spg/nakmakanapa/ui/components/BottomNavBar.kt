package com.spg.nakmakanapa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spg.nakmakanapa.ui.theme.DarkGreenHeader
import com.spg.nakmakanapa.ui.theme.TextPrimary
import com.spg.nakmakanapa.ui.theme.TextSecondary

sealed class NavItem(val route: String, val title: String) {
    object Home : NavItem("home", "Home")
    object Adventure : NavItem("adventure", "Adventure")
    object SurpriseMe : NavItem("surprise_me", "Surprise\nMe!")
    object Saved : NavItem("saved", "Saved")
    object Profile : NavItem("profile", "Profile")
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSurpriseMeClick: () -> Unit
) {
    // Floating layout where Surprise Me! button overflows above the navigation bar
    // without getting cut off or creating double white borders!
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom White Navigation Bar
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                NavItemCell(
                    title = NavItem.Home.title,
                    isSelected = currentRoute == NavItem.Home.route,
                    onClick = { onNavigate(NavItem.Home.route) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = if (currentRoute == NavItem.Home.route) DarkGreenHeader else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Adventure
                NavItemCell(
                    title = NavItem.Adventure.title,
                    isSelected = currentRoute == NavItem.Adventure.route,
                    onClick = { onNavigate(NavItem.Adventure.route) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Adventure",
                        tint = if (currentRoute == NavItem.Adventure.route) DarkGreenHeader else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Spacer for elevated Surprise Me button
                Spacer(modifier = Modifier.width(68.dp))

                // Saved
                NavItemCell(
                    title = NavItem.Saved.title,
                    isSelected = currentRoute == NavItem.Saved.route,
                    onClick = { onNavigate(NavItem.Saved.route) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Saved",
                        tint = if (currentRoute == NavItem.Saved.route) DarkGreenHeader else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Profile
                NavItemCell(
                    title = NavItem.Profile.title,
                    isSelected = currentRoute == NavItem.Profile.route,
                    onClick = { onNavigate(NavItem.Profile.route) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = if (currentRoute == NavItem.Profile.route) DarkGreenHeader else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Center Floating Circle ("Surprise Me!")
        Box(
            modifier = Modifier
                .offset(y = (-18).dp)
                .size(72.dp)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(DarkGreenHeader)
                .clickable { onSurpriseMeClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Surprise\nMe!",
                color = Color.White,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun NavItemCell(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) TextPrimary else TextSecondary
        )
    }
}