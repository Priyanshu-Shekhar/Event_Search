package com.example.eventfinder.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.eventfinder.data.api.ApiClient
import com.example.eventfinder.data.local.FavoritesManager
import com.example.eventfinder.data.model.Event
import com.example.eventfinder.ui.components.EventCard
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoritesManager = remember { FavoritesManager(context) }

    var keyword by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("10") }
    var selectedCategory by remember { mutableStateOf("All") }
    var useCurrentLocation by remember { mutableStateOf(true) }
    var locationText by remember { mutableStateOf("") }

    // ALWAYS USE LOS ANGELES FOR "CURRENT LOCATION"
    val currentLatitude = 34.0522
    val currentLongitude = -118.2437

    var keywordError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Event>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var autoSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }

    // Location autocomplete state
    var locationSuggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var showLocationSuggestions by remember { mutableStateOf(false) }

    val categories = listOf("All", "Music", "Sports", "Arts & Theatre", "Film", "Miscellaneous")

    // Map display names to Ticketmaster segment IDs
    val categoryToSegmentId = mapOf(
        "All" to null,
        "Music" to "KZFzniwnSyZfZ7v7nJ",
        "Sports" to "KZFzniwnSyZfZ7v7nE",
        "Arts & Theatre" to "KZFzniwnSyZfZ7v7na",
        "Film" to "KZFzniwnSyZfZ7v7nn",
        "Miscellaneous" to "KZFzniwnSyZfZ7v7n1"
    )

    fun performSearch() {
        keywordError = keyword.isBlank()
        if (keywordError) return

        showSuggestions = false
        showLocationSuggestions = false

        scope.launch {
            isLoading = true
            hasSearched = true
            try {
                var lat: Double
                var lon: Double

                if (useCurrentLocation) {
                    lat = currentLatitude
                    lon = currentLongitude
                } else {
                    // Use coordinates from selected location suggestion if available
                    val selectedLocation = locationSuggestions.firstOrNull {
                        it.displayName == locationText
                    }

                    if (selectedLocation != null) {
                        lat = selectedLocation.lat
                        lon = selectedLocation.lon
                    } else {
                        // Fallback to geocoding
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocationName(locationText, 1)
                            if (addresses.isNullOrEmpty()) {
                                lat = 34.0522
                                lon = -118.2437
                            } else {
                                lat = addresses[0].latitude
                                lon = addresses[0].longitude
                            }
                        } catch (e: Exception) {
                            lat = 34.0522
                            lon = -118.2437
                        }
                    }
                }

                val segmentId = categoryToSegmentId[selectedCategory]

                // Your GCP backend uses lat/lng parameters (not geoPoint)
                val response = ApiClient.apiService.searchEvents(
                    keyword = keyword,
                    segmentId = segmentId,
                    radius = distance.toIntOrNull() ?: 10,
                    latitude = lat,
                    longitude = lon
                )

                searchResults = parseEventsFromResponse(response)
            } catch (e: Exception) {
                e.printStackTrace()
                searchResults = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Events") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Keyword field
            OutlinedTextField(
                value = keyword,
                onValueChange = {
                    keyword = it
                    keywordError = false
                    if (it.length >= 2) {
                        scope.launch {
                            try {
                                val response = ApiClient.apiService.getAutoSuggestions(it)
                                autoSuggestions = parseSuggestionsFromResponse(response)
                                showSuggestions = autoSuggestions.isNotEmpty()
                            } catch (e: Exception) {
                                autoSuggestions = emptyList()
                            }
                        }
                    } else {
                        showSuggestions = false
                    }
                },
                label = { Text("Search events…") },
                isError = keywordError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    showSuggestions = false
                    performSearch()
                }),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        showSuggestions = false
                        performSearch()
                    }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                }
            )

            if (keywordError) {
                Text(
                    text = "Keyword is required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (showSuggestions && autoSuggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(autoSuggestions) { suggestion ->
                            Text(
                                text = suggestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        keyword = suggestion
                                        showSuggestions = false
                                    }
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location selector with autocomplete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Location:", modifier = Modifier.width(80.dp))
                if (useCurrentLocation) {
                    TextButton(onClick = { useCurrentLocation = false }) {
                        Text("Current Location")
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Row {
                            OutlinedTextField(
                                value = locationText,
                                onValueChange = { newValue ->
                                    locationText = newValue
                                    // Fetch location suggestions from Nominatim
                                    if (newValue.length >= 3) {
                                        scope.launch {
                                            try {
                                                val response = ApiClient.nominatimService.searchLocations(newValue)
                                                locationSuggestions = parseLocationSuggestionsFromNominatim(response)
                                                showLocationSuggestions = locationSuggestions.isNotEmpty()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                locationSuggestions = emptyList()
                                                showLocationSuggestions = false
                                            }
                                        }
                                    } else {
                                        showLocationSuggestions = false
                                    }
                                },
                                label = { Text("Enter location") },
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                useCurrentLocation = true
                                locationText = ""
                                showLocationSuggestions = false
                            }) {
                                Text("Use Current")
                            }
                        }

                        // Location suggestions dropdown
                        if (showLocationSuggestions && locationSuggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    items(locationSuggestions) { suggestion ->
                                        Text(
                                            text = suggestion.displayName,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    locationText = suggestion.displayName
                                                    showLocationSuggestions = false
                                                }
                                                .padding(12.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (suggestion != locationSuggestions.last()) {
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Distance selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Distance:", modifier = Modifier.width(80.dp))
                OutlinedTextField(
                    value = distance,
                    onValueChange = { distance = it },
                    modifier = Modifier.width(100.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("miles")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category tabs
            ScrollableTabRow(selectedTabIndex = categories.indexOf(selectedCategory)) {
                categories.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            if (hasSearched) performSearch()
                        },
                        text = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (hasSearched) {
                if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "No events found",
                                modifier = Modifier.padding(24.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { event ->
                            EventCard(
                                event = event,
                                isFavorite = favoritesManager.isFavorite(event.id),
                                onFavoriteClick = {
                                    favoritesManager.toggleFavorite(event)
                                },
                                onCardClick = { onNavigateToDetails(event.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Location suggestion data class
data class LocationSuggestion(
    val displayName: String,
    val lat: Double,
    val lon: Double
)

// Parse Nominatim location suggestions
fun parseLocationSuggestionsFromNominatim(response: List<JsonObject>): List<LocationSuggestion> {
    return response.mapNotNull { obj ->
        try {
            val displayName = obj.get("display_name")?.asString ?: return@mapNotNull null
            val lat = obj.get("lat")?.asString?.toDoubleOrNull() ?: return@mapNotNull null
            val lon = obj.get("lon")?.asString?.toDoubleOrNull() ?: return@mapNotNull null
            LocationSuggestion(displayName, lat, lon)
        } catch (e: Exception) {
            null
        }
    }
}

// Parse Ticketmaster event response
fun parseEventsFromResponse(response: JsonObject): List<Event> {
    try {
        val embedded = response.getAsJsonObject("_embedded") ?: return emptyList()
        val eventsArray = embedded.getAsJsonArray("events") ?: return emptyList()

        return eventsArray.mapNotNull { element ->
            try {
                val eventObj = element.asJsonObject
                val id = eventObj.get("id")?.asString ?: return@mapNotNull null
                val name = eventObj.get("name")?.asString ?: "Unknown Event"

                val dates = eventObj.getAsJsonObject("dates")
                val start = dates?.getAsJsonObject("start")
                val localDate = start?.get("localDate")?.asString ?: ""
                val localTime = start?.get("localTime")?.asString

                val venueEmbedded = eventObj.getAsJsonObject("_embedded")
                val venues = venueEmbedded?.getAsJsonArray("venues")
                val venueName = if (venues != null && venues.size() > 0) {
                    venues[0].asJsonObject.get("name")?.asString ?: "Unknown Venue"
                } else {
                    "Unknown Venue"
                }

                val classifications = eventObj.getAsJsonArray("classifications")
                val category = if (classifications != null && classifications.size() > 0) {
                    classifications[0].asJsonObject.getAsJsonObject("segment")
                        ?.get("name")?.asString ?: "Event"
                } else {
                    "Event"
                }

                val images = eventObj.getAsJsonArray("images")
                val imageUrl = if (images != null && images.size() > 0) {
                    images[0].asJsonObject.get("url")?.asString
                } else {
                    null
                }

                Event(
                    id = id,
                    name = name,
                    date = localDate,
                    time = localTime,
                    venue = venueName,
                    category = category,
                    imageUrl = imageUrl
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return emptyList()
    }
}

// Parse autocomplete suggestions
fun parseSuggestionsFromResponse(response: JsonObject): List<String> {
    try {
        val embedded = response.getAsJsonObject("_embedded") ?: return emptyList()
        val attractions = embedded.getAsJsonArray("attractions") ?: return emptyList()

        return attractions.mapNotNull { element ->
            try {
                element.asJsonObject.get("name")?.asString
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return emptyList()
    }
}
