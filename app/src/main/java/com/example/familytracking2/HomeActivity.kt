package com.example.familytracking2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familytracking2.ui.theme.FamilyTracking2Theme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.*

// --- 1. DATA & SCREEN DEFINITIONS ---

sealed class Screen(val route: String, val icon: Int) {
    object Home : Screen("home", R.drawable.home_tombol)
    object Saved : Screen("saved", R.drawable.saved)
    object Map : Screen("map", R.drawable.map_tombol)
    object History : Screen("history", R.drawable.activity)
    object Profile : Screen("profile", 0)
}

data class Device(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val location: LatLng,
    val image: Int
)

// --- 2. MAIN ACTIVITY ---

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ambil username yang dikirim dari LoginActivity
        val username = intent.getStringExtra("EXTRA_USERNAME") ?: "User"

        setContent {
            FamilyTracking2Theme {
                MainScreen(username = username)
            }
        }
    }
}

// --- 3. MAIN SCREEN COMPOSABLE ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(username: String) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Scaffold(
        bottomBar = {
            if (currentScreen !is Screen.Profile) {
                BottomNavBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { screen -> currentScreen = screen }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                is Screen.Home -> HomeContent(
                    onProfileClick = { currentScreen = Screen.Profile }
                )
                is Screen.Saved -> SavedLocationScreen()
                is Screen.Map -> MapScreen()
                is Screen.History -> HistoryScreen()
                is Screen.Profile -> ProfileContent(username = username) // Pass username ke Profile
            }
        }
    }
}

// --- 4. HOME CONTENT ---

@Composable
fun HomeContent(modifier: Modifier = Modifier, onProfileClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Home",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Welcome to Family Tracking Home Page!")
    }
}

// --- 5. PROFILE CONTENT ---

@Composable
fun ProfileContent(modifier: Modifier = Modifier, username: String) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = username, // Gunakan username dinamis disini
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(end = 16.dp)
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4285F4))
            )
        }

        val menuItems = listOf("Profile", "Notification", "Research", "Account")
        menuItems.forEach { item ->
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD9D9D9),
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = item,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- 6. MAP SCREEN & LOGIC ---

private enum class MapPage { DeviceList, MapDisplay }

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val devices = remember {
        mutableStateListOf(
            Device(name = "Kakak", address = "Jl. magelang km.14", location = LatLng(-7.6835, 110.3392), image = R.drawable.logo),
            Device(name = "Adek", address = "SDN 1 Sinduadi, Jl. M...", location = LatLng(-7.7595, 110.359), image = R.drawable.logo)
        )
    }

    var currentPage by remember { mutableStateOf(MapPage.DeviceList) }
    var devicesToShowOnMap by remember { mutableStateOf<List<Device>?>(null) }
    var showAddDeviceSheet by remember { mutableStateOf(false) }

    if (showAddDeviceSheet) {
        DeviceAddDialog(
            onDismissRequest = { showAddDeviceSheet = false },
            onDeviceAdded = { newDevice ->
                devices.add(newDevice)
                showAddDeviceSheet = false
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (currentPage) {
            MapPage.DeviceList -> DeviceConnectScreen(
                devices = devices,
                onAddDeviceClick = { showAddDeviceSheet = true },
                onViewAllClick = {
                    devicesToShowOnMap = devices
                    currentPage = MapPage.MapDisplay
                },
                onDeviceClick = { device ->
                    devicesToShowOnMap = listOf(device)
                    currentPage = MapPage.MapDisplay
                }
            )
            MapPage.MapDisplay -> MapDisplayScreen(
                devices = devicesToShowOnMap ?: emptyList(),
                onBack = { currentPage = MapPage.DeviceList }
            )
        }
    }
}

@Composable
fun DeviceConnectScreen(
    devices: List<Device>,
    onAddDeviceClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onDeviceClick: (Device) -> Unit
) {
    var showMyQrDialog by remember { mutableStateOf(false) }
    val myPairingCode = remember { "DEVICE-${UUID.randomUUID().toString().take(8).uppercase()}" }

    if (showMyQrDialog) {
        QrCodeDisplayDialog(
            code = myPairingCode,
            onDismissRequest = { showMyQrDialog = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showMyQrDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 65.dp)
            ) {
                Icon(Icons.Rounded.QrCode2, contentDescription = "Show My QR Code", tint = Color.White)
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Device Connect", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                IconButton(
                    onClick = onAddDeviceClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Device", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(devices, key = { it.id }) { device ->
                    DeviceItem(device = device, onClick = { onDeviceClick(device) })
                }
            }

            Button(
                onClick = onViewAllClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("View All Map", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun DeviceItem(device: Device, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8E8E8))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = device.image),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(device.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Text(device.address, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
fun MapDisplayScreen(devices: List<Device>, onBack: () -> Unit) {
    val cameraPositionState = rememberCameraPositionState()
    LaunchedEffect(devices) {
        if (devices.isNotEmpty()) {
            if (devices.size == 1) {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newCameraPosition(
                        CameraPosition(devices.first().location, 15f, 0f, 0f)
                    ),
                    durationMs = 1000
                )
            } else {
                val boundsBuilder = LatLngBounds.builder()
                devices.forEach { boundsBuilder.include(it.location) }
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100),
                    durationMs = 1000
                )
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            devices.forEach { device ->
                Marker(
                    state = MarkerState(position = device.location),
                    title = device.name,
                    snippet = device.address
                )
            }
        }
        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.8f))
        ) {
            Text("Kembali ke Daftar", color = Color.Black)
        }
    }
}

// --- 7. OTHER SCREENS ---

@Composable
fun SavedLocationScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Saved Location",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { SavedLocationItem("Ngops Cantik w Bestie") }
            item { SavedLocationItem("Spot nugas nyantai") }
            item { SavedLocationItem("Resto enak") }
        }
    }
}

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "History Activity",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text("No history available yet.")
    }
}

// --- 8. COMPONENTS & DIALOGS ---

@Composable
fun SavedLocationItem(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE0E0E0))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.saved),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DeviceAddDialog(onDismissRequest: () -> Unit, onDeviceAdded: (Device) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add Device") },
        text = { Text("Fitur tambah device dummy.") },
        confirmButton = {
            TextButton(onClick = {
                onDeviceAdded(
                    Device(
                        name = "New Device",
                        address = "Unknown Location",
                        location = LatLng(-7.7, 110.3),
                        image = R.drawable.logo
                    )
                )
            }) { Text("Add Dummy") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}

@Composable
fun QrCodeDisplayDialog(code: String, onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("My QR Code") },
        text = { Text("Code: $code") },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text("Close") }
        }
    )
}

@Composable
fun BottomNavBar(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Black
    ) {
        val items = listOf(Screen.Home, Screen.Saved, Screen.Map, Screen.History)
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.route,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.route.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        fontSize = 10.sp
                    )
                },
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Blue,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
