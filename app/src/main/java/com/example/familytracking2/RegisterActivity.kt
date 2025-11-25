package com.example.familytracking2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familytracking2.ui.theme.FamilyTracking2Theme
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

// HAPUS IMPORT INI: import kotlin.io.path.exists (Ini penyebab error overload)

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FamilyTracking2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    RegisterScreen()
                }
            }
        }
    }
}

// Helper Hashing
fun hashPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

// Model Data User untuk Firebase
data class UserData(
    val fullName: String? = null,
    val password: String? = null
)

@Composable
fun RegisterScreen() {
    val context = LocalContext.current

    // State input
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome To Family Tracker",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFC51162))
                .padding(vertical = 48.dp, horizontal = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CustomRegisterTextField(value = firstName, onValueChange = { firstName = it }, placeholder = "First Name")
                CustomRegisterTextField(value = lastName, onValueChange = { lastName = it }, placeholder = "Last Name")
                CustomRegisterTextField(value = username, onValueChange = { username = it }, placeholder = "Username (Tanpa Spasi)")
                CustomRegisterTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (username.isNotEmpty() && password.isNotEmpty()) {
                            isLoading = true
                            // 1. Inisialisasi Firebase
                            val database = FirebaseDatabase.getInstance()
                            val usersRef = database.getReference("users")

                            // Bersihkan username dari karakter aneh
                            val cleanUsername = username.replace(Regex("[^a-zA-Z0-9]"), "")

                            // 2. Cek apakah username sudah ada
                            usersRef.child(cleanUsername).get().addOnSuccessListener { snapshot ->
                                // Error sebelumnya terjadi di sini karena import yang salah
                                if (snapshot.exists()) {
                                    isLoading = false
                                    Toast.makeText(context, "Username sudah terdaftar!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // 3. Simpan data ke Firebase
                                    val newUser = UserData(
                                        fullName = "$firstName $lastName",
                                        password = hashPassword(password)
                                    )

                                    usersRef.child(cleanUsername).setValue(newUser)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            Toast.makeText(context, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(context, LoginActivity::class.java)
                                            context.startActivity(intent)
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            Toast.makeText(context, "Gagal menyimpan: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }.addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "Koneksi Error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Isi semua kolom!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF4285F4)
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(40.dp).width(150.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Blue)
                    } else {
                        Text(text = "REGISTER", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = placeholder, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            cursorColor = Color.Black,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    )
}
