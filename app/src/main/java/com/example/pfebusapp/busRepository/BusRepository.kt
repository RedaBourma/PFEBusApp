package com.example.pfebusapp.busRepository

import android.util.Log
import com.example.pfebusapp.firebase.FirestoreHelper
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BusRepository {
    private val TAG = "BusRepository"
    private val firestoreHelper = FirestoreHelper()
    private val db = firestoreHelper.getDb()

//    suspend fun getAllBusData(): List<Bus>? = try {
//        val snapshot = db.collection("users").limit(100).get().await()
//        Log.d(TAG, "Query completed. Documents count: ${snapshot.size()}")
//        snapshot.toObjects(Bus::class.java)
//    }catch (e: Exception){
//        Log.e(TAG, "getAllBusData: failed to get data", e)
//        null
//    }

    fun getAllBusData(
        onSuccess: (List<Bus>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting bus data fetch from collection: ${FirestoreHelper.BUS_COLLECTION}")
                val snapshot = db.collection(FirestoreHelper.BUS_COLLECTION).get().await()
                Log.d(TAG, "Query completed. Documents count: ${snapshot.size()}")
                
                if (snapshot.isEmpty) {
                    Log.w(TAG, "No documents found in collection ${FirestoreHelper.BUS_COLLECTION}")
                    onSuccess(emptyList())
                    return@launch
                }

                val buses = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: run {
                            Log.e(TAG, "Document ${doc.id} has null data")
                            return@mapNotNull null
                        }
                        
                        Log.d(TAG, "Processing document ${doc.id}")
                        
                        // Get position directly as GeoPoint
                        val position = data["position"] as? GeoPoint ?: GeoPoint(0.0, 0.0)
                        Log.d(TAG, "Position: $position")
                        
                        // Convert trajet array
                        val trajetList = (data["trajet"] as? List<*>)?.mapNotNull { trajetItem ->
                            if (trajetItem is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                (trajetItem as Map<String, GeoPoint>).toMap()
                            } else {
                                Log.e(TAG, "Invalid trajet item type: ${trajetItem?.javaClass?.simpleName}")
                                null
                            }
                        } ?: listOf()
                        Log.d(TAG, "Trajet list: $trajetList")

                        // Get busStop reference
                        val busStopRef = data["busStop"] as? com.google.firebase.firestore.DocumentReference
                        
                        // Get busStops array
                        val busStopsRefs = (data["busStops"] as? List<*>)?.mapNotNull { 
                            it as? com.google.firebase.firestore.DocumentReference 
                        } ?: listOf()

                        val bus = Bus(
                            num = data["num"] as? String ?: "",
                            immatriculation = data["immatriculation"] as? String ?: "",
                            marque = data["marque"] as? String ?: "",
                            nbrPlace = (data["nbrPlace"] as? Number)?.toInt() ?: 0,
                            status = data["status"] as? String ?: "",
                            position = position,
                            trajet = trajetList,
                            busStop = busStopRef,
                            busStops = busStopsRefs
                        )
                        Log.d(TAG, "Successfully created Bus object: $bus")
                        bus
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document ${doc.id}", e)
                        null
                    }
                }

                Log.d(TAG, "Total buses converted: ${buses.size}")
                if (buses.isEmpty()) {
                    Log.w(TAG, "No buses were successfully converted from the documents")
                }
                
                if (buses.isNotEmpty()) {
                    val firstBus = buses.first()
                    Log.d(TAG, "First bus details:")
                    Log.d(TAG, "- num: ${firstBus.num}")
                    Log.d(TAG, "- marque: ${firstBus.marque}")
                    Log.d(TAG, "- immatriculation: ${firstBus.immatriculation}")
                    Log.d(TAG, "- position: ${firstBus.position}")
                    Log.d(TAG, "- trajet size: ${firstBus.trajet.size}")
                    Log.d(TAG, "- busStop: ${firstBus.busStop}")
                    Log.d(TAG, "- busStops size: ${firstBus.busStops.size}")
                }
                
                onSuccess(buses)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch bus data", e)
                onFailure(e)
            }
        }
    }



