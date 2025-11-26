package com.example.familytracking2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familytracking2.ui.theme.FamilyTracking2Theme
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

class LoginActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "LoginActivity onCreate called")
        enableEdgeToEdge()

        setContent {
            FamilyTracking2Theme {
                Box(modifier = Modifier.fillMaxSize()) {

                    // ✅ Aman - panggil langsung di Composable
                    if (resourceExists("map_baru")) {
                        Image(
                            painter = painterResource(id = R.drawable.map_baru),
                            contentDescription = "Background",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.LightGray)
                        )
                    }

                    // Lapisan semi transparan di atas background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    // Konten utama login
                    AuthScreen(
                        onLoginSuccess = { username ->
                            Log.d(TAG, "Login success, navigating to Home")
                            navigateToHome(username)
                        },
                        onRegisterClick = {
                            Log.d(TAG, "Navigating to Register")
                            navigateToRegister()
                        }
                    )
                }
            }
        }
    }

    // ✅ Fungsi untuk cek apakah resource drawable ada
    private fun resourceExists(name: String): Boolean {
        val resId = resources.getIdentifier(name, "drawable", packageName)
        return resId != 0
    }

    // ✅ Navigasi ke Home tanpa crash
    private fun navigateToHome(username: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("EXTRA_USERNAME", username)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // ✅ Navigasi ke Register tanpa kembali ke menu awal
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }
}

// 🔐 Fungsi hash password
fun hashPasswordLogin(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

// 🧩 Tampilan utama login
@Composable
fun AuthScreen(
    onLoginSuccess: (String) -> Unit,
    onRegisterClick: () -> Unit
) {
    val context = LocalContext.current
    var inputUsername by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Welcome To Family Tracker",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            "Login Untuk menghubungkan Sistem",
            fontSize = 14.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Username
                TextField(
                    value = inputUsername,
                    onValueChange = { inputUsername = it },
                    label = { Text("Username") },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password
                TextField(
                    value = inputPassword,
                    onValueChange = { inputPassword = it },
                    label = { Text("Password") },
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol LOGIN
                Button(
                    onClick = {
                        if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
                            Toast.makeText(context, "Isi Username dan Password!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true

                        val database = FirebaseDatabase.getInstance()
                        val cleanUsername = inputUsername.replace(Regex("[^a-zA-Z0-9]"), "")

                        database.getReference("users").child(cleanUsername).get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    val storedPass = snapshot.child("password").getValue(String::class.java)
                                    val hashedInput = hashPasswordLogin(inputPassword)

                                    if (storedPass == hashedInput) {
                                        isLoading = false
                                        Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()

                                        // Delay sedikit agar Toast tampil sebelum pindah halaman
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            onLoginSuccess(cleanUsername)
                                        }, 500)
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Password Salah!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Username tidak ditemukan!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC51162)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("LOGIN", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Tombol ke halaman register
                TextButton(onClick = onRegisterClick) {
                    Text("Belum punya akun? Register", color = Color.Gray)
                }
            }
        }
    }
}
