// server.js
const express = require('express');
const cors = require('cors');
const path = require('path');
const { MongoClient } = require('mongodb');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 8080;

// ==================== CONFIGURATION ====================
// Add your API keys here or in .env file
const TICKETMASTER_API_KEY = process.env.TICKETMASTER_API_KEY || 'EBCx2cIuOzyKWgX9IYUMTWGtRTGRZBiN';
const SPOTIFY_CLIENT_ID = process.env.SPOTIFY_CLIENT_ID || '939fd11becaa4ba18a41500f039b8985';
const SPOTIFY_CLIENT_SECRET = process.env.SPOTIFY_CLIENT_SECRET || '3ef4a77b28c441baa17e35f31d5317eb';
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://101priyanshushekhar_db_user:jg6mML8LWHXJZJtC@cluster0.eqkzyyc.mongodb.net/?appName=Cluster0';
const DB_NAME = process.env.DB_NAME || 'eventSearch';
const COLLECTION_NAME = process.env.COLLECTION_NAME || 'favorites';

// ==================== MIDDLEWARE ====================
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'build')));

// ==================== MONGODB CONNECTION ====================
let db;
let favoritesCollection;

async function connectToDatabase() {
  try {
    const client = await MongoClient.connect(MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');
    db = client.db(DB_NAME);
    favoritesCollection = db.collection(COLLECTION_NAME);
  } catch (error) {
    console.error('MongoDB connection error:', error);
    process.exit(1);
  }
}

connectToDatabase();

// ==================== SPOTIFY ACCESS TOKEN ====================
let spotifyAccessToken = null;
let spotifyTokenExpiry = null;

async function getSpotifyAccessToken() {
  if (spotifyAccessToken && spotifyTokenExpiry && Date.now() < spotifyTokenExpiry) {
    return spotifyAccessToken;
  }

  try {
    const credentials = Buffer.from(`${SPOTIFY_CLIENT_ID}:${SPOTIFY_CLIENT_SECRET}`).toString('base64');
    const response = await fetch('https://accounts.spotify.com/api/token', {
      method: 'POST',
      headers: {
        'Authorization': `Basic ${credentials}`,
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: 'grant_type=client_credentials'
    });

    const data = await response.json();
    spotifyAccessToken = data.access_token;
    spotifyTokenExpiry = Date.now() + (data.expires_in * 1000);
    return spotifyAccessToken;
  } catch (error) {
    console.error('Error getting Spotify access token:', error);
    throw error;
  }
}

// ==================== API ROUTES ====================

// Autocomplete/Suggest endpoint
app.get('/api/suggest', async (req, res) => {
  try {
    const { keyword } = req.query;
    if (!keyword) {
      return res.status(400).json({ error: 'Keyword is required' });
    }

    const url = `https://app.ticketmaster.com/discovery/v2/suggest?apikey=${TICKETMASTER_API_KEY}&keyword=${encodeURIComponent(keyword)}`;
    const response = await fetch(url);
    const data = await response.json();
    
    res.json(data);
  } catch (error) {
    console.error('Error fetching suggestions:', error);
    res.status(500).json({ error: 'Failed to fetch suggestions' });
  }
});

// Event search endpoint
app.get('/api/search', async (req, res) => {
  try {
    const { keyword, segmentId, radius, lat, lng } = req.query;

    if (!keyword || !lat || !lng) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }

    // Convert lat/lng to geohash using a simple implementation
    const geohash = require('ngeohash');
    const geoPoint = geohash.encode(parseFloat(lat), parseFloat(lng), 7);

    let url = `https://app.ticketmaster.com/discovery/v2/events.json?apikey=${TICKETMASTER_API_KEY}&keyword=${encodeURIComponent(keyword)}&geoPoint=${geoPoint}&radius=${radius || 10}&unit=miles`;
    
    if (segmentId && segmentId !== 'All') {
      url += `&segmentId=${segmentId}`;
    }

    const response = await fetch(url);
    const data = await response.json();
    
    res.json(data);
  } catch (error) {
    console.error('Error searching events:', error);
    res.status(500).json({ error: 'Failed to search events' });
  }
});

// Event details endpoint
app.get('/api/event/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const url = `https://app.ticketmaster.com/discovery/v2/events/${id}.json?apikey=${TICKETMASTER_API_KEY}`;
    
    const response = await fetch(url);
    const data = await response.json();
    
    res.json(data);
  } catch (error) {
    console.error('Error fetching event details:', error);
    res.status(500).json({ error: 'Failed to fetch event details' });
  }
});

// Spotify artist search endpoint
app.get('/api/artist', async (req, res) => {
  try {
    const { name } = req.query;
    if (!name) {
      return res.status(400).json({ error: 'Artist name is required' });
    }

    const token = await getSpotifyAccessToken();
    
    // Search for artist
    const searchUrl = `https://api.spotify.com/v1/search?q=${encodeURIComponent(name)}&type=artist&limit=1`;
    const searchResponse = await fetch(searchUrl, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const searchData = await searchResponse.json();

    if (!searchData.artists || !searchData.artists.items || searchData.artists.items.length === 0) {
      return res.status(404).json({ error: 'Artist not found' });
    }

    const artist = searchData.artists.items[0];

    // Get artist's albums
    const albumsUrl = `https://api.spotify.com/v1/artists/${artist.id}/albums?limit=12`;
    const albumsResponse = await fetch(albumsUrl, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const albumsData = await albumsResponse.json();

    const result = {
      ...artist,
      albums: albumsData.items || []
    };

    res.json(result);
  } catch (error) {
    console.error('Error fetching artist data:', error);
    res.status(500).json({ error: 'Failed to fetch artist data' });
  }
});

// ==================== FAVORITES ROUTES ====================

// Get all favorites
app.get('/api/favorites', async (req, res) => {
  try {
    const favorites = await favoritesCollection.find({}).toArray();
    res.json(favorites);
  } catch (error) {
    console.error('Error fetching favorites:', error);
    res.status(500).json({ error: 'Failed to fetch favorites' });
  }
});

// Add to favorites
app.post('/api/favorites', async (req, res) => {
  try {
    const event = req.body;
    
    // Check if already exists
    const existing = await favoritesCollection.findOne({ id: event.id });
    if (existing) {
      return res.status(400).json({ error: 'Event already in favorites' });
    }

    await favoritesCollection.insertOne({
      ...event,
      addedAt: new Date()
    });
    
    res.status(201).json({ message: 'Added to favorites' });
  } catch (error) {
    console.error('Error adding to favorites:', error);
    res.status(500).json({ error: 'Failed to add to favorites' });
  }
});

// Remove from favorites
app.delete('/api/favorites/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const result = await favoritesCollection.deleteOne({ id });
    
    if (result.deletedCount === 0) {
      return res.status(404).json({ error: 'Event not found in favorites' });
    }
    
    res.json({ message: 'Removed from favorites' });
  } catch (error) {
    console.error('Error removing from favorites:', error);
    res.status(500).json({ error: 'Failed to remove from favorites' });
  }
});

// ==================== SERVE FRONTEND ====================
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'build', 'index.html'));
});

// ==================== START SERVER ====================
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});