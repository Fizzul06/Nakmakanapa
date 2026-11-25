package com.spg.nakmakanapa.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.spg.nakmakanapa.ui.theme.DarkGreenHeader
import com.spg.nakmakanapa.ui.theme.YellowButton

@Composable
fun LandingScreen(
    onJomMakanClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fullscreen forest background image
        Image(
            painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1200&q=80"),
            contentDescription = "Background Trees",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark overlay gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo Header Container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo Icon Box
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkGreenHeader),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = "Logo Icon",
                        tint = YellowButton,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Nak",
                    color = Color.White,
                    fontFamily = FontFamily.Cursive,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Normal
                )

                Text(
                    text = "Makan Apa?",
                    color = Color.White,
                    fontFamily = FontFamily.Cursive,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Can't decide but hungry?",
                    color = Color.White,
                    fontFamily = FontFamily.Cursive,
                    fontSize = 24.sp,
                    textDecoration = TextDecoration.Underline
                )

                Text(
                    text = "We will help!",
                    color = Color.White,
                    fontFamily = FontFamily.Cursive,
                    fontSize = 24.sp
                )
            }

            // Bottom Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Yellow Pill Button
                Button(
                    onClick = onJomMakanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = YellowButton),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Text(
                        text = "Jom Makan!",
                        color = DarkGreenHeader,
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Later",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.clickable { onJomMakanClick() }
                )
            }
        }
    }
}