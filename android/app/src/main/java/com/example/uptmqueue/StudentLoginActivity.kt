package com.example.uptmqueue

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.uptmqueue.databinding.ActivityStudentLoginBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StudentLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentLoginBinding
    private lateinit var database: DatabaseReference

    companion object {
        private const val TAG = "StudentLoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ CHECK IF ALREADY LOGGED IN BEFORE SHOWING LOGIN SCREEN
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val savedId = sharedPref.getString("studentId", "")

        if (!savedId.isNullOrEmpty()) {
            // ✅ Already logged in → Go directly to MainActivity
            Log.d(TAG, "✅ Already logged in as: $savedId")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // ❌ Not logged in → Show login screen
        Log.d(TAG, "❌ Not logged in, showing login screen")
        binding = ActivityStudentLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference
        Log.d(TAG, "Firebase Database initialized")

        setupToolbar()
        setupListeners()
        setupTextWatchers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        // Sign In Button
        binding.btnSignIn.setOnClickListener {
            if (validateInput()) {
                performLogin()
            }
        }
    }

    private fun setupTextWatchers() {
        // Clear error when user starts typing
        binding.etStudentId.addTextChangedListener {
            binding.tilStudentId.error = null
        }

        binding.etPassword.addTextChangedListener {
            binding.tilPassword.error = null
        }
    }

    private fun validateInput(): Boolean {
        val studentId = binding.etStudentId.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        var isValid = true

        // Validate Student ID
        if (studentId.isEmpty()) {
            binding.tilStudentId.error = "Please enter your Student ID"
            isValid = false
        }

        // Validate Password
        if (password.isEmpty()) {
            binding.tilPassword.error = "Please enter your password"
            isValid = false
        }

        return isValid
    }

    private fun performLogin() {
        val studentId = binding.etStudentId.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Show loading state
        binding.btnSignIn.isEnabled = false
        binding.btnSignIn.text = "Signing in..."

        Log.d(TAG, "Attempting login for Student ID: $studentId")

        // Query Firebase for student
        database.child("students").child(studentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "Firebase response received. Exists: ${snapshot.exists()}")

                    if (snapshot.exists()) {
                        val storedPassword = snapshot.child("password").getValue(String::class.java)
                        val studentName = snapshot.child("name").getValue(String::class.java) ?: "Student"
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""
                        val matricNo = snapshot.child("matricNo").getValue(String::class.java) ?: ""
                        val phone = snapshot.child("phone").getValue(String::class.java) ?: ""

                        Log.d(TAG, "Student found: $studentName")

                        if (storedPassword == password) {
                            // ✅ Login successful - SAVE TO SHAREDPREFERENCES
                            Log.d(TAG, "Login successful!")

                            // ✅ STEP 1: CLEAR OLD DATA FIRST
                            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            sharedPref.edit().clear().apply()
                            Log.d(TAG, "✅ Cleared old SharedPreferences")

                            // ✅ STEP 2: SAVE NEW STUDENT DATA
                            sharedPref.edit().apply {
                                putString("studentId", studentId)
                                putString("studentName", studentName)
                                putString("studentEmail", email)
                                putString("matricNo", matricNo)
                                putString("studentPhone", phone)
                                apply()
                            }
                            Log.d(TAG, "✅ Saved: ID=$studentId, Name=$studentName")

                            Toast.makeText(
                                this@StudentLoginActivity,
                                "Welcome back, $studentName!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // ✅ STEP 3: Navigate to MainActivity
                            val intent = Intent(this@StudentLoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()

                        } else {
                            // Wrong password
                            Log.w(TAG, "Invalid password")
                            showError("Invalid password. Please try again.")
                        }
                    } else {
                        // Student ID not found
                        Log.w(TAG, "Student ID not found in database")
                        showError("Student ID not found. Please check and try again.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Database error: ${error.message}", error.toException())
                    showError("Connection error: ${error.message}")
                }
            })
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.btnSignIn.isEnabled = true
        binding.btnSignIn.text = "Sign In"
    }
}