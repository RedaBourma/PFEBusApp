package com.example.pfebusapp.routes

import android.location.Location
import android.util.Log
import com.example.pfebusapp.busRepository.Bus
import com.google.firebase.firestore.GeoPoint
import java.util.PriorityQueue
import kotlin.math.min
import kotlin.math.max

/**
 * Advanced route finding algorithm to find optimal bus routes 
 * between two points in Guelmim.
 */
class RouteFinder {
    companion object {
        private const val TAG = "RouteFinder"
        private const val MAX_WALKING_DISTANCE = 1500.0 // meters - increased for better coverage
        private const val MAX_TRANSFER_DISTANCE = 600.0 // meters
        private const val MAX_TRANSFERS = 2 // Maximum number of transfers to consider
        
        /**
         * Calculate distance between two GeoPoints in meters
         */
        fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
            val results = FloatArray(1)
            Location.distanceBetween(
                point1.latitude, point1.longitude,
                point2.latitude, point2.longitude,
                results
            )
            return results[0].toDouble()
        }
        
        /**
         * Check if a point is near another
         */
        fun isNearPoint(point1: GeoPoint, point2: GeoPoint, maxDistanceMeters: Double = 1000.0): Boolean {
            return calculateDistance(point1, point2) <= maxDistanceMeters
        }
    }

    /**
     * Represents a segment of a route that includes a bus and stops
     */
    data class RouteSegment(
        val bus: Bus,
        val startStop: GeoPoint,
        val endStop: GeoPoint,
        val walkingDistanceToStart: Double,
        val walkingDistanceFromEnd: Double,
        val busTravelDistance: Double
    ) {
        val totalSegmentDistance: Double
            get() = walkingDistanceToStart + busTravelDistance + walkingDistanceFromEnd
            
        // For display
        val startStopName: String
            get() {
                val nearestStop = bus.stops.minByOrNull { stop ->
                    calculateDistance(
                        GeoPoint(stop.latitude, stop.longitude),
                        startStop
                    )
                }
                return nearestStop?.name ?: "Bus Stop"
            }
            
        val endStopName: String
            get() {
                val nearestStop = bus.stops.minByOrNull { stop ->
                    calculateDistance(
                        GeoPoint(stop.latitude, stop.longitude),
                        endStop
                    )
                }
                return nearestStop?.name ?: "Bus Stop"
            }
    }

    /**
     * Represents a complete route with multiple segments
     */
    data class Route(
        val segments: List<RouteSegment>,
        val totalWalkingDistance: Double,
        val totalBusDistance: Double,
        val numberOfTransfers: Int
    ) {
        val totalDistance: Double
            get() = totalWalkingDistance + totalBusDistance
    }

    /**
     * Find the best routes between start and end points
     * @param buses List of available buses
     * @param start Starting location
     * @param end Destination location
     * @return List of possible routes sorted by optimality
     */
    fun findOptimalRoute(buses: List<Bus>, start: GeoPoint, end: GeoPoint): List<Route> {
        Log.d(TAG, "Finding routes from $start to $end using ${buses.size} buses")
        val possibleRoutes = mutableListOf<Route>()
        
        try {
            // Check for very close points - just walk if it's a short distance
            val directDistance = RouteFinder.calculateDistance(start, end)
            if (directDistance <= MAX_WALKING_DISTANCE) {
                Log.d(TAG, "Destination is within walking distance ($directDistance meters)")
                val walkingRoute = createWalkingRoute(start, end, directDistance)
                possibleRoutes.add(walkingRoute)
            }
            
            // Find direct routes (single bus)
            buses.forEach { bus ->
                findDirectRoute(bus, start, end)?.let { route ->
                    possibleRoutes.add(route)
                    Log.d(TAG, "Found direct route using bus ${bus.busNumber}")
                }
            }
    
            // Find routes with one transfer
            buses.forEach { firstBus ->
                buses.forEach { secondBus ->
                    if (firstBus.busNumber != secondBus.busNumber) {
                        findRouteWithTransfer(firstBus, secondBus, start, end)?.let { route ->
                            possibleRoutes.add(route)
                            Log.d(TAG, "Found route with transfer from ${firstBus.busNumber} to ${secondBus.busNumber}")
                        }
                    }
                }
            }
            
            // Find routes with two transfers (for complex journeys)
            if (possibleRoutes.isEmpty() || possibleRoutes.all { it.totalWalkingDistance > MAX_WALKING_DISTANCE }) {
                Log.d(TAG, "Looking for routes with 2 transfers...")
                buses.forEach { firstBus ->
                    buses.forEach { secondBus ->
                        if (firstBus.busNumber != secondBus.busNumber) {
                            buses.forEach { thirdBus ->
                                if (secondBus.busNumber != thirdBus.busNumber && 
                                    firstBus.busNumber != thirdBus.busNumber) {
                                    findRouteWithDoubleTransfer(
                                        firstBus, secondBus, thirdBus, start, end
                                    )?.let { route ->
                                        possibleRoutes.add(route)
                                        Log.d(TAG, "Found route with double transfer: ${firstBus.busNumber} -> ${secondBus.busNumber} -> ${thirdBus.busNumber}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
    
            // Sort routes by a combination of factors for best overall experience
            val sortedRoutes = possibleRoutes.sortedWith(
                compareBy<Route> { 
                    // Prioritize routes with smaller total walking distances
                    it.totalWalkingDistance 
                }.thenBy { 
                    // Then fewer transfers
                    it.numberOfTransfers 
                }.thenBy { 
                    // Then overall distance
                    it.totalDistance
                }
            )
            
            Log.d(TAG, "Found ${sortedRoutes.size} possible routes")
            return sortedRoutes
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding routes: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Create a simple walking route when points are very close
     */
    private fun createWalkingRoute(start: GeoPoint, end: GeoPoint, distance: Double): Route {
        // Create a "virtual" bus for the walking segment
        val walkingBus = Bus(
            num = "Walk",
            marque = "Walking",
            trajet = emptyList(),
            stops = emptyList()
        )
        
        val segment = RouteSegment(
            bus = walkingBus,
            startStop = start,
            endStop = end,
            walkingDistanceToStart = 0.0,
            walkingDistanceFromEnd = 0.0,
            busTravelDistance = distance
        )
        
        return Route(
            segments = listOf(segment),
            totalWalkingDistance = distance,
            totalBusDistance = 0.0,
            numberOfTransfers = 0
        )
    }

    /**
     * Find a direct route using a single bus
     */
    private fun findDirectRoute(bus: Bus, start: GeoPoint, end: GeoPoint): Route? {
        var bestSegment: RouteSegment? = null
        var minTotalDistance = Double.MAX_VALUE

        try {
            // Process each pair of stops on the route
            for (i in bus.stops.indices) {
                val startStop = bus.stops[i]
                val startStopPoint = GeoPoint(startStop.latitude, startStop.longitude)
                val distanceToStartStop = RouteFinder.calculateDistance(start, startStopPoint)
                
                // Only consider stops within walking distance
                if (distanceToStartStop <= MAX_WALKING_DISTANCE) {
                    for (j in i until bus.stops.size) {
                        val endStop = bus.stops[j]
                        val endStopPoint = GeoPoint(endStop.latitude, endStop.longitude)
                        val distanceFromEndStop = RouteFinder.calculateDistance(endStopPoint, end)
                        
                        // Only consider stops within walking distance of destination
                        if (distanceFromEndStop <= MAX_WALKING_DISTANCE) {
                            // Calculate distance traveled by bus between these stops
                            val busTravelDistance = calculateBusDistance(bus, i, j)
                            
                            // Calculate total distance including walking
                            val totalDistance = distanceToStartStop + busTravelDistance + distanceFromEndStop
                            
                            if (totalDistance < minTotalDistance) {
                                minTotalDistance = totalDistance
                                bestSegment = RouteSegment(
                                    bus = bus,
                                    startStop = startStopPoint,
                                    endStop = endStopPoint,
                                    walkingDistanceToStart = distanceToStartStop,
                                    walkingDistanceFromEnd = distanceFromEndStop,
                                    busTravelDistance = busTravelDistance
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in findDirectRoute: ${e.message}", e)
        }

        return bestSegment?.let {
            Route(
                segments = listOf(it),
                totalWalkingDistance = it.walkingDistanceToStart + it.walkingDistanceFromEnd,
                totalBusDistance = it.busTravelDistance,
                numberOfTransfers = 0
            )
        }
    }

    /**
     * Find a route that requires one bus transfer
     */
    private fun findRouteWithTransfer(
        firstBus: Bus,
        secondBus: Bus,
        start: GeoPoint,
        end: GeoPoint
    ): Route? {
        var bestRoute: Route? = null
        var minTotalDistance = Double.MAX_VALUE

        try {
            // For each stop in the first bus
            for (i in firstBus.stops.indices) {
                val firstBusStartStop = firstBus.stops[i]
                val firstBusStartPoint = GeoPoint(firstBusStartStop.latitude, firstBusStartStop.longitude)
                val walkingToFirstBus = RouteFinder.calculateDistance(start, firstBusStartPoint)
                
                // Only consider stops within walking distance
                if (walkingToFirstBus <= MAX_WALKING_DISTANCE) {
                    // For each possible exit stop on first bus
                    for (j in i until firstBus.stops.size) {
                        val firstBusEndStop = firstBus.stops[j]
                        val firstBusEndPoint = GeoPoint(firstBusEndStop.latitude, firstBusEndStop.longitude)
                        
                        // Calculate distance traveled on first bus
                        val firstBusTravelDistance = calculateBusDistance(firstBus, i, j)
                        
                        // For each potential second bus
                        for (k in secondBus.stops.indices) {
                            val secondBusStartStop = secondBus.stops[k]
                            val secondBusStartPoint = GeoPoint(secondBusStartStop.latitude, secondBusStartStop.longitude)
                            
                            // Calculate transfer distance
                            val transferDistance = RouteFinder.calculateDistance(firstBusEndPoint, secondBusStartPoint)
                            
                            // Only consider transfers within reasonable walking distance
                            if (transferDistance <= MAX_TRANSFER_DISTANCE) {
                                // For each destination stop on second bus
                                for (l in k until secondBus.stops.size) {
                                    val secondBusEndStop = secondBus.stops[l]
                                    val secondBusEndPoint = GeoPoint(secondBusEndStop.latitude, secondBusEndStop.longitude)
                                    val walkingToDestination = RouteFinder.calculateDistance(secondBusEndPoint, end)
                                    
                                    // Only consider final stops within walking distance of destination
                                    if (walkingToDestination <= MAX_WALKING_DISTANCE) {
                                        // Calculate distance on second bus
                                        val secondBusTravelDistance = calculateBusDistance(secondBus, k, l)
                                        
                                        // Calculate total distance
                                        val totalDistance = walkingToFirstBus + firstBusTravelDistance + 
                                                           transferDistance + 
                                                           secondBusTravelDistance + walkingToDestination
                                        
                                        if (totalDistance < minTotalDistance) {
                                            minTotalDistance = totalDistance
                                            
                                            val segment1 = RouteSegment(
                                                bus = firstBus,
                                                startStop = firstBusStartPoint,
                                                endStop = firstBusEndPoint,
                                                walkingDistanceToStart = walkingToFirstBus,
                                                walkingDistanceFromEnd = transferDistance / 2, // Split transfer distance
                                                busTravelDistance = firstBusTravelDistance
                                            )
                                            
                                            val segment2 = RouteSegment(
                                                bus = secondBus,
                                                startStop = secondBusStartPoint,
                                                endStop = secondBusEndPoint,
                                                walkingDistanceToStart = transferDistance / 2, // Split transfer distance
                                                walkingDistanceFromEnd = walkingToDestination,
                                                busTravelDistance = secondBusTravelDistance
                                            )
                                            
                                            bestRoute = Route(
                                                segments = listOf(segment1, segment2),
                                                totalWalkingDistance = walkingToFirstBus + transferDistance + walkingToDestination,
                                                totalBusDistance = firstBusTravelDistance + secondBusTravelDistance,
                                                numberOfTransfers = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in findRouteWithTransfer: ${e.message}", e)
        }

        return bestRoute
    }

    /**
     * Find a route that requires two bus transfers
     */
    private fun findRouteWithDoubleTransfer(
        firstBus: Bus,
        secondBus: Bus,
        thirdBus: Bus,
        start: GeoPoint,
        end: GeoPoint
    ): Route? {
        // First find all viable transfer points between buses
        val firstToSecondTransfers = findTransferPoints(firstBus, secondBus)
        val secondToThirdTransfers = findTransferPoints(secondBus, thirdBus)
        
        if (firstToSecondTransfers.isEmpty() || secondToThirdTransfers.isEmpty()) {
            return null
        }
        
        var bestRoute: Route? = null
        var bestTotalDistance = Double.MAX_VALUE
        
        try {
            // Find boarding points for first bus
            firstBus.stops.forEachIndexed { firstStartIndex, firstStartStop ->
                val firstStartPoint = GeoPoint(firstStartStop.latitude, firstStartStop.longitude)
                val walkToFirstBus = RouteFinder.calculateDistance(start, firstStartPoint)
                
                if (walkToFirstBus <= MAX_WALKING_DISTANCE) {
                    // For each viable transfer from first to second bus
                    firstToSecondTransfers.forEach { (firstEndIndex, secondStartIndex, transferDistance) ->
                        // For each viable transfer from second to third bus
                        secondToThirdTransfers.forEach { (secondEndIndex, thirdStartIndex, secondTransferDistance) ->
                            // Check that the second bus segment is valid (start before end)
                            if (secondStartIndex <= secondEndIndex) {
                                // Find ending points for third bus
                                thirdBus.stops.indices.forEach { thirdEndIndex ->
                                    if (thirdEndIndex >= thirdStartIndex) {
                                        val thirdEndStop = thirdBus.stops[thirdEndIndex]
                                        val thirdEndPoint = GeoPoint(thirdEndStop.latitude, thirdEndStop.longitude)
                                        val walkFromThirdBus = RouteFinder.calculateDistance(thirdEndPoint, end)
                                        
                                        if (walkFromThirdBus <= MAX_WALKING_DISTANCE) {
                                            // Calculate all segment distances
                                            val firstBusDistance = calculateBusDistance(firstBus, firstStartIndex, firstEndIndex)
                                            val secondBusDistance = calculateBusDistance(secondBus, secondStartIndex, secondEndIndex)
                                            val thirdBusDistance = calculateBusDistance(thirdBus, thirdStartIndex, thirdEndIndex)
                                            
                                            val totalWalking = walkToFirstBus + transferDistance + secondTransferDistance + walkFromThirdBus
                                            val totalBusDistance = firstBusDistance + secondBusDistance + thirdBusDistance
                                            val totalDistance = totalWalking + totalBusDistance
                                            
                                            if (totalDistance < bestTotalDistance) {
                                                bestTotalDistance = totalDistance
                                                
                                                // Create route segments
                                                val firstSegment = RouteSegment(
                                                    bus = firstBus,
                                                    startStop = firstStartPoint,
                                                    endStop = GeoPoint(
                                                        firstBus.stops[firstEndIndex].latitude,
                                                        firstBus.stops[firstEndIndex].longitude
                                                    ),
                                                    walkingDistanceToStart = walkToFirstBus,
                                                    walkingDistanceFromEnd = transferDistance / 2,
                                                    busTravelDistance = firstBusDistance
                                                )
                                                
                                                val secondSegment = RouteSegment(
                                                    bus = secondBus,
                                                    startStop = GeoPoint(
                                                        secondBus.stops[secondStartIndex].latitude,
                                                        secondBus.stops[secondStartIndex].longitude
                                                    ),
                                                    endStop = GeoPoint(
                                                        secondBus.stops[secondEndIndex].latitude,
                                                        secondBus.stops[secondEndIndex].longitude
                                                    ),
                                                    walkingDistanceToStart = transferDistance / 2,
                                                    walkingDistanceFromEnd = secondTransferDistance / 2,
                                                    busTravelDistance = secondBusDistance
                                                )
                                                
                                                val thirdSegment = RouteSegment(
                                                    bus = thirdBus,
                                                    startStop = GeoPoint(
                                                        thirdBus.stops[thirdStartIndex].latitude,
                                                        thirdBus.stops[thirdStartIndex].longitude
                                                    ),
                                                    endStop = thirdEndPoint,
                                                    walkingDistanceToStart = secondTransferDistance / 2,
                                                    walkingDistanceFromEnd = walkFromThirdBus,
                                                    busTravelDistance = thirdBusDistance
                                                )
                                                
                                                bestRoute = Route(
                                                    segments = listOf(firstSegment, secondSegment, thirdSegment),
                                                    totalWalkingDistance = totalWalking,
                                                    totalBusDistance = totalBusDistance,
                                                    numberOfTransfers = 2
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in findRouteWithDoubleTransfer: ${e.message}", e)
        }
        
        return bestRoute
    }
    
    /**
     * Find all viable transfer points between two buses
     * @return List of transfer points (firstBusStopIndex, secondBusStopIndex, transferDistance)
     */
    private fun findTransferPoints(firstBus: Bus, secondBus: Bus): List<Triple<Int, Int, Double>> {
        val transferPoints = mutableListOf<Triple<Int, Int, Double>>()
        
        firstBus.stops.forEachIndexed { firstIndex, firstStop ->
            val firstPoint = GeoPoint(firstStop.latitude, firstStop.longitude)
            
            secondBus.stops.forEachIndexed { secondIndex, secondStop ->
                val secondPoint = GeoPoint(secondStop.latitude, secondStop.longitude)
                val distance = RouteFinder.calculateDistance(firstPoint, secondPoint)
                
                if (distance <= MAX_TRANSFER_DISTANCE) {
                    transferPoints.add(Triple(firstIndex, secondIndex, distance))
                }
            }
        }
        
        return transferPoints
    }

    /**
     * Calculate the travel distance between two stops on a bus route
     */
    private fun calculateBusDistance(bus: Bus, startIndex: Int, endIndex: Int): Double {
        if (startIndex == endIndex) return 0.0
        if (startIndex > endIndex) return 0.0
        if (bus.stops.isEmpty()) return 0.0
        
        var distance = 0.0
        for (i in startIndex until endIndex) {
            val currentStop = bus.stops[i]
            val nextStop = bus.stops[i + 1]
            
            val currentPoint = GeoPoint(currentStop.latitude, currentStop.longitude)
            val nextPoint = GeoPoint(nextStop.latitude, nextStop.longitude)
            
            distance += RouteFinder.calculateDistance(currentPoint, nextPoint)
        }
        
        return distance
    }
} 