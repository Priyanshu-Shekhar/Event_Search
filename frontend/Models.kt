package com.example.eventfinder.data.model

data class Event(
    val id: String,
    val name: String,
    val date: String,
    val time: String? = null,
    val venue: String,
    val category: String,
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class EventDetails(
    val id: String,
    val name: String,
    val localDate: String,
    val localTime: String? = null,
    val artists: List<String>? = null,
    val venue: String,
    val genres: List<String>,
    val priceRanges: String? = null,
    val ticketStatus: String,
    val buyTicketUrl: String,
    val seatMapUrl: String? = null
)

data class Artist(
    val name: String,
    val imageUrl: String? = null,
    val spotifyUrl: String,
    val followers: Long,
    val popularity: Int,
    val genres: List<String>,
    val albums: List<Album>
)

data class Album(
    val name: String,
    val imageUrl: String,
    val releaseDate: String,
    val totalTracks: Int,
    val spotifyUrl: String
)

data class Venue(
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val imageUrl: String? = null,
    val ticketmasterUrl: String
)

data class SearchResponse(
    val events: List<Event>
)

data class AutoSuggest(
    val suggestions: List<String>
)

data class FavoriteEvent(
    val eventId: String,
    val name: String,
    val date: String,
    val time: String?,
    val venue: String,
    val category: String,
    val imageUrl: String?,
    val addedAt: Long
)