//    fun getAllBusData(): List<Bus>? {
//        db.collection(FirestoreHelper.BUS_COLLECTION).get()
//            .addOnSuccessListener { result ->
//                for (document in result){
//                    Log.d("ss", "${document.id} => ${document.data}")
//
//                }
//                return@addOnSuccessListener result.toObjects(Bus::class.java)
//            }.addOnFailureListener { exception ->
//                Log.w("ss", "Error getting documents.", exception)
//                return@addOnFailureListener null
//            }
//    }
}

//
//    init {
//        Log.d(TAG, "BusRepository initialized")
//        // Check if the collection exists
//        firestoreHelper.checkCollectionExists(FirestoreHelper.BUS_COLLECTION)
//    }
//
//    suspend fun getAllBusData(): List<Bus>? = withContext(Dispatchers.IO) {
//        try {
//            Log.d(TAG, "getAllBusData: Starting fetch from '${FirestoreHelper.BUS_COLLECTION}' collection")
//
//            // Try to get collection metadata first
//            try {
//                val metaRef = db.collection(FirestoreHelper.BUS_COLLECTION)
//                Log.d(TAG, "Collection reference path: ${metaRef.path}")
//            } catch (e: Exception) {
//                Log.e(TAG, "Error getting collection reference", e)
//            }
//
//            // Set a timeout for the Firestore query
//            withTimeout(10000L) { // 10 seconds timeout
//                val snapshot = db.collection(FirestoreHelper.BUS_COLLECTION).get().await()
//                Log.d(TAG, "Query completed. Documents count: ${snapshot.size()}")
//
//                if (snapshot.isEmpty) {
//                    Log.w(TAG, "No documents found in collection ${FirestoreHelper.BUS_COLLECTION}")
//                    return@withTimeout emptyList()
//                }
//
//                val buses = snapshot.toObjects(Bus::class.java)
//                Log.d(TAG, "getAllBusData: Fetch completed, found ${buses.size} buses")
//
//                if (buses.isNotEmpty()) {
//                    val firstBus = buses.first()
//                    Log.d(TAG, "First bus: num=${firstBus.num}, marque=${firstBus.marque}, immatriculation=${firstBus.immatriculation}")
//                    Log.d(TAG, "First bus position: ${firstBus.position}")
//                    Log.d(TAG, "First bus trajet size: ${firstBus.trajet.size}")
//                }
//
//                buses
//            }
//        } catch (e: FirebaseFirestoreException) {
//            Log.e(TAG, "Firestore exception: ${e.code}, ${e.message}", e)
//            null
//        } catch (e: Exception) {
//            Log.e(TAG, "getAllBusData: failed to get data: ${e.javaClass.simpleName}: ${e.message}", e)
//            null
//        }
//    }

    // For testing when Firestore is not working
//    fun getMockBusData(): List<Bus> {
//        Log.d(TAG, "getMockBusData: Creating mock data")
//        return listOf(
//            Bus(
//                num = "1",
//                immatriculation = "2334/64",
//                marque = "Mercedes",
//                nbrPlace = 20,
//                status = "retour",
//                position = listOf(28.9865, -10.0572), // Guelmim coordinates
//                trajet = listOf(
//                    listOf(28.9865, -10.0572), // Start
//                    listOf(28.9855, -10.0562), // Point 1
//                    listOf(28.9845, -10.0552)  // Point 2
//                )
//            ),
//            Bus(
//                num = "2",
//                immatriculation = "5678/64",
//                marque = "Volvo",
//                nbrPlace = 30,
//                status = "aller",
//                position = listOf(28.9855, -10.0582),
//                trajet = listOf(
//                    listOf(28.9855, -10.0582), // Start
//                    listOf(28.9865, -10.0592), // Point 1
//                    listOf(28.9875, -10.0602)  // Point 2
//                )
//            ),
//            Bus(
//                num = "3",
//                immatriculation = "9012/64",
//                marque = "MAN",
//                nbrPlace = 25,
//                status = "arret",
//                position = listOf(28.9875, -10.0562),
//                trajet = listOf(
//                    listOf(28.9875, -10.0562) // Parked at station
//                )
//            )
//        )
//    }
