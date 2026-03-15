package com.swipeapply.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer

@Composable
fun ShimmerJobCard(
    modifier: Modifier = Modifier
) {
    val shimmerColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .shimmer(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmerColor)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerColor)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerColor)
                )
            }

            Column {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(shimmerColor)
                        )
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(shimmerColor)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(shimmerColor)
                        )
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(shimmerColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerCardStack(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        repeat(3) { index ->
            val scale = 1f - (index * 0.04f)
            val offset = (index * 12).dp

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
