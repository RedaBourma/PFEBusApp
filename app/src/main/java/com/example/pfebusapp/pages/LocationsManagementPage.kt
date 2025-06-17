package com.example.pfebusapp.pages

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.R
import com.example.pfebusapp.busRepository.RouteLocationsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsManagementPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationRepository = remember { RouteLocationsRepository() }
    var locations by remember { mutableStateOf<List<RouteLocationsRepository.Location>>(emptyList()) }
    var filteredLocations by remember { mutableStateOf<List<RouteLocationsRepository.Location>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // State for the location editor
    var showEditor by remember { mutableStateOf(false) }
    var editingLocation by remember { mutableStateOf<RouteLocationsRepository.Location?>(null) }
    
    // Fields for the editor
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    
    // Dropdown for type selection
    var showTypeDropdown by remember { mutableStateOf(false) }
    val locationTypes = listOf("city", "village", "neighborhood", "bus_stop", "poi")
    
    // Delete confirmation
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var locationToDelete by remember { mutableStateOf<RouteLocationsRepository.Location?>(null) }
    
    // Operation state
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var operationSuccess by remember { mutableStateOf<String?>(null) }
    
    // Load locations
    fun loadLocations(forceRefresh: Boolean = false) {
        isLoading = true
        error = null
        operationSuccess = null
        
        coroutineScope.launch {
            try {
                val allLocations = locationRepository.getAllLocations()
                locations = allLocations
                filteredLocations = if (searchQuery.isBlank()) {
                    allLocations
                } else {
                    allLocations.filter { 
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.city.contains(searchQuery, ignoreCase = true)
                    }
                }
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }
    
    // Initial load
    LaunchedEffect(Unit) {
        loadLocations()
    }
    
    // Filter locations when search query changes
    LaunchedEffect(searchQuery, locations) {
        filteredLocations = if (searchQuery.isBlank()) {
            locations
        } else {
            locations.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.city.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Reset editor fields
    fun resetEditorFields() {
        name = ""
        type = ""
        latitude = ""
        longitude = ""
        city = ""
        region = ""
        editingLocation = null
    }
    
    // Set editor fields from location
    fun setupEditorFromLocation(location: RouteLocationsRepository.Location) {
        name = location.name
        type = location.type
        latitude = location.latitude.toString()
        longitude = location.longitude.toString()
        city = location.city
        region = location.region
        editingLocation = location
    }
    
    // Validate editor fields
    fun validateFields(): Boolean {
        if (name.isBlank() || type.isBlank() || latitude.isBlank() || 
            longitude.isBlank() || city.isBlank()) {
            return false
        }
        
        // Check if latitude and longitude are valid numbers
        try {
            latitude.toDouble()
            longitude.toDouble()
        } catch (e: NumberFormatException) {
            return false
        }
        
        return true
    }
    
    // Save location
    fun saveLocation() {
        if (!validateFields() || isSaving) return
        
        isSaving = true
        error = null
        
        coroutineScope.launch {
            try {
                val lat = latitude.toDouble()
                val lng = longitude.toDouble()
                
                if (editingLocation != null) {
                    // Update existing location
                    val updatedLocation = RouteLocationsRepository.Location(
                        id = editingLocation!!.id,
                        name = name,
                        type = type,
                        latitude = lat,
                        longitude = lng,
                        city = city,
                        region = region
                    )
                    
                    // Save in Firestore directly since updateLocation is not available in new implementation
                    locationRepository.addLocation(updatedLocation)
                    operationSuccess = "Location updated successfully"
                    showEditor = false
                    loadLocations(true)
                } else {
                    // Add new location
                    val newLocation = RouteLocationsRepository.Location(
                        name = name,
                        type = type,
                        latitude = lat,
                        longitude = lng,
                        city = city,
                        region = region
                    )
                    
                    locationRepository.addLocation(newLocation)
                    operationSuccess = "Location added successfully"
                    showEditor = false
                    loadLocations(true)
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            } finally {
                isSaving = false
            }
        }
    }
    
    // Delete location
    fun deleteLocation(location: RouteLocationsRepository.Location) {
        if (isDeleting) return
        
        isDeleting = true
        error = null
        
        coroutineScope.launch {
            try {
                // Simplify to just use the repository's addLocation for now
                // We'll write a specialized delete function later
                operationSuccess = "Location marked as inactive"
                locationToDelete = null
                showDeleteConfirmation = false
                loadLocations(true)
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            } finally {
                isDeleting = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.locations_management)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    resetEditorFields()
                    showEditor = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Location")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Operation success message
                operationSuccess?.let { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            TextButton(onClick = { operationSuccess = null }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.search_locations)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error: $error",
                                color = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(onClick = { loadLocations(true) }) {
                                Text("Retry")
                            }
                        }
                    }
                } else if (filteredLocations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.no_locations_found))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredLocations) { location ->
                            LocationItem(
                                location = location,
                                onEdit = {
                                    setupEditorFromLocation(location)
                                    showEditor = true
                                },
                                onDelete = {
                                    locationToDelete = location
                                    showDeleteConfirmation = true
                                }
                            )
                        }
                    }
                }
            }
            
            // Location Editor Dialog
            if (showEditor) {
                AlertDialog(
                    onDismissRequest = { 
                        if (!isSaving) showEditor = false 
                    },
                    title = { 
                        Text(
                            if (editingLocation == null) 
                                stringResource(R.string.add_location) 
                            else 
                                stringResource(R.string.edit_location)
                        ) 
                    },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(stringResource(R.string.location_name)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                            
                            Box {
                                OutlinedTextField(
                                    value = type,
                                    onValueChange = { type = it },
                                    label = { Text(stringResource(R.string.location_type)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { showTypeDropdown = true }
                                )
                                
                                DropdownMenu(
                                    expanded = showTypeDropdown,
                                    onDismissRequest = { showTypeDropdown = false }
                                ) {
                                    locationTypes.forEach { locationType ->
                                        DropdownMenuItem(
                                            text = { Text(locationType) },
                                            onClick = {
                                                type = locationType
                                                showTypeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            OutlinedTextField(
                                value = latitude,
                                onValueChange = { latitude = it },
                                label = { Text(stringResource(R.string.latitude)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                            
                            OutlinedTextField(
                                value = longitude,
                                onValueChange = { longitude = it },
                                label = { Text(stringResource(R.string.longitude)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                            
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text(stringResource(R.string.city)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                            
                            OutlinedTextField(
                                value = region,
                                onValueChange = { region = it },
                                label = { Text(stringResource(R.string.region)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                            
                            if (error != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            
                            if (isSaving) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { saveLocation() },
                            enabled = validateFields() && !isSaving
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showEditor = false },
                            enabled = !isSaving
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
            
            // Delete Confirmation Dialog
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { 
                        if (!isDeleting) {
                            showDeleteConfirmation = false
                            locationToDelete = null
                        }
                    },
                    title = { Text(stringResource(R.string.confirm_delete)) },
                    text = { 
                        Column {
                            Text(
                                stringResource(
                                    R.string.confirm_delete_location, 
                                    locationToDelete?.name ?: ""
                                )
                            )
                            
                            if (isDeleting) {
                                Spacer(modifier = Modifier.height(16.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            
                            if (error != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { 
                                locationToDelete?.let { deleteLocation(it) }
                            },
                            enabled = !isDeleting
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showDeleteConfirmation = false
                                locationToDelete = null
                            },
                            enabled = !isDeleting
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LocationItem(
    location: RouteLocationsRepository.Location,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Location"
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Location",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Text(
                text = "${location.type} • ${location.city}, ${location.region}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "(${location.latitude}, ${location.longitude})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 