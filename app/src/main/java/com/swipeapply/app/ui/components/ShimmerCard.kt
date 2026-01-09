package com.swipeapply.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer
import com.swipeapply.app.ui.theme.BackgroundCard
import com.swipeapply.app.ui.theme.BackgroundSecondary

/**
 * Shimmer loading skeleton for job cards.
 * Shows while cards are loading from backend.
 */
@Composable
fun ShimmerJobCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .shimmer(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Company info
            Column {
                // Hiring badge shimmer
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundSecondary)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Company logo placeholder
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BackgroundSecondary)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Company name shimmer
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundSecondary)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Job title shimmer
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BackgroundSecondary)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Location chips shimmer
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundSecondary)
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundSecondary)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Salary shimmer
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BackgroundSecondary)
                )
            }
            
            // Bottom section - Tech stack shimmer
            Column {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BackgroundSecondary)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Tech chips shimmer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundSecondary)
                        )
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundSecondary)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundSecondary)
                        )
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundSecondary)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stack of shimmer cards to show during loading
 */
@Composable
fun ShimmerCardStack(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Show 3 shimmer cards with depth effect
        repeat(3) { index ->
            val scale = 1f - (index * 0.05f)
            val offset = (index * 8).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = offset)
                    .then(
                if (scale < 1f) {
                    Modifier.width((300 * scale).dp)
                    } else Modifier
                    )
            ) {
                ShimmerJobCard()
            }
        }
    }
}
