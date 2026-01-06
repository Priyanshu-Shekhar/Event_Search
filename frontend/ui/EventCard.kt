package com.example.eventfinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.eventfinder.data.model.Event
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventCard(
    event: Event,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onCardClick: () -> Unit
) {
    var favorite by remember { mutableStateOf(isFavorite) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            if (event.imageUrl != null) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = event.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback gradient background if no image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF5E72E4),
                                    Color(0xFF825EE4)
                                )
                            )
                        )
                )
            }

            // Dark overlay for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 200f
                        )
                    )
            )

            // Top badges (Category and Date)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Category Badge
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = event.category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5E72E4)
                    )
                }

                // Date Badge
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = formatEventDateTime(event.date, event.time),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }

            // Bottom Info Card
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            MarqueeText(
                                text = event.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = event.venue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        // Favorite Button
                        IconButton(
                            onClick = {
                                onFavoriteClick()
                                favorite = !favorite
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (favorite) Color(0xFFFFC107) else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatEventDateTime(date: String, time: String?): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = inputFormat.parse(date)
        val calendar = Calendar.getInstance()

        if (parsedDate != null) {
            calendar.time = parsedDate
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            var formatted = outputFormat.format(parsedDate)

            // Add time if available
            if (time != null && time.isNotBlank()) {
                try {
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val parsedTime = timeFormat.parse(time)
                    if (parsedTime != null) {
                        val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        formatted += ", ${displayTimeFormat.format(parsedTime)}"
                    }
                } catch (e: Exception) {
                    // If time parsing fails, just show date
                }
            }
            return formatted
        }
        "$date ${time ?: ""}"
    } catch (e: Exception) {
        "$date ${time ?: ""}"
    }
}
