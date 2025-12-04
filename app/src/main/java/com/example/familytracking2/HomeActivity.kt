package com.example.familytracking2

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.familytracking2.ui.theme.FamilyTracking2Theme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.UUID

// --- 1. DATA DEFINITIONS ---

sealed class Screen(val route: String, val iconRes: Int, val label: String) {
    object Home : Screen("home", R.drawable.home_tombol, "Home")
    object Saved : Screen("saved", R.drawable.saved, "Saved")
    object Map : Screen("map", R.drawable.map_tombol, "Map")
    object History : Screen("history", R.drawable.activity, "History")
    object Profile : Screen("profile", 0, "Profile")
}

// Data class untuk Gallery
data class GalleryItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val resourceId: Int? = null,
    val locationName: String
)

// Data class Device (Penting agar AddDeviceSheet tidak error)
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

        // Ambil username dari LoginActivity
        val username = intent.getStringExtra("EXTRA_USERNAME")?.replaceFirstChar { it.uppercase() } ?: "User"

        setContent {
            FamilyTracking2Theme {
                // Warna background aplikasi keseluruhan
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF0F8FF)
                ) {
                    MainScreen(username = username)
                }
            }
        }
    }
}

// --- 3. MAIN SCREEN ---

@Composable
fun MainScreen(username: String) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Menggunakan Scaffold agar Bottom Navigation Bar standar (seperti sebelumnya)
    Scaffold(
        bottomBar = {
            // Sembunyikan navbar jika di halaman profile (opsional, sesuai logika lama)
            if (currentScreen !is Screen.Profile) {
                BottomNavBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { screen -> currentScreen = screen }
                )
            }
        }
    ) { innerPadding ->
        // Content Area dengan padding dari Scaffold agar tidak tertutup navbar
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                is Screen.Home -> HomeDashboardContent(username = username)
                is Screen.Saved -> PlaceholderScreen("Saved Locations")
                is Screen.Map -> PlaceholderScreen("Live Tracking Map")
                is Screen.History -> PlaceholderScreen("Activity History")
                is Screen.Profile -> PlaceholderScreen("User Profile")
            }
        }
    }
}

// --- 4. HOME DASHBOARD CONTENT (Fitur Utama - Desain Baru) ---

@Composable
fun HomeDashboardContent(username: String) {
    val scrollState = rememberScrollState()

    // Simulasi data lokasi otomatis untuk "Activity GPS"
    val lastVisitedPlace = "Candi Prambanan"

    // State untuk Gallery
    val galleryItems = remember { mutableStateListOf<GalleryItem>() }

    // Launcher untuk memilih gambar dari galeri HP
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            galleryItems.add(0, GalleryItem(uri = uri, locationName = "Lokasi Baru"))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // A. Header Section (Abu-abu di atas)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .padding(top = 50.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Hello, $username",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Welcome to Family Tracker",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
                // Profile Picture Biru Bulat
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // B. Your Journey Section
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Your journey",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Koordinat contoh (Yogyakarta)
                    val jogja = LatLng(-7.7956, 110.3695)
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(jogja, 12f)
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled = false,
                            rotationGesturesEnabled = false,
                            tiltGesturesEnabled = false
                        )
                    ) {
                        Marker(state = MarkerState(position = LatLng(-7.79, 110.36)), title = "Point 1")
                        Marker(state = MarkerState(position = LatLng(-7.80, 110.37)), title = "Point 2")
                        Marker(state = MarkerState(position = LatLng(-7.81, 110.35)), title = "Point 3")

                        Polyline(
                            points = listOf(
                                LatLng(-7.79, 110.36),
                                LatLng(-7.80, 110.37),
                                LatLng(-7.81, 110.35)
                            ),
                            color = Color(0xFFC51162),
                            width = 5f
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x1A4285F4))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // C. Gallery Section
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Gallery",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Badge Count
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFC51162)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = galleryItems.size.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tombol Tambah Foto (+)
                item {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.LightGray.copy(alpha = 0.5f))
                            .clickable {
                                galleryLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Photo",
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // List Foto
                items(galleryItems) { item ->
                    GalleryImageItem(item)
                }

                // Placeholder Photos
                if (galleryItems.isEmpty()) {
                    item { PlaceholderImage(Color(0xFF81C784)) } // Hijau
                    item { PlaceholderImage(Color(0xFF64B5F6)) } // Biru
                    item { PlaceholderImage(Color(0xFFE57373)) } // Merah
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // D. Activity GPS Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFE0E0E0))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Activity GPS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC51162)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val text = buildAnnotatedString {
                    append("Anda baru saja mengunjungi tempat ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) {
                        append(lastVisitedPlace)
                    }
                    append(" yang kami rekomendasikan, apakah itu anda? bagaimana keadaan disana, kuharap kamu selalu senang")
                }

                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // Spacer bawah biasa
    }
}

// --- 5. COMPONENTS ---

// [KEMBALI KE LOGIKA LAMA] Bottom Navigation Bar Standar
@Composable
fun BottomNavBar(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Black,
        tonalElevation = 8.dp
    ) {
        val items = listOf(Screen.Home, Screen.Saved, Screen.Map, Screen.History)
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    // ✅ PERBAIKAN: Menghapus try-catch di sini
                    if (screen.iconRes != 0) {
                        Icon(
                            painter = painterResource(id = screen.iconRes),
                            contentDescription = screen.label,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.Home, contentDescription = null)
                    }
                },
                label = {
                    Text(
                        text = screen.label,
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

@Composable
fun GalleryImageItem(item: GalleryItem) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Image(
            painter = rememberAsyncImagePainter(item.uri),
            contentDescription = "Gallery Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun PlaceholderImage(color: Color) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
    )
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}
