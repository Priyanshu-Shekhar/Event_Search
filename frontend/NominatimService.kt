package com.example.eventfinder.data.api

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

// Nominatim API for location autocomplete (FREE - no API key needed)
interface NominatimService {
    @GET("search")
    suspend fun searchLocations(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressDetails: Int = 1
    ): List<JsonObject>
}
