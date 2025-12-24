package com.example.familytracking2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.familytracking2.ui.theme.FamilyTracking2Theme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- 1. DATA DEFINITIONS ---

// ✅ SOLUSI BAGIAN 1: Buat kelas data yang ramah Firebase untuk lokasi
data class FirebaseLatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    // Fungsi konversi ke LatLng asli Google Maps
    fun toGoogleLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }
}

sealed class Screen(val route: String, val iconRes: Int, val label: String) {
    object Home : Screen("home", R.drawable.home_tombol, "Home")
    object Saved : Screen("saved", R.drawable.saved, "Saved")
    object Map : Screen("map", R.drawable.map_tombol, "Map")
    object History : Screen("history", R.drawable.activity, "History")
    object Profile : Screen("profile", 0, "Profile")
}

data class GalleryItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val resourceId: Int? = null,
    val locationName: String
)

data class Device(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val address: String = "Mencari lokasi...", // Default text
    // ✅ SOLUSI BAGIAN 2: Gunakan kelas data kita sendiri, bukan LatLng
    val location: FirebaseLatLng = FirebaseLatLng(),
    val image: Int = 0,
    val profileImageUrl: String = "",
    val profileImageBase64: String = "",
    val uniqueCode: String = ""
)

data class SavedLocation(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

// ✅ Data class baru untuk riwayat
data class HistoryEvent(
    val id: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val type: String = "DEFAULT" // "ADD_DEVICE", "RECEIVE_DEVICE"
)

private enum class MapPage { DeviceList, MapDisplay }

// --- 2. MAIN ACTIVITY ---

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val username = intent.getStringExtra("EXTRA_USERNAME")?.replaceFirstChar { it.uppercase() } ?: "User"
        setContent {
            FamilyTracking2Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF0F8FF)) {
                    MainScreen(initialUsername = username)
                }
            }
        }
    }
}

// --- 3. MAIN SCREEN ---

@Composable
fun MainScreen(initialUsername: String) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var currentUsername by remember { mutableStateOf(initialUsername) }
    var profileImageBase64 by remember { mutableStateOf("") }
    var uniqueCode by remember { mutableStateOf("Loading...") }

    val cleanUsername = remember(initialUsername) {
        initialUsername.lowercase().replace(Regex("[^a-zA-Z0-9]"), "")
    }

    LaunchedEffect(cleanUsername) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(cleanUsername)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val code = snapshot.child("uniqueCode").value?.toString()
                    uniqueCode = if (code.isNullOrEmpty()) {
                        val newCode = UUID.randomUUID().toString().substring(0, 8).uppercase()
                        userRef.child("uniqueCode").setValue(newCode)
                        newCode
                    } else {
                        code
                    }
                    profileImageBase64 = snapshot.child("profileImageBase64").value?.toString() ?: ""
                } else {
                    val newCode = UUID.randomUUID().toString().substring(0, 8).uppercase()
                    userRef.child("uniqueCode").setValue(newCode)
                    uniqueCode = newCode
                    profileImageBase64 = ""
                }
            }
            override fun onCancelled(error: DatabaseError) { uniqueCode = "Error" }
        })
    }

    Scaffold(
        bottomBar = {
            if (currentScreen !is Screen.Profile) {
                BottomNavBar(currentScreen = currentScreen, onScreenSelected = { screen -> currentScreen = screen })
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                is Screen.Home -> HomeDashboardContent(
                    username = currentUsername,
                    profileImageBase64 = profileImageBase64,
                    uniqueCode = uniqueCode,
                    userKey = cleanUsername
                )
                is Screen.Saved -> SavedTabScreen(currentUserKey = cleanUsername)
                is Screen.Map -> MapTabScreen(currentUserKey = cleanUsername, currentUserName = currentUsername) // Pass current user name
                is Screen.History -> HistoryTabScreen(currentUserKey = cleanUsername) // ✅ Tampilan baru untuk riwayat
                is Screen.Profile -> PlaceholderScreen("User Profile")
            }
        }
    }
}

