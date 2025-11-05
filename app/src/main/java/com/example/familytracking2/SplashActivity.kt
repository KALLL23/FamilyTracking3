package com.example.familytracking2 // Ganti dengan nama package Anda

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Sembunyikan ActionBar jika ada
        supportActionBar?.hide()

        // Referensi ke semua view dari layout
        val mapView: ImageView = findViewById(R.id.map_background_view)
        val logoView: ImageView = findViewById(R.id.logo_view)
        val textView: TextView = findViewById(R.id.text_view)

        // 1. Atur kondisi awal sebelum animasi (semua transparan)
        mapView.alpha = 0f
        logoView.alpha = 0f
        textView.alpha = 0f

        // 2. Mulai animasi secara bersamaan
        // Animasi untuk Peta (Fade In)
        mapView.animate()
            .alpha(1f) // Muncul secara perlahan
            .setDuration(2000) // Durasi lebih lama agar lembut
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Animasi untuk Logo (Muncul dan Membesar)
        logoView.animate()
            .alpha(1f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(ANIMATION_DURATION)
            .setStartDelay(500) // Mulai sedikit lebih lambat dari peta
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Animasi untuk Teks (Muncul dan Geser ke Atas)
        textView.animate()
            .alpha(1f)
            .translationY(-40f) // Bergerak ke atas sedikit
            .setDuration(ANIMATION_DURATION)
            .setStartDelay(800) // Mulai setelah logo mulai muncul
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // 3. Pindah ke MainActivity setelah animasi selesai
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Tutup SplashActivity agar tidak bisa kembali
        }, 3000) // Total durasi splash screen (misalnya 3 detik)
    }
}
