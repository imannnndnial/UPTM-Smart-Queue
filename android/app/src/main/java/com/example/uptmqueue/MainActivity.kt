package com.example.uptmqueue

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.uptmqueue.databinding.ActivityMainContainerBinding
import com.example.uptmqueue.fragments.HistoryFragment
import com.example.uptmqueue.fragments.HomeFragment
import com.example.uptmqueue.fragments.ProfileFragment
import com.example.uptmqueue.fragments.TicketsFragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import android.content.pm.PackageManager
import android.os.Build

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainContainerBinding
    private lateinit var database: DatabaseReference

    // Student Data (shared across fragments)
    var studentId: String = ""
        private set
    var studentName: String = ""
        private set
    var studentEmail: String = ""
        private set
    var matricNo: String = ""
        private set
    var studentPhone: String = ""
        private set

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadStudentDataFromSharedPreferences()

        if (!isLoggedIn()) {
            redirectToLogin()
            return
        }


        binding = ActivityMainContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        // Init database dulu
        database = FirebaseDatabase.getInstance().reference

        // Lepas tu baru FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                database.child("students").child(studentId).child("fcmToken").setValue(token)
            }
        }

        setupBottomNavigation()

        if (savedInstanceState == null) {
            checkActiveTicketOnReboot()
        }
    }

    // ✅ NEW: LOAD FROM SHAREDPREFERENCES
    private fun loadStudentDataFromSharedPreferences() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        studentId = sharedPref.getString("studentId", "") ?: ""
        studentName = sharedPref.getString("studentName", "Student") ?: "Student"
        studentEmail = sharedPref.getString("studentEmail", "") ?: ""
        matricNo = sharedPref.getString("matricNo", "") ?: ""
        studentPhone = sharedPref.getString("studentPhone", "") ?: ""

        // ✅ DEBUG LOGGING
        Log.d(TAG, "========================================")
        Log.d(TAG, "Loaded student data from SharedPreferences:")
        Log.d(TAG, "ID: $studentId")
        Log.d(TAG, "Name: $studentName")
        Log.d(TAG, "Email: $studentEmail")
        Log.d(TAG, "Matric: $matricNo")
        Log.d(TAG, "Phone: $studentPhone")
        Log.d(TAG, "========================================")
    }

    // ✅ NEW: CHECK IF LOGGED IN
    private fun isLoggedIn(): Boolean {
        return studentId.isNotEmpty()
    }

    // ✅ NEW: REDIRECT TO LOGIN IF NOT LOGGED IN
    private fun redirectToLogin() {
        Log.w(TAG, "⚠️ No student data found - redirecting to login")

        val intent = Intent(this, StudentLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_history -> {
                    // Same as nav_ticket - show TicketsFragment
                    loadFragment(HistoryFragment())
                    true
                }
                R.id.nav_ticket -> {
                    // Show TicketsFragment
                    loadFragment(TicketsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun checkActiveTicketOnReboot() {
        Log.d(TAG, "🔍 Checking for active ticket on reboot...")

        database.child("queue").child("active")
            .orderByChild("studentId")
            .equalTo(studentId)
            .get()
            .addOnSuccessListener { snapshot ->
                var hasActiveTicket = false

                for (child in snapshot.children) {
                    val status = child.child("status").getValue(String::class.java)
                    if (status == "waiting" || status == "serving") {
                        hasActiveTicket = true
                        Log.d(TAG, "✅ Active ticket found: ${child.key}, status: $status")
                        break
                    }
                }

                if (hasActiveTicket) {
                    Log.d(TAG, "✅ Resuming with active ticket — loading TicketsFragment")
                    // Ada active ticket — tunjuk TicketsFragment terus
                    loadFragment(TicketsFragment())
                    binding.bottomNavigation.selectedItemId = R.id.nav_ticket
                } else {
                    Log.d(TAG, "ℹ️ No active ticket — loading HomeFragment normally")
                    loadFragment(HomeFragment())
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "❌ Error checking active ticket: ${error.message}")
                // Kalau error, fallback ke HomeFragment
                loadFragment(HomeFragment())
            }
    }

    // Helper function untuk navigate dari fragment
    fun navigateToFragment(fragment: Fragment, menuItemId: Int) {
        loadFragment(fragment)
        binding.bottomNavigation.selectedItemId = menuItemId
    }
}