// ✅ --- 4. HISTORY TAB SCREEN ---
@Composable
fun HistoryTabScreen(currentUserKey: String) {
    val historyEvents = remember { mutableStateListOf<HistoryEvent>() }
    val database = FirebaseDatabase.getInstance()

    // Load history events from Firebase
    LaunchedEffect(currentUserKey) {
        val historyRef = database.getReference("users").child(currentUserKey).child("history").orderByChild("timestamp")
        historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyEvents.clear()
                for (child in snapshot.children) {
                    val event = child.getValue(HistoryEvent::class.java)
                    if (event != null) {
                        historyEvents.add(0, event.copy(id = child.key ?: "")) // Add to top for newest first
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Riwayat Aktivitas", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        if (historyEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.activity),
                        contentDescription = "empty history",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Belum ada aktivitas tercatat.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(historyEvents) { event ->
                    HistoryItem(event)
                }
            }
        }
    }
}

@Composable
fun HistoryItem(event: HistoryEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon: ImageVector = when (event.type) {
            "ADD_DEVICE" -> Icons.Outlined.PersonAdd
            "RECEIVE_DEVICE" -> Icons.Outlined.PersonPin
            else -> Icons.Default.Notifications
        }
        val iconColor = when (event.type) {
            "ADD_DEVICE" -> Color(0xFF1E88E5) // Blue
            "RECEIVE_DEVICE" -> Color(0xFF43A047) // Green
            else -> Color.Gray
        }

        Icon(
            imageVector = icon,
            contentDescription = event.type,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(event.message, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(event.timestamp)),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}


// --- 5. SAVED TAB SCREEN ---
@Composable
fun SavedTabScreen(currentUserKey: String) {
    val savedLocations = remember { mutableStateListOf<SavedLocation>() }
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance()

    LaunchedEffect(currentUserKey) {
        val savedRef = database.getReference("users").child(currentUserKey).child("saved_locations")
        savedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                savedLocations.clear()
                for (child in snapshot.children) {
                    val location = child.getValue(SavedLocation::class.java)
                    if (location != null) {
                        savedLocations.add(location.copy(id = child.key ?: ""))
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Saved Locations", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        if (savedLocations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Bookmark, contentDescription = "empty", tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Belum ada lokasi yang disimpan.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(savedLocations) { location ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = "loc", tint = Color(0xFFC51162))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(location.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(String.format("%.4f, %.4f", location.latitude, location.longitude), fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = {
                                database.getReference("users").child(currentUserKey).child("saved_locations").child(location.id).removeValue()
                                Toast.makeText(context, "Lokasi dihapus", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "delete", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- 6. MAP TAB SCREEN ---

@Composable
fun MapTabScreen(currentUserKey: String, currentUserName: String) {
    val devices = remember { mutableStateListOf<Device>() }
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance()

    var currentPage by remember { mutableStateOf(MapPage.DeviceList) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var isCheckingCode by remember { mutableStateOf(false) }
    var focusedDevice by remember { mutableStateOf<Device?>(null) }

    LaunchedEffect(currentUserKey) {
        val myDevicesRef = database.getReference("users").child(currentUserKey).child("connected_devices")
        myDevicesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                devices.clear()
                for (child in snapshot.children) {
                    val device = child.getValue(Device::class.java)
                    if (device != null) {
                        devices.add(device.copy(id = child.key ?: ""))
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    if (showAddDeviceDialog) {
        AddDeviceDialog(
            isLoading = isCheckingCode,
            onDismiss = { showAddDeviceDialog = false },
            onAddDevice = { name, inputCode ->
                isCheckingCode = true
                val usersRef = database.getReference("users")

                usersRef.orderByChild("uniqueCode").equalTo(inputCode)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            isCheckingCode = false
                            if (snapshot.exists()) {
                                val targetUserSnapshot = snapshot.children.first()
                                val targetUserKey = targetUserSnapshot.key ?: return
                                val targetProfileBase64 = targetUserSnapshot.child("profileImageBase64").value?.toString() ?: ""
                                val isAlreadyAdded = devices.any { it.uniqueCode == inputCode }

                                if (isAlreadyAdded) {
                                    Toast.makeText(context, "Perangkat ini sudah ada!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val myRef = database.getReference("users").child(currentUserKey).child("connected_devices").push()
                                    val newDevice = Device(
                                        id = myRef.key!!,
                                        name = name,
                                        address = "Mencari lokasi...",
                                        location = FirebaseLatLng(-7.75, 110.36),
                                        image = R.drawable.logo,
                                        profileImageBase64 = targetProfileBase64,
                                        profileImageUrl = "", // Field lama dikosongkan
                                        uniqueCode = inputCode
                                    )
                                    myRef.setValue(newDevice).addOnSuccessListener {
                                        // ✅ Buat entri riwayat
                                        val timestamp = System.currentTimeMillis()
                                        // Untuk pengguna saat ini
                                        val myHistoryRef = database.getReference("users").child(currentUserKey).child("history").push()
                                        myHistoryRef.setValue(HistoryEvent(id = myHistoryRef.key!!, message = "Menambahkan device $name", timestamp = timestamp, type = "ADD_DEVICE"))
                                        // Untuk pengguna target
                                        val targetHistoryRef = database.getReference("users").child(targetUserKey).child("history").push()
                                        targetHistoryRef.setValue(HistoryEvent(id = targetHistoryRef.key!!, message = "$currentUserName menambahkan Anda sebagai device", timestamp = timestamp, type = "RECEIVE_DEVICE"))

                                        Toast.makeText(context, "Berhasil terhubung!", Toast.LENGTH_SHORT).show()
                                        showAddDeviceDialog = false
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Kode tidak ditemukan!", Toast.LENGTH_LONG).show()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) { isCheckingCode = false }
                    })
            }
        )
    }

    when (currentPage) {
        MapPage.DeviceList -> {
            DeviceListContent(
                devices = devices,
                onAddDeviceClick = { showAddDeviceDialog = true },
                onViewAllMapClick = {
                    focusedDevice = null
                    currentPage = MapPage.MapDisplay
                },
                onDeviceClick = { device ->
                    focusedDevice = device
                    currentPage = MapPage.MapDisplay
                }
            )
        }
        MapPage.MapDisplay -> {
            FullMapDisplay(
                devices = devices,
                focusedDevice = focusedDevice,
                onBack = { currentPage = MapPage.DeviceList },
                onSaveLocation = { name, latLng ->
                    val savedRef = database.getReference("users").child(currentUserKey).child("saved_locations").push()
                    val locationData = SavedLocation(id = savedRef.key!!, name = name, latitude = latLng.latitude, longitude = latLng.longitude)
                    savedRef.setValue(locationData).addOnSuccessListener {
                        Toast.makeText(context, "Lokasi '$name' tersimpan!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// --- 7. DEVICE LIST UI & MAP DISPLAY ---

@Composable
fun DeviceListContent(
    devices: List<Device>,
    onAddDeviceClick: () -> Unit,
    onViewAllMapClick: () -> Unit,
    onDeviceClick: (Device) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Device Connect", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddDeviceClick, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black)) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
            }
            Spacer(Modifier.height(24.dp))
            if (devices.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada perangkat yang terhubung", color = Color.Gray)
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(devices) { device -> DeviceListItem(device) { onDeviceClick(device) } }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onViewAllMapClick() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                Text("View All Map", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun DeviceListItem(device: Device, onClick: () -> Unit) {
    val context = LocalContext.current
    var address by remember { mutableStateOf(device.address) }

    // ✅ Fitur Baru: LaunchedEffect untuk mengambil alamat saat lokasi berubah
    LaunchedEffect(device.location) {
        val lat = device.location.latitude
        val lng = device.location.longitude
        // Hindari pencarian jika lokasi default/tidak valid
        if (lat != 0.0 || lng != 0.0) {
            address = getAddressFromLocation(context, lat, lng) ?: "Lokasi tidak terdeteksi"
        } else {
            address = "${device.name} tidak diketahui lokasinya"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFE8E8E8)).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)) {
            // SOLUSI: Logika render yang aman dari crash
            val imageBitmap = remember(device.profileImageBase64) {
                decodeBase64ToBitmap(device.profileImageBase64)
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Device Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (device.profileImageUrl.isNotEmpty()) {
                // Fallback ke URL lama jika Base64 tidak ada
                Image(
                    painter = rememberAsyncImagePainter(model = device.profileImageUrl),
                    contentDescription = "Device Profile (Legacy)",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Ikon default jika tidak ada gambar sama sekali
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.align(Alignment.Center), tint = Color.Gray)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(device.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            // ✅ Fitur Baru: Tampilkan state alamat
            Text(address, fontSize = 12.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun FullMapDisplay(
    devices: List<Device>,
    focusedDevice: Device?,
    onBack: () -> Unit,
    onSaveLocation: (String, LatLng) -> Unit
) {
    BackHandler { onBack() }
    val cameraPositionState = rememberCameraPositionState()
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(devices, focusedDevice) {
        if (devices.isNotEmpty()) {
            try {
                val update = if (focusedDevice != null) {
                    // ✅ SOLUSI BAGIAN 4: Konversi ke LatLng asli saat digunakan
                    CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(focusedDevice.location.toGoogleLatLng(), 17f))
                } else if (devices.size == 1) {
                    CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(devices.first().location.toGoogleLatLng(), 15f))
                } else {
                    val bounds = LatLngBounds.builder().apply { devices.forEach { include(it.location.toGoogleLatLng()) } }.build()
                    CameraUpdateFactory.newLatLngBounds(bounds, 150)
                }
                cameraPositionState.animate(update)
            } catch (e: Exception) {}
        }
    }

    if (showSaveDialog) {
        var locationName by remember { mutableStateOf(focusedDevice?.name ?: "Lokasi Baru") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Simpan Lokasi Ini?") },
            text = {
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Nama Lokasi") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (locationName.isNotEmpty()) {
                        onSaveLocation(locationName, cameraPositionState.position.target)
                        showSaveDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Batal") } }
        )
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            // ✅ SOLUSI BAGIAN 5: Konversi ke LatLng asli saat render Marker
            devices.forEach { device ->
                Marker(
                    state = MarkerState(position = device.location.toGoogleLatLng()),
                    title = device.name,
                    snippet = device.address
                )
            }
        }
        IconButton(onClick = { onBack() }, modifier = Modifier.padding(16.dp).clip(CircleShape).background(Color.White).size(48.dp).align(Alignment.TopStart)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
        }
        IconButton(onClick = { showSaveDialog = true }, modifier = Modifier.padding(16.dp).clip(CircleShape).background(Color.White).size(48.dp).align(Alignment.TopEnd)) {
            Icon(Icons.Default.BookmarkAdd, contentDescription = "Save", tint = Color(0xFFC51162))
        }
    }
}

// --- 8. HOME DASHBOARD & PROFILE LOGIC ---

@Composable
fun HomeDashboardContent(
    username: String,
    profileImageBase64: String,
    uniqueCode: String,
    userKey: String
) {
    val scrollState = rememberScrollState()
    val lastVisitedPlace = "Candi Prambanan"
    val galleryItems = remember { mutableStateListOf<GalleryItem>() }
    var showProfileDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            galleryItems.add(0, GalleryItem(uri = uri, locationName = "Lokasi Baru"))
        }
    }

    if (showProfileDialog) {
        ProfileSettingsDialog(
            currentName = username,
            currentProfileBase64 = profileImageBase64,
            uniqueCode = uniqueCode,
            onDismiss = { showProfileDialog = false },
            onSave = { _, newUri ->
                newUri?.let { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                        val byteArray = baos.toByteArray()
                        val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

                        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userKey)
                        // Simpan data baru
                        userRef.child("profileImageBase64").setValue(encodedImage)
                        // Hapus data lama agar konsisten
                        userRef.child("profileImageUrl").removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Foto profil diperbarui!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Gagal menyimpan foto: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal memproses gambar.", Toast.LENGTH_SHORT).show()
                    }
                }
                showProfileDialog = false
            }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Box(Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(top = 50.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Hello, $username", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Welcome to Family Tracker", fontSize = 14.sp, color = Color.DarkGray)
                }
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).clickable { showProfileDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    val imageBitmap = remember(profileImageBase64) { decodeBase64ToBitmap(profileImageBase64) }
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        DefaultProfileIcon()
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("Your journey", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4), modifier = Modifier.padding(bottom = 12.dp))
            Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth().height(220.dp)) {
                val jogja = LatLng(-7.7956, 110.3695)
                val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(jogja, 12f) }
                GoogleMap(
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false, zoomGesturesEnabled = false)
                ) {
                    Polyline(points = listOf(LatLng(-7.79, 110.36), LatLng(-7.80, 110.37)), color = Color(0xFFC51162), width = 5f)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gallery", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFC51162)), contentAlignment = Alignment.Center) {
                    Text(galleryItems.size.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Box(
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(Color.LightGray.copy(alpha = 0.5f)).clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, "Add Photo", tint = Color.Gray, modifier = Modifier.size(32.dp))
                    }
                }
                items(galleryItems) { item -> GalleryImageItem(item) }
                if (galleryItems.isEmpty()) {
                    item { PlaceholderImage(Color(0xFF81C784)) }
                    item { PlaceholderImage(Color(0xFF64B5F6)) }
                    item { PlaceholderImage(Color(0xFFE57373)) }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFE0E0E0)).padding(20.dp)
        ) {
            Column {
                Text("Activity GPS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC51162))
                Spacer(modifier = Modifier.height(8.dp))
                val text = buildAnnotatedString {
                    append("Anda baru saja mengunjungi tempat ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) { append(lastVisitedPlace) }
                    append(" yang kami rekomendasikan, apakah itu anda? bagaimana keadaan disana, kuharap kamu selalu senang")
                }
                Text(text, fontSize = 14.sp, color = Color.Black.copy(alpha = 0.8f), lineHeight = 20.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
fun ProfileSettingsDialog(
    currentName: String,
    currentProfileBase64: String,
    uniqueCode: String,
    onDismiss: () -> Unit,
    onSave: (String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        tempUri = uri
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(16.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.size(100.dp).clip(CircleShape).clickable { imageLauncher.launch("image/*") }, Alignment.Center) {
                    val imageModifier = Modifier.fillMaxSize()

                    when {
                        tempUri != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(tempUri),
                                contentDescription = "Profile Preview",
                                modifier = imageModifier,
                                contentScale = ContentScale.Crop
                            )
                        }
                        currentProfileBase64.isNotEmpty() -> {
                            val imageBitmap = remember(currentProfileBase64) { decodeBase64ToBitmap(currentProfileBase64) }
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "Current Profile",
                                    modifier = imageModifier,
                                    contentScale = ContentScale.Crop
                                )
                            } else { DefaultProfileIcon() }
                        }
                        else -> { DefaultProfileIcon() }
                    }

                    Box(modifier = imageModifier.background(Color.Black.copy(0.3f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Change", tint = Color.White.copy(0.8f))
                    }
                }
                Text("Tap to change photo", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Display Name") }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = uniqueCode, onValueChange = {}, label = { Text("Your Unique ID") }, readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(uniqueCode))
                            Toast.makeText(context, "Code Copied!", Toast.LENGTH_SHORT).show()
                        }) { Icon(painterResource(android.R.drawable.ic_menu_save), contentDescription = "Copy") }
                    },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color(0xFFF5F5F5))
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { onSave(name, tempUri) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(12.dp)) {
                    Text("Save Changes", color = Color.White)
                }
            }
        }
    }
}

private fun decodeBase64ToBitmap(base64String: String): ImageBitmap? {
    return try {
        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

// ✅ Fitur Baru: Fungsi bantuan untuk mendapatkan alamat dari koordinat
private suspend fun getAddressFromLocation(context: Context, latitude: Double, longitude: Double): String? {
    // Jalankan di background thread untuk tidak memblokir UI
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            // getFromLocation() bischcca butuh waktu, makanya di IO dispatcher
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                // Gabungkan beberapa baris alamat untuk hasil yang lebih lengkap dan informatif
                // Contoh: "Jl. Merdeka No.10, Kota Bandung, Jawa Barat"
                address.getAddressLine(0)
            } else {
                "Alamat tidak ditemukan"
            }
        } catch (e: IOException) {
            // Error jaringan atau I/O
            "Gagal mendapatkan alamat"
        }
    }
}


@Composable
private fun BoxScope.DefaultProfileIcon() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF4285F4)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = "Profile",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}


// --- 9. SHARED COMPONENTS ---
@Composable
fun AddDeviceDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAddDevice: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Tracking Device") },
        text = {
            Column {
                Text("Masukkan Nama Panggilan dan Kode Unik Device.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Panggilan (Misal: Ayah)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = deviceId, onValueChange = { deviceId = it.uppercase() }, label = { Text("Kode Unik (Contoh: A05B82B1)") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Memeriksa kode...", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotEmpty() && deviceId.isNotEmpty()) { onAddDevice(name, deviceId) } },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                enabled = !isLoading
            ) { Text("Connect") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel", color = Color.Gray) } },
        containerColor = Color.White
    )
}

@Composable
fun BottomNavBar(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White, contentColor = Color.Black, tonalElevation = 8.dp) {
        val items = listOf(Screen.Home, Screen.Saved, Screen.Map, Screen.History)
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    if (screen.iconRes != 0) {
                        Icon(painter = painterResource(id = screen.iconRes), contentDescription = screen.label, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Home, contentDescription = null)
                    }
                },
                label = { Text(screen.label, fontSize = 10.sp) },
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Blue, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
            )
        }
    }
}

@Composable
fun GalleryImageItem(item: GalleryItem) {
    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp))) {
        Image(painter = rememberAsyncImagePainter(item.uri), contentDescription = "Gallery Image", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun PlaceholderImage(color: Color) {
    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(color))
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}
