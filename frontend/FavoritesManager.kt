package com.example.eventfinder.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.eventfinder.data.model.Event
import com.example.eventfinder.data.model.FavoriteEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun addFavorite(event: Event) {
        val favorites = getFavorites().toMutableList()
        val favoriteEvent = FavoriteEvent(
            eventId = event.id,
            name = event.name,
            date = event.date,
            time = event.time,
            venue = event.venue,
            category = event.category,
            imageUrl = event.imageUrl,
            addedAt = System.currentTimeMillis()
        )
        favorites.removeAll { it.eventId == event.id }
        favorites.add(favoriteEvent)
        saveFavorites(favorites)
    }

    fun removeFavorite(eventId: String) {
        val favorites = getFavorites().toMutableList()
        favorites.removeAll { it.eventId == eventId }
        saveFavorites(favorites)
    }

    fun isFavorite(eventId: String): Boolean {
        return getFavorites().any { it.eventId == eventId }
    }

    fun getFavorites(): List<FavoriteEvent> {
        val json = prefs.getString("favorites_list", null) ?: return emptyList()
        val type = object : TypeToken<List<FavoriteEvent>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveFavorites(favorites: List<FavoriteEvent>) {
        val json = gson.toJson(favorites)
        prefs.edit().putString("favorites_list", json).apply()
    }

    fun toggleFavorite(event: Event): Boolean {
        return if (isFavorite(event.id)) {
            removeFavorite(event.id)
            false
        } else {
            addFavorite(event)
            true
        }
    }
}