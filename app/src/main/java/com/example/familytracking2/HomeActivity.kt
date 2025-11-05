package com.example.familytracking2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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

sealed class Screen(val route: String, val icon: Int) {
    object Home : Screen("home", R.drawable.home_tombol)
    object Saved : Screen("saved", R.drawable.saved)
    object Map : Screen("map", R.drawable.map_tombol)
    object History : Screen("history", R.drawable.activity)
}

data class Device(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val location: LatLng,
    val image: Int
)

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FamilyTracking2Theme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Map) } // Default ke Map untuk testing
    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen -> currentScreen = screen }
            )
        }
    ) { innerPadding ->
        when (currentScreen) {
            is Screen.Home -> HomeContent(modifier = Modifier.padding(innerPadding))
            is Screen.Saved -> SavedLocationScreen(modifier = Modifier.padding(innerPadding))
            is Screen.Map -> MapScreen(modifier = Modifier.padding(innerPadding))
            is Screen.History -> HistoryScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

private enum class MapPage {
    DeviceList,
    MapDisplay
}

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
        AddDeviceSheet(
            onDismiss = { showAddDeviceSheet = false },
            onSaveDevice = { newDevice ->
                devices.add(newDevice) // Tambahkan device baru ke daftar
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (currentPage) {
            MapPage.DeviceList -> {
                DeviceConnectScreen(
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
            }
            MapPage.MapDisplay -> {
                MapDisplayScreen(
                    devices = devicesToShowOnMap ?: emptyList(),
                    onBack = { currentPage = MapPage.DeviceList }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        ShowPairingQrDialog(
            myPairingCode = myPairingCode,
            onDismiss = { showMyQrDialog = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showMyQrDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 65.dp) // Sesuaikan padding agar tidak terlalu mepet
            ) {
                Icon(Icons.Rounded.QrCode2, contentDescription = "Show My QR Code", tint = Color.White)
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = Color.White // Set background color di sini
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Gunakan padding dari Scaffold
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            // Header (Tombol + dan Judul)
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

            // Daftar Device
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(devices, key = { it.id }) { device ->
                    DeviceItem(device = device, onClick = { onDeviceClick(device) })
                }
            }

            // Tombol View All Map
            Button(
                onClick = onViewAllClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp), // Beri padding vertikal
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
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { HistoryItem("10:30 AM", "Tiba di 'Kantor'") }
            item { HistoryItem("09:00 AM", "Berangkat dari 'Rumah'") }
            item { HistoryItem("Kemarin", "Mengunjungi 'Ngops Cantik w Bestie'") }
            item { HistoryItem("2 hari lalu", "Pergi ke 'Mall Grand Indonesia'") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLocationItem(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9C27B0))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItem(time: String, activity: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = time, color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = activity, fontSize = 16.sp)
        }
    }
}

@Composable
fun BottomNavBar(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    val screens = listOf(Screen.Home, Screen.Saved, Screen.Map, Screen.History)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE8E2E9))
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        screens.forEach { screen ->
            val isSelected = currentScreen.route == screen.route
            val backgroundColor = if (isSelected) Color(0xFF9C27B0) else Color.Transparent
            val iconColor = if (isSelected) Color.White else Color.Gray

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable { onScreenSelected(screen) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = screen.icon),
                    contentDescription = screen.route,
                    modifier = Modifier.size(if (isSelected) 32.dp else 28.dp),
                    colorFilter = ColorFilter.tint(iconColor)
                )
            }
        }
    }
}

@Composable
fun HomeContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F8FF))
            .verticalScroll(rememberScrollState())
    ) {
        Header()
        YourJourney()
        Gallery()
        ActivityGpsCard()
    }
}

@Composable
fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "Hello, Wahyu", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = "Welcome to Family Tracker", color = Color.Gray, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(0xFF4A90E2))
        )
    }
}

@Composable
fun YourJourney() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Your journey",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A90E2)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.map),
            contentDescription = "Journey Map",
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun Gallery() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Gallery", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9C27B0)),
                contentAlignment = Alignment.Center
            ) {
                Text("3", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { GalleryImage(R.drawable.logo) }
            item { GalleryImage(R.drawable.logo) }
            item { GalleryImage(R.drawable.logo) }
        }
    }
}

@Composable
fun GalleryImage(drawableId: Int) {
    Image(
        painter = painterResource(id = drawableId),
        contentDescription = null,
        modifier = Modifier
            .size(100.dp, 120.dp)
            .clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Crop
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityGpsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E2E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activity GPS",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9C27B0)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Anda baru saja mengunjungi tempat .... yang kami rekomendasikan, apakah itu anda? bagaimana keadaan disana, kuharap kamu selalu senang",
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}