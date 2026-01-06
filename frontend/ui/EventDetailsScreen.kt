package com.example.eventfinder.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.eventfinder.data.api.ApiClient
import com.example.eventfinder.data.local.FavoritesManager
import androidx.compose.material.icons.outlined.StarBorder
import com.example.eventfinder.data.model.Event
import com.example.eventfinder.ui.components.MarqueeText
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventDetailsScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoritesManager = remember { FavoritesManager(context) }

    var eventData by remember { mutableStateOf<JsonObject?>(null) }
    var artistsData by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var venueData by remember { mutableStateOf<JsonObject?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(favoritesManager.isFavorite(eventId)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val tabs = listOf("Details", "Artist", "Venue")
    var eventName by remember { mutableStateOf("") }

    LaunchedEffect(eventId) {
        isLoading = true
        errorMessage = null
        Log.d("EventDetails", "Loading event: $eventId")

        try {
            val response = ApiClient.apiService.getEventDetails(eventId)
            eventData = response
            eventName = response.get("name")?.asString ?: "Event Details"

            // Extract venue data
            val embedded = response.getAsJsonObject("_embedded")
            val venues = embedded?.getAsJsonArray("venues")
            if (venues != null && venues.size() > 0) {
                venueData = venues[0].asJsonObject
            }

            // Get artists
            val attractions = embedded?.getAsJsonArray("attractions")
            if (attractions != null && attractions.size() > 0) {
                val artistsList = mutableListOf<JsonObject>()
                for (i in 0 until minOf(attractions.size(), 5)) {
                    try {
                        val attraction = attractions[i].asJsonObject
                        val artistName = attraction.get("name")?.asString
                        if (artistName != null && artistName.isNotBlank()) {
                            try {
                                val artistInfo = ApiClient.apiService.getArtistInfo(artistName)
                                artistsList.add(artistInfo)
                            } catch (e: Exception) {
                                Log.w("EventDetails", "Artist not found: $artistName")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("EventDetails", "Error processing attraction", e)
                    }
                }
                artistsData = artistsList
            }
        } catch (e: Exception) {
            Log.e("EventDetails", "Error loading event", e)
            errorMessage = "Failed to load event details"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (eventName.isNotEmpty()) {
                            MarqueeText(
                                text = eventName,
                                style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
                            )
                        } else {
                            Text("Event Details", color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                eventData?.let { data ->
                                    val id = data.get("id")?.asString ?: eventId
                                    val name = data.get("name")?.asString ?: ""
                                    val dates = data.getAsJsonObject("dates")
                                    val start = dates?.getAsJsonObject("start")
                                    val date = start?.get("localDate")?.asString ?: ""
                                    val time = start?.get("localTime")?.asString
                                    val embedded = data.getAsJsonObject("_embedded")
                                    val venues = embedded?.getAsJsonArray("venues")
                                    val venue = if (venues != null && venues.size() > 0) {
                                        venues[0].asJsonObject.get("name")?.asString ?: ""
                                    } else ""

                                    val event = Event(
                                        id = id,
                                        name = name,
                                        date = date,
                                        time = time,
                                        venue = venue,
                                        category = "",
                                        imageUrl = null
                                    )
                                    isFavorite = favoritesManager.toggleFavorite(event)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFFC107) else Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title, color = Color.White) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "Unknown Error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateBack) {
                                Text("Go Back")
                            }
                        }
                    }
                }
                else -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> DetailsTab(eventData)
                            1 -> ArtistsTab(artistsData)
                            2 -> VenueTab(venueData)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsTab(eventData: JsonObject?) {
    val context = LocalContext.current

    if (eventData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No event data available")
        }
        return
    }

    // Extract all data before composables with proper null/type checking
    val dates = eventData.getAsJsonObject("dates")
    val start = dates?.getAsJsonObject("start")
    val localDate = start?.get("localDate")?.asString ?: ""
    val localTime = start?.get("localTime")?.asString
    val formattedDate = if (localDate.isNotEmpty()) formatEventDate(localDate, localTime) else ""

    val embedded = eventData.getAsJsonObject("_embedded")

    // Artists - safe array access
    val artistNames = mutableListOf<String>()
    try {
        val attractionsElement = embedded?.get("attractions")
        if (attractionsElement != null && attractionsElement.isJsonArray) {
            val attractions = attractionsElement.asJsonArray
            for (i in 0 until attractions.size()) {
                attractions[i].asJsonObject.get("name")?.asString?.let {
                    artistNames.add(it)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("DetailsTab", "Error parsing attractions", e)
    }

    // Venue - safe array access
    var venueName = ""
    try {
        val venuesElement = embedded?.get("venues")
        if (venuesElement != null && venuesElement.isJsonArray) {
            val venues = venuesElement.asJsonArray
            if (venues.size() > 0) {
                venueName = venues[0].asJsonObject.get("name")?.asString ?: ""
            }
        }
    } catch (e: Exception) {
        Log.e("DetailsTab", "Error parsing venues", e)
    }

    // Genres - safe array access
    val genres = mutableListOf<String>()
    try {
        val classificationsElement = eventData.get("classifications")
        if (classificationsElement != null && classificationsElement.isJsonArray) {
            val classifications = classificationsElement.asJsonArray
            if (classifications.size() > 0) {
                val classification = classifications[0].asJsonObject
                classification.getAsJsonObject("segment")?.get("name")?.asString?.let { genres.add(it) }
                classification.getAsJsonObject("genre")?.get("name")?.asString?.let {
                    if (it != "Undefined") genres.add(it)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("DetailsTab", "Error parsing genres", e)
    }

    // Ticket Status
    val status = dates?.getAsJsonObject("status")?.get("code")?.asString ?: "unknown"

    // URLs
    val buyTicketUrl = eventData.get("url")?.asString

    // Seatmap - safe array/object access (THIS WAS THE BUG!)
    var seatmapUrl: String? = null
    try {
        val seatmapElement = eventData.get("seatmap")
        if (seatmapElement != null) {
            if (seatmapElement.isJsonArray) {
                val seatmap = seatmapElement.asJsonArray
                if (seatmap.size() > 0) {
                    seatmapUrl = seatmap[0].asJsonObject.get("staticUrl")?.asString
                }
            } else if (seatmapElement.isJsonObject) {
                seatmapUrl = seatmapElement.asJsonObject.get("staticUrl")?.asString
            }
        }
    } catch (e: Exception) {
        Log.e("DetailsTab", "Error parsing seatmap", e)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Event",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (formattedDate.isNotEmpty()) {
                        DetailRow("Date", formattedDate)
                    }

                    if (artistNames.isNotEmpty()) {
                        DetailRow("Artists", artistNames.joinToString(", "))
                    }

                    if (venueName.isNotEmpty()) {
                        DetailRow("Venue", venueName)
                    }

                    if (genres.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Genres",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                genres.forEach { genre ->
                                    Surface(
                                        color = Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            genre,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ticket Status",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            color = when (status.lowercase()) {
                                "onsale" -> Color(0xFF4CAF50)
                                "offsale" -> Color(0xFFF44336)
                                "cancelled", "canceled" -> Color(0xFF000000)
                                else -> Color.Gray
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = status.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        if (buyTicketUrl != null) {
            item {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buyTicketUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buy Ticket")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    val eventName = eventData.get("name")?.asString ?: "Event"
                    val shareUrl = buyTicketUrl ?: ""
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out $eventName at $shareUrl")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Event"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Event")
            }
        }

        if (seatmapUrl != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Seatmap",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = seatmapUrl,
                            contentDescription = "Seat Map",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun ArtistsTab(artistsData: List<JsonObject>) {
    if (artistsData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No artist data",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artistsData) { artistData ->
                ArtistItem(artistData = artistData)
            }
        }
    }
}
@Composable
fun ArtistItem(artistData: JsonObject) {
    val context = LocalContext.current

    // Extract all data before composables
    val artistName = artistData.get("name")?.asString ?: "Unknown Artist"
    val spotifyUrl = artistData.getAsJsonObject("external_urls")?.get("spotify")?.asString
    val images = artistData.getAsJsonArray("images")
    val imageUrl = if (images != null && images.size() > 0) {
        images[0].asJsonObject.get("url")?.asString
    } else null
    val followers = artistData.getAsJsonObject("followers")?.get("total")?.asLong ?: 0
    val popularity = artistData.get("popularity")?.asInt ?: 0

    val genres = artistData.getAsJsonArray("genres")
    val genreList = mutableListOf<String>()
    if (genres != null && genres.size() > 0) {
        for (i in 0 until genres.size()) {
            genres[i].asString?.let { genreList.add(it) }
        }
    }

    val albums = artistData.getAsJsonArray("albums")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    artistName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (spotifyUrl != null) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            "Open Spotify",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Artist",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Followers: ${formatNumber(followers)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Popularity: $popularity%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (genreList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    genreList.take(3).forEach { genre ->
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                genre,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Display ALL albums in a 2-column grid
            if (albums != null && albums.size() > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Albums (${albums.size()})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Create a 2-column grid of albums
                val albumsList = mutableListOf<JsonObject>()
                for (i in 0 until albums.size()) {
                    try {
                        albumsList.add(albums[i].asJsonObject)
                    } catch (e: Exception) {
                        Log.e("ArtistItem", "Error parsing album at index $i", e)
                    }
                }

                // Display albums in rows of 2
                val rows = albumsList.chunked(2)
                rows.forEach { rowAlbums ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowAlbums.forEach { album ->
                            AlbumCard(
                                album = album,
                                context = context,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Fill empty space if odd number of albums in last row
                        if (rowAlbums.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: JsonObject,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val albumImages = album.getAsJsonArray("images")
    val albumImageUrl = if (albumImages != null && albumImages.size() > 0) {
        albumImages[0].asJsonObject.get("url")?.asString
    } else null

    val albumUrl = album.getAsJsonObject("external_urls")?.get("spotify")?.asString
    val albumName = album.get("name")?.asString ?: ""
    val releaseDate = album.get("release_date")?.asString ?: ""
    val totalTracks = album.get("total_tracks")?.asInt ?: 0

    Card(
        modifier = modifier,
        onClick = {
            if (albumUrl != null) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(albumUrl))
                context.startActivity(intent)
            }
        },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            if (albumImageUrl != null) {
                AsyncImage(
                    model = albumImageUrl,
                    contentDescription = albumName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder for missing image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Album,
                        contentDescription = "Album",
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    albumName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$totalTracks tracks",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    releaseDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun VenueTab(venueData: JsonObject?) {
    val context = LocalContext.current

    if (venueData == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No venue information available",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    // Extract data before composables
    val venueName = venueData.get("name")?.asString ?: ""
    val venueUrl = venueData.get("url")?.asString
    val address = venueData.getAsJsonObject("address")
    val city = venueData.getAsJsonObject("city")
    val state = venueData.getAsJsonObject("state")
    val addressLine = address?.get("line1")?.asString ?: ""
    val cityName = city?.get("name")?.asString ?: ""
    val stateName = state?.get("name")?.asString ?: ""
    val images = venueData.getAsJsonArray("images")
    val imageUrl = if (images != null && images.size() > 0) {
        images[0].asJsonObject.get("url")?.asString
    } else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            venueName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (venueUrl != null) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(venueUrl))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    "Open Ticketmaster",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (addressLine.isNotEmpty()) {
                        Text(
                            "Address:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$addressLine, $cityName, $stateName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (imageUrl != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Venue",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}

fun formatEventDate(date: String, time: String?): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = inputFormat.parse(date) ?: return date
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.time = parsedDate
        val eventYear = calendar.get(Calendar.YEAR)

        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        var formatted = dateFormat.format(parsedDate)

        if (time != null && time.isNotBlank()) {
            try {
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val parsedTime = timeFormat.parse(time)
                if (parsedTime != null) {
                    val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    formatted += ", ${displayTimeFormat.format(parsedTime)}"
                }
            } catch (e: Exception) {
                Log.e("formatEventDate", "Error parsing time", e)
            }
        }
        formatted
    } catch (e: Exception) {
        Log.e("formatEventDate", "Error formatting date", e)
        "$date ${time ?: ""}"
    }
}
