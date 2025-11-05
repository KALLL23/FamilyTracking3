package com.example.familytracking2

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.maps.model.LatLng // <-- IMPORT YANG HILANG SUDAH DITAMBAHKAN
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

// Enum untuk mengontrol form mana yang ditampilkan di dalam sheet
enum class AddDevicePage {
    Selection, // Pilihan awal
    HandphoneForm,
    GpsForm
}

// Data class untuk menampung data dari form
data class HandphoneFormData(
    var name: String = "",
    var phoneNumber: String = "",
    var pairingCode: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceSheet(
    onDismiss: () -> Unit,
    onSaveDevice: (Device) -> Unit // Callback untuk menyimpan device baru
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentPage by remember { mutableStateOf(AddDevicePage.Selection) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                when (currentPage) {
                    AddDevicePage.Selection -> {
                        SelectionScreen(
                            onHandphoneClick = { currentPage = AddDevicePage.HandphoneForm },
                            onGpsClick = { currentPage = AddDevicePage.GpsForm }
                        )
                    }
                    AddDevicePage.HandphoneForm -> {
                        HandphoneForm(onSave = { formData ->
                            // Buat objek Device baru dari data form
                            val newDevice = Device(
                                name = formData.name,
                                address = "Pairing code: ${formData.pairingCode}", // Contoh address
                                location = LatLng(-7.7, 110.3), // Lokasi default sementara
                                image = R.drawable.logo // Gambar default
                            )
                            onSaveDevice(newDevice)
                            onDismiss() // Tutup sheet
                        })
                    }
                    AddDevicePage.GpsForm -> {
                        // Implementasi untuk GPS Form bisa ditambahkan di sini
                        GpsForm(onSave = { onDismiss() })
                    }
                }
            }
        }
    }
}

// Halaman Pilihan Awal
@Composable
fun SelectionScreen(onHandphoneClick: () -> Unit, onGpsClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add New Device", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onHandphoneClick, modifier = Modifier.fillMaxWidth()) {
            Text("Handphone")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onGpsClick, modifier = Modifier.fillMaxWidth()) {
            Text("GPS Tracking")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Form untuk "Add Handphone"
@Composable
fun HandphoneForm(onSave: (HandphoneFormData) -> Unit) {
    // State untuk setiap input field
    val formData = remember { mutableStateOf(HandphoneFormData()) }
    val isFormValid by remember {
        derivedStateOf {
            formData.value.name.isNotBlank() && formData.value.pairingCode.isNotBlank()
        }
    }

    // Launcher untuk QR Code Scanner
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            result.contents?.let {
                // Update state pairingCode dengan hasil scan
                formData.value = formData.value.copy(pairingCode = it)
            }
        }
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Add Handphone", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = formData.value.name,
            onValueChange = { formData.value = formData.value.copy(name = it) },
            label = { Text("Nama Device / Pemilik") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = formData.value.phoneNumber,
            onValueChange = { formData.value = formData.value.copy(phoneNumber = it) },
            label = { Text("Nomor Telepon HP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = formData.value.pairingCode,
                onValueChange = { formData.value = formData.value.copy(pairingCode = it) },
                label = { Text("Kode Pairing") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                // Memulai scanner
                val options = ScanOptions()
                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                options.setPrompt("Scan a QR code")
                options.setCameraId(0)  // 0 untuk kamera belakang
                options.setBeepEnabled(true)
                options.setBarcodeImageEnabled(true)
                scanLauncher.launch(options)
            }) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan QR Code")
            }
        }
        // ... Sisa form (Deskripsi, Interval, Notifikasi) bisa ditambahkan di sini dengan state-nya masing-masing
        Button(
            onClick = { onSave(formData.value) },
            enabled = isFormValid, // Tombol aktif jika form valid
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Pair Now")
        }
    }
}

// Dialog untuk menampilkan QR Code
@Composable
fun ShowPairingQrDialog(myPairingCode: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Scan to Pair",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Generate QR Code dari pairing code
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(
                    myPairingCode,
                    BarcodeFormat.QR_CODE,
                    400,
                    400
                )

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Pairing QR Code",
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Pairing Code:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    myPairingCode,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Sisa Composable (GpsForm, Checkbox, IntervalSelector)
@Composable
fun GpsForm(onSave: () -> Unit) {
    /* Implementasi GpsForm di sini */
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Form GPS Tracking (Coming Soon)", modifier=Modifier.padding(32.dp))
        Button(onClick = onSave) { Text("Close") }
    }
}

@Composable
fun CheckboxWithLabel(label: String) {
    var isChecked by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isChecked = !isChecked }) {
        Checkbox(checked = isChecked, onCheckedChange = { isChecked = it })
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalSelector(label: String, options: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(options.firstOrNull() ?: "") }
    Column {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedOption = option
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
