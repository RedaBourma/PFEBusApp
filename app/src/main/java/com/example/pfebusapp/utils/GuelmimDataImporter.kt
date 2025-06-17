package com.example.pfebusapp.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.pfebusapp.firebase.FirestoreHelper
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Enhanced utility class to import Guelmim city data from OpenStreetMap via Overpass API
 * This provides a comprehensive dataset for the bus routing application.
 * 
 * Usage from any Activity or Fragment:
 * 
 * lifecycleScope.launch {
 *     val importer = GuelmimDataImporter(context)
 *     importer.importAllData { progress, message ->
 *         // Update UI with progress
 *     }
 * }
 */
class GuelmimDataImporter(private val context: Context) {
    private val TAG = "GuelmimDataImporter"
    private val db = FirestoreHelper().getDb()
    private val LOCATIONS_COLLECTION = "locations"
    
    // Coordinates for Guelmim city center
    private val GUELMIM_LAT = 28.9865
    private val GUELMIM_LON = -10.0572
    private val RADIUS_KM = 20 // Expanded to 20km radius around city center
    
    // Overpass API endpoints - using multiple mirrors to prevent rate limiting
    private val OVERPASS_APIS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    )
    
    private var currentApiIndex = 0
    
    /**
     * Location data class - matches structure in RouteLocationsRepository
     */
    data class Location(
        val id: String = UUID.randomUUID().toString().substring(0, 8),
        val name: String = "",
        val type: String = "poi", // poi, bus_stop, school, university, hospital, etc.
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val city: String = "Guelmim",
        val region: String = "Guelmim-Oued Noun",
        val address: String = "",
        val description: String = ""
    )
    
    /**
     * Main function to import all types of data
     */
    suspend fun importAllData(progressCallback: (Float, String) -> Unit): Int {
        var totalLocationsCollected = 0
        val allLocations = mutableListOf<Location>()
        
        try {
            // Check if database already has substantial amount of data
            val existingCount = checkExistingLocationsCount()
            if (existingCount > 100) {
                Log.d(TAG, "Database already has $existingCount locations, skipping import")
                progressCallback(1.0f, "Base de données déjà remplie avec $existingCount emplacements")
                return existingCount
            }
        
            // 1. Schools and educational institutions
            progressCallback(0.05f, "Récupération des écoles et universités...")
            val schools = fetchSchools()
            allLocations.addAll(schools)
            totalLocationsCollected += schools.size
            progressCallback(0.15f, "Collecté ${schools.size} écoles et universités")
            
            // 2. Important amenities
            progressCallback(0.15f, "Récupération des services (hôpitaux, banques, etc.)...")
            val amenities = fetchAmenities() 
            allLocations.addAll(amenities)
            totalLocationsCollected += amenities.size
            progressCallback(0.30f, "Collecté ${amenities.size} services publics")
            
            // 3. Public transport stops
            progressCallback(0.30f, "Récupération des arrêts de bus et stations...")
            val transport = fetchPublicTransport()
            allLocations.addAll(transport)
            totalLocationsCollected += transport.size
            progressCallback(0.45f, "Collecté ${transport.size} arrêts de bus")
            
            // 4. Commercial places
            progressCallback(0.45f, "Récupération des commerces...")
            val commercial = fetchCommercialPlaces()
            allLocations.addAll(commercial)
            totalLocationsCollected += commercial.size
            progressCallback(0.60f, "Collecté ${commercial.size} commerces")
            
            // 5. Streets and roads
            progressCallback(0.60f, "Récupération des routes principales...")
            val roads = fetchMajorRoads()
            allLocations.addAll(roads)
            totalLocationsCollected += roads.size
            progressCallback(0.75f, "Collecté ${roads.size} routes")
            
            // 6. Tourist attractions and landmarks
            progressCallback(0.75f, "Récupération des attractions touristiques...")
            val landmarks = fetchLandmarks()
            allLocations.addAll(landmarks)
            totalLocationsCollected += landmarks.size
            progressCallback(0.85f, "Collecté ${landmarks.size} attractions")
            
            // 7. Residential areas and neighborhoods
            progressCallback(0.85f, "Récupération des quartiers résidentiels...")
            val residential = fetchResidentialAreas()
            allLocations.addAll(residential)
            totalLocationsCollected += residential.size
            progressCallback(0.90f, "Collecté ${residential.size} quartiers résidentiels")
            
            // Save all locations in one batch operation
            progressCallback(0.95f, "Enregistrement de $totalLocationsCollected emplacements dans la base de données...")
            val savedCount = saveLocationsInBatch(allLocations)
            
            Log.d(TAG, "Successfully imported $savedCount out of $totalLocationsCollected locations")
            progressCallback(1.0f, "Importation terminée : $savedCount emplacements enregistrés")
            
            // Mark importer as complete
            markImportComplete()
            
            return savedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing data: ${e.message}", e)
            progressCallback(1.0f, "Erreur: ${e.message}")
            throw e
        }
    }
    
    /**
     * Check if database already has locations
     */
    private suspend fun checkExistingLocationsCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                // Temporarily bypass the check and always return 0 to force import
                Log.d(TAG, "Bypassing existing locations check to force reimport")
                return@withContext 0
                
                /* Original code commented out for now
                val snapshot = db.collection(LOCATIONS_COLLECTION)
                    .get()
                    .await()
                
                return@withContext snapshot.size()
                */
            } catch (e: Exception) {
                Log.e(TAG, "Error checking existing locations count", e)
                return@withContext 0
            }
        }
    }
    
    /**
     * Save all locations in a batch operation
     */
    private suspend fun saveLocationsInBatch(locations: List<Location>): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting batch save of ${locations.size} locations")
                var savedCount = 0
                
                // Process in batches of 500 (Firestore limit for batch operations)
                val batchSize = 500
                val batches = locations.chunked(batchSize)
                
                for (batch in batches) {
                    val uniqueLocations = removeDuplicates(batch)
                    Log.d(TAG, "Processing batch with ${uniqueLocations.size} unique locations")
                    
                    var writeBatch = db.batch()
                    var operationsInCurrentBatch = 0
                    
                    for (location in uniqueLocations) {
                        val docRef = db.collection(LOCATIONS_COLLECTION).document()
                        writeBatch.set(docRef, location)
                        operationsInCurrentBatch++
                        savedCount++
                        
                        // Firestore has a limit of 500 operations per batch
                        if (operationsInCurrentBatch >= 499) {
                            writeBatch.commit().await()
                            Log.d(TAG, "Committed batch with $operationsInCurrentBatch operations")
                            writeBatch = db.batch()
                            operationsInCurrentBatch = 0
                        }
                    }
                    
                    // Commit any remaining operations
                    if (operationsInCurrentBatch > 0) {
                        writeBatch.commit().await()
                        Log.d(TAG, "Committed final batch with $operationsInCurrentBatch operations")
                    }
                }
                
                Log.d(TAG, "Successfully saved $savedCount locations")
                return@withContext savedCount
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving locations in batch: ${e.message}", e)
                return@withContext 0
            }
        }
    }
    
    /**
     * Remove duplicate locations from a list
     */
    private fun removeDuplicates(locations: List<Location>): List<Location> {
        val uniqueMap = mutableMapOf<String, Location>()
        
        for (location in locations) {
            // Use name and coordinates as the key for uniqueness
            val key = "${location.name}_${location.latitude}_${location.longitude}"
            uniqueMap[key] = location
        }
        
        return uniqueMap.values.toList()
    }
    
    /**
     * Mark the import as complete by storing a flag in shared preferences
     */
    private fun markImportComplete() {
        try {
            val prefs = context.getSharedPreferences("import_data", Context.MODE_PRIVATE)
            val currentTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())
            
            prefs.edit()
                .putString("last_import_time", currentTime)
                .putBoolean("import_complete", true)
                .apply()
                
            Log.d(TAG, "Import marked as complete at $currentTime")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking import as complete", e)
        }
    }
    
    /**
     * Fetch schools and educational institutions with more comprehensive tags
     */
    private suspend fun fetchSchools(): List<Location> {
        val query = """
            [out:json];
            (
              // Standard educational facilities
              node["amenity"="school"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="school"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="university"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="university"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="college"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="college"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="kindergarten"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="kindergarten"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="language_school"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="language_school"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="music_school"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["building"="school"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["building"="school"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["building"="university"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["building"="university"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Specific search for common Moroccan educational institution naming patterns
              node["name"~"[Éé]cole|[Ll]ycée|[Cc]ollège|[Ii]nstitut|[Uu]niversité|[Ff]aculté|EST|ISTA|OFPPT|[Cc]entre [Ff]ormation|[Mm]adrasa|مدرسة|كلية|معهد|جامعة"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["name"~"[Éé]cole|[Ll]ycée|[Cc]ollège|[Ii]nstitut|[Uu]niversité|[Ff]aculté|EST|ISTA|OFPPT|[Cc]entre [Ff]ormation|[Mm]adrasa|مدرسة|كلية|معهد|جامعة"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Known educational institutions in Guelmim by specific names
              node["name"~"Ibn Zohr|Ibn Sina|Hassan II|Mohammed V|Al Massira|[Aa]l [Ii]rfane|[Qq]uartier [Aa]dministratif"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["name"~"Ibn Zohr|Ibn Sina|Hassan II|Mohammed V|Al Massira|[Aa]l [Ii]rfane|[Qq]uartier [Aa]dministratif"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
            );
            out center;
        """.trimIndent()
        
        return executeOverpassQuery(query, "school")
    }
    
    /**
     * Fetch important amenities (hospitals, banks, etc.) with more categories
     */
    private suspend fun fetchAmenities(): List<Location> {
        val query = """
            [out:json];
            (
              // Healthcare facilities
              node["amenity"="hospital"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="hospital"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="clinic"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="clinic"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="doctors"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="dentist"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["healthcare"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["healthcare"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Emergency services
              node["amenity"="police"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="police"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="fire_station"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="fire_station"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="ambulance_station"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Government and civic services
              node["amenity"="townhall"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="townhall"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["office"="government"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["office"="government"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="post_office"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="library"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="library"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Financial services
              node["amenity"="bank"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="atm"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Health services
              node["amenity"="pharmacy"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Religious places
              node["amenity"="place_of_worship"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="place_of_worship"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Commercial places
              node["amenity"="marketplace"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="marketplace"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="restaurant"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="cafe"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Search for common Moroccan service names
              node["name"~"(?i)gendarmerie|(?i)police|(?i)commissariat|(?i)hôpital|(?i)dispensaire|(?i)mairie|(?i)préfecture|(?i)tribunal|(?i)poste|مستشفى|مركز صحي|مركز الشرطة|بلدية|محكمة|بريد"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["name"~"(?i)gendarmerie|(?i)police|(?i)commissariat|(?i)hôpital|(?i)dispensaire|(?i)mairie|(?i)préfecture|(?i)tribunal|(?i)poste|مستشفى|مركز صحي|مركز الشرطة|بلدية|محكمة|بريد"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Known important institutions in Guelmim
              node["name"~"Centre Hospitalier|Hôpital Hassan II|CHU|Sûreté Nationale|Protection Civile|Barid Al-Maghrib|Prefecture|Tribunal|Municipalité|Commune"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["name"~"Centre Hospitalier|Hôpital Hassan II|CHU|Sûreté Nationale|Protection Civile|Barid Al-Maghrib|Prefecture|Tribunal|Municipalité|Commune"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
            );
            out center;
        """.trimIndent()
        
        return executeOverpassQuery(query, "poi")
    }
    
    /**
     * Fetch public transport related objects (bus stops, stations) with more detailed queries
     */
    private suspend fun fetchPublicTransport(): List<Location> {
        val query = """
            [out:json];
            (
              node["highway"="bus_stop"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["public_transport"="stop_position"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["public_transport"="platform"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["public_transport"="station"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["public_transport"="station"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="bus_station"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="bus_station"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="taxi"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="car_rental"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="car_sharing"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="bicycle_rental"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Try to pick up bus stops with no proper tagging but with bus in name
              node["name"~"(?i)bus|(?i)stop|(?i)station|(?i)gare|(?i)arrêt"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
            );
            out center;
        """.trimIndent()
        
        return executeOverpassQuery(query, "bus_stop")
    }
    
    /**
     * Fetch commercial locations (shops, markets, malls) with more categories
     */
    private suspend fun fetchCommercialPlaces(): List<Location> {
        val query = """
            [out:json];
            (
              node["shop"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["shop"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="marketplace"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="marketplace"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["shop"="supermarket"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["shop"="supermarket"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["shop"="mall"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["shop"="mall"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="cafe"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="restaurant"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="fast_food"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["shop"="bakery"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["shop"="butcher"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["shop"="convenience"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["shop"="greengrocer"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Souk and market related terms in French and Arabic
              node["name"~"(?i)souk|(?i)marché|(?i)bazar|سوق"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["name"~"(?i)souk|(?i)marché|(?i)bazar|سوق"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
            );
            out center;
        """.trimIndent()
        
        return executeOverpassQuery(query, "commercial")
    }
    
    /**
     * Fetch major roads and streets with better categorization
     */
    private suspend fun fetchMajorRoads(): List<Location> {
        val query = """
            [out:json];
            (
              // Main roads
              way["highway"="primary"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["highway"="secondary"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["highway"="tertiary"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["highway"="residential"]["name"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["highway"="unclassified"]["name"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["highway"="trunk"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["highway"="trunk_link"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Important junctions and landmarks
              node["highway"="motorway_junction"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["highway"="crossing"]["crossing"="traffic_signals"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Important streets in Guelmim by name patterns
              way["name"~"(?i)avenue|(?i)boulevard|(?i)route|(?i)rue|(?i)place|(?i)rond[- ]?point|(?i)carrefour|شارع|طريق|ساحة|دوار"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Main streets in Guelmim by specific common names
              way["name"~"Hassan II|Mohammed V|Mohammed VI|Moulay Ismail|Al Massira|Oued Noun|Tan Tan|Nation Unies|La Marche Verte|Bir Anzarane|Al Quds|Allal El Fassi"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Roundabouts (common landmark meeting points)
              node["highway"="mini_roundabout"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["junction"="roundabout"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
            );
            out center;
        """.trimIndent()
        
        return executeOverpassQuery(query, "street")
    }
    
    /**
     * Fetch tourist attractions and landmarks
     */
    private suspend fun fetchLandmarks(): List<Location> {
        val query = """
            [out:json];
            (
              node["tourism"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["tourism"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["historic"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["historic"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["leisure"="park"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["leisure"="park"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["leisure"="stadium"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["leisure"="stadium"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["leisure"="sports_centre"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["leisure"="sports_centre"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["leisure"="garden"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["leisure"="garden"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="cinema"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="cinema"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["amenity"="theatre"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["amenity"="theatre"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              
              // Monument, landmark or viewpoint
              node["tourism"="attraction"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["tourism"="attraction"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["tourism"="viewpoint"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              node["natural"="peak"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
            );
            out center;
        """.trimIndent()
        
        return executeOverpassQuery(query, "landmark")
    }
    
    /**
     * Fetch residential areas and neighborhoods
     */
    private suspend fun fetchResidentialAreas(): List<Location> {
        val query = """
            [out:json];
            (
              way["landuse"="residential"]["name"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              relation["landuse"="residential"]["name"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["place"="neighbourhood"]["name"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              relation["place"="neighbourhood"]["name"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["place"="suburb"]["name"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              relation["place"="suburb"]["name"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              way["place"="quarter"]["name"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
              relation["place"="quarter"]["name"](around:${RADIUS_KM * 1000},${GUELMIM_LAT},${GUELMIM_LON});
            );
            out center;
        """.trimIndent()
        
        return executeOverpassQuery(query, "neighborhood")
    }
    
    /**
     * Execute an Overpass API query with improved fault tolerance
     */
    private suspend fun executeOverpassQuery(query: String, locationType: String): List<Location> {
        return withContext(Dispatchers.IO) {
            val locations = mutableListOf<Location>()
            var apiAttempts = 0
            var success = false
            
            while (!success && apiAttempts < OVERPASS_APIS.size * 2) {
                try {
                    val apiUrl = OVERPASS_APIS[currentApiIndex % OVERPASS_APIS.size]
                    currentApiIndex++
                    
                    Log.d(TAG, "Executing Overpass query for $locationType on $apiUrl")
                    
                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                    val urlString = "$apiUrl?data=$encodedQuery"
                    val url = URL(urlString)
                    
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 20000
                    connection.readTimeout = 20000
                    
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        
                        reader.close()
                        
                        // Parse the JSON response
                        val jsonResponse = JSONObject(response.toString())
                        val elements = jsonResponse.getJSONArray("elements")
                        
                        for (i in 0 until elements.length()) {
                            val element = elements.getJSONObject(i)
                            val type = element.getString("type")
                            
                            var lat = 0.0
                            var lon = 0.0
                            var name = ""
                            
                            // Handle different element types
                            when (type) {
                                "node" -> {
                                    lat = element.getDouble("lat")
                                    lon = element.getDouble("lon")
                                    if (element.has("tags") && element.getJSONObject("tags").has("name")) {
                                        name = element.getJSONObject("tags").getString("name")
                                    }
                                }
                                "way", "relation" -> {
                                    // For ways and relations, use the center coordinates if available
                                    if (element.has("center")) {
                                        lat = element.getJSONObject("center").getDouble("lat")
                                        lon = element.getJSONObject("center").getDouble("lon")
                                    }
                                    
                                    // Try multiple language versions of name
                                    if (element.has("tags")) {
                                        val tags = element.getJSONObject("tags")
                                        if (tags.has("name")) {
                                            name = tags.getString("name")
                                        } else if (tags.has("name:fr")) {
                                            name = tags.getString("name:fr")
                                        } else if (tags.has("name:ar")) {
                                            name = tags.getString("name:ar")
                                        } else if (tags.has("int_name")) {
                                            name = tags.getString("int_name")
                                        }
                                    }
                                }
                            }
                            
                            if (lat != 0.0 && lon != 0.0) {
                                // Determine more specific type if possible
                                var specificType = locationType
                                if (element.has("tags")) {
                                    val tags = element.getJSONObject("tags")
                                    if (tags.has("amenity")) {
                                        val amenity = tags.getString("amenity")
                                        specificType = when (amenity) {
                                            "school" -> "school"
                                            "university", "college" -> "university"
                                            "hospital", "clinic" -> "hospital"
                                            "pharmacy" -> "pharmacy"
                                            "bank" -> "bank"
                                            "police" -> "police"
                                            "bus_station" -> "bus_stop"
                                            "marketplace" -> "market"
                                            "restaurant", "cafe", "fast_food" -> "restaurant"
                                            "place_of_worship" -> "religious"
                                            else -> locationType
                                        }
                                    } else if (tags.has("highway") && tags.getString("highway") == "bus_stop") {
                                        specificType = "bus_stop"
                                    } else if (tags.has("shop")) {
                                        specificType = "commercial"
                                    } else if (tags.has("tourism")) {
                                        specificType = "landmark"
                                    } else if (tags.has("historic")) {
                                        specificType = "landmark"
                                    }
                                }
                                
                                // Create a description from available tags
                                val description = buildDescription(element)
                                
                                // If no name is available, create a generic one based on type
                                if (name.isBlank()) {
                                    name = when (specificType) {
                                        "street" -> "Rue à Guelmim"
                                        "school" -> "École à Guelmim"
                                        "university" -> "Établissement éducatif à Guelmim"
                                        "hospital" -> "Établissement médical à Guelmim"
                                        "bus_stop" -> "Arrêt de bus à Guelmim"
                                        "commercial" -> "Établissement commercial à Guelmim"
                                        "restaurant" -> "Restaurant à Guelmim"
                                        "landmark" -> "Point d'intérêt à Guelmim"
                                        "neighborhood" -> "Quartier à Guelmim"
                                        else -> "Point d'intérêt à Guelmim"
                                    }
                                }
                                
                                // Create Location object
                                val location = Location(
                                    id = UUID.randomUUID().toString().substring(0, 8),
                                    name = name,
                                    type = specificType,
                                    latitude = lat,
                                    longitude = lon,
                                    city = "Guelmim",
                                    region = "Guelmim-Oued Noun",
                                    description = description
                                )
                                
                                locations.add(location)
                            }
                        }
                        
                        Log.d(TAG, "Found ${locations.size} locations of type $locationType")
                        success = true
                        
                    } else {
                        Log.e(TAG, "HTTP error response code: $responseCode")
                        apiAttempts++
                        
                        // Wait a bit before trying again
                        Thread.sleep(500)
                    }
                    
                    connection.disconnect()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing Overpass query: ${e.message}", e)
                    apiAttempts++
                    
                    // Wait a bit before trying again
                    Thread.sleep(1000)
                }
            }
            
            if (!success) {
                Log.w(TAG, "All API attempt failures for $locationType query")
            }
            
            return@withContext locations
        }
    }
    
    /**
     * Build a description string from element tags with improved formatting
     */
    private fun buildDescription(element: JSONObject): String {
        val description = StringBuilder()
        
        if (element.has("tags")) {
            val tags = element.getJSONObject("tags")
            val keys = tags.keys()
            
            // Add relevant tags to description
            val relevantTags = listOf(
                "name", "name:ar", "name:fr", "addr:street", "addr:city", 
                "phone", "website", "operator", "amenity", "shop", "highway",
                "opening_hours", "contact:phone", "contact:email", "brand", "leisure",
                "tourism", "historic", "landuse", "internet_access", "place"
            )
            
            while (keys.hasNext()) {
                val key = keys.next()
                if (key in relevantTags) {
                    val value = tags.getString(key)
                    if (value.isNotEmpty() && key != "name") {
                        if (description.isNotEmpty()) description.append(", ")
                        
                        // Format key nicely
                        val formattedKey = key.replace(":", " ").replace("_", " ")
                            .split(" ")
                            .joinToString(" ") { word ->
                                word.replaceFirstChar { 
                                    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
                                }
                            }
                            
                        description.append("$formattedKey: $value")
                    }
                }
            }
        }
        
        return description.toString()
    }
} 