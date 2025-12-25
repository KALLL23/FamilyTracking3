package com.example.familytracking2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val ANIMATION_DURATION = 1500L
    // Waktu kadaluarsa sesi (7 hari dalam milidetik)
    private val SESSION_EXPIRY_TIME = 7 * 24 * 60 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        supportActionBar?.hide()

        val mapView: ImageView = findViewById(R.id.map_background_view)
        val logoView: ImageView = findViewById(R.id.logo_view)
        val textView: TextView = findViewById(R.id.text_view)

        mapView.alpha = 0f
        logoView.alpha = 0f
        textView.alpha = 0f

        mapView.animate()
            .alpha(1f)
            .setDuration(2000)
            .setInterpolator(DecelerateInterpolator())
            .start()

        logoView.animate()
            .alpha(1f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(ANIMATION_DURATION)
            .setStartDelay(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        textView.animate()
            .alpha(1f)
            .translationY(-40f)
            .setDuration(ANIMATION_DURATION)
            .setStartDelay(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Ganti Handler standar dengan pengecekan login setelah delay animasi
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginSession()
        }, 3000)
    }

    private fun checkLoginSession() {
        val sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)
        val username = sharedPreferences.getString(LoginActivity.KEY_USERNAME, "")
        val loginTimestamp = sharedPreferences.getLong(LoginActivity.KEY_LOGIN_TIMESTAMP, 0L)

        val currentTime = System.currentTimeMillis()

        if (isLoggedIn && username != null && username.isNotEmpty()) {
            if (currentTime - loginTimestamp > SESSION_EXPIRY_TIME) {
                // Sesi kadaluarsa (lebih dari 7 hari), hapus data dan ke Login
                val editor = sharedPreferences.edit()
                editor.clear()
                editor.apply()
                navigateToLogin()
            } else {
                // Sesi valid, langsung ke Home
                navigateToHome(username)
            }
        } else {
            // Belum login, ke halaman Login
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToHome(username: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("EXTRA_USERNAME", username)
        startActivity(intent)
        finish()
    }
}
