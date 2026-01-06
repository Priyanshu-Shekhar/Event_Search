package com.example.eventfinder.data.api

import com.example.eventfinder.data.model.*
import com.google.gson.JsonObject
import retrofit2.http.*

interface ApiService {
    // Autocomplete suggestions (from your GCP backend)
    @GET("api/suggest")
    suspend fun getAutoSuggestions(
        @Query("keyword") keyword: String
    ): JsonObject

    // Search events (from your GCP backend)
    @GET("api/search")
    suspend fun searchEvents(
        @Query("keyword") keyword: String,
        @Query("segmentId") segmentId: String? = null,
        @Query("radius") radius: Int,
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double
    ): JsonObject

    // Get event details (from your GCP backend)
    @GET("api/event/{id}")
    suspend fun getEventDetails(
        @Path("id") eventId: String
    ): JsonObject

    // Get artist information - Spotify (from your GCP backend)
    @GET("api/artist")
    suspend fun getArtistInfo(
        @Query("name") artistName: String
    ): JsonObject

    // Favorites endpoints (from your GCP backend)
    @GET("api/favorites")
    suspend fun getFavorites(): List<FavoriteEvent>

    @POST("api/favorites")
    suspend fun addFavorite(@Body event: FavoriteEvent): JsonObject

    @DELETE("api/favorites/{id}")
    suspend fun removeFavorite(@Path("id") eventId: String): JsonObject
}
