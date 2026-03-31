package com.example.uptmqueue.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.uptmqueue.MainActivity
import com.example.uptmqueue.R
import com.example.uptmqueue.StudentLoginActivity
import com.example.uptmqueue.databinding.FragmentProfileBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainActivity: MainActivity

    companion object {
        private const val TAG = "ProfileFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = activity as MainActivity

        setupToolbar()
        displayStudentInfo()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            mainActivity.navigateToFragment(HomeFragment(), R.id.nav_home)
        }
    }

    private fun displayStudentInfo() {
        binding.apply {
            tvStudentName.text = mainActivity.studentName
            tvStudentId.text = "ID: ${mainActivity.studentId}"
            tvMatricNumber.text = "Matric: ${mainActivity.matricNo}"
            tvEmail.text = mainActivity.studentEmail.ifEmpty { "Not available" }
            tvPhone.text = mainActivity.studentPhone.ifEmpty { "Not available" }
        }
    }

    private fun setupClickListeners() {
        binding.cardChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.cardPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Opening privacy policy...", Toast.LENGTH_SHORT).show()
        }

        val sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        var isLoadingPrefs = true

        binding.switchQueue.setOnCheckedChangeListener { _, isChecked ->
            if (!isLoadingPrefs) {
                sharedPref.edit().putBoolean("notif_queue", isChecked).apply()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = notificationManager.getNotificationChannel("queue_channel")
                    channel?.importance = if (isChecked) android.app.NotificationManager.IMPORTANCE_HIGH
                    else android.app.NotificationManager.IMPORTANCE_NONE
                    channel?.let { notificationManager.createNotificationChannel(it) }
                }
            }
        }

        binding.switchMessages.setOnCheckedChangeListener { _, isChecked ->
            if (!isLoadingPrefs) {
                sharedPref.edit().putBoolean("notif_messages", isChecked).apply()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = notificationManager.getNotificationChannel("messages_channel")
                    channel?.importance = if (isChecked) android.app.NotificationManager.IMPORTANCE_HIGH
                    else android.app.NotificationManager.IMPORTANCE_NONE
                    channel?.let { notificationManager.createNotificationChannel(it) }
                }
            }
        }

        binding.switchAnnouncements.setOnCheckedChangeListener { _, isChecked ->
            if (!isLoadingPrefs) {
                sharedPref.edit().putBoolean("notif_announcements", isChecked).apply()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = notificationManager.getNotificationChannel("announcements_channel")
                    channel?.importance = if (isChecked) android.app.NotificationManager.IMPORTANCE_DEFAULT
                    else android.app.NotificationManager.IMPORTANCE_NONE
                    channel?.let { notificationManager.createNotificationChannel(it) }
                }
            }
        }

        binding.switchQueue.isChecked = sharedPref.getBoolean("notif_queue", true)
        binding.switchMessages.isChecked = sharedPref.getBoolean("notif_messages", true)
        binding.switchAnnouncements.isChecked = sharedPref.getBoolean("notif_announcements", true)
        isLoadingPrefs = false

        var isExpanded = false
        binding.layoutNotifHeader.setOnClickListener {
            isExpanded = !isExpanded
            if (isExpanded) {
                binding.layoutNotifContent.visibility = View.VISIBLE
                binding.ivNotifArrow.animate().rotation(270f).setDuration(200).start()
            } else {
                binding.layoutNotifContent.visibility = View.GONE
                binding.ivNotifArrow.animate().rotation(90f).setDuration(200).start()
            }
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)

        val etOldPassword = dialogView.findViewById<TextInputEditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirmChange)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val oldPass = etOldPassword.text.toString().trim()
            val newPass = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(requireContext(), "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(requireContext(), "New passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (oldPass == newPass) {
                Toast.makeText(requireContext(), "New password must be different from old password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            changePassword(oldPass, newPass, dialog, btnConfirm)
        }

        dialog.show()
    }

    private fun changePassword(
        oldPassword: String,
        newPassword: String,
        dialog: AlertDialog,
        btnConfirm: MaterialButton
    ) {
        val studentId = mainActivity.studentId

        if (studentId.isEmpty()) {
            Toast.makeText(requireContext(), "Student ID not found. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        btnConfirm.isEnabled = false
        btnConfirm.text = "Changing..."

        val dbRef = FirebaseDatabase.getInstance().getReference("students").child(studentId)

        dbRef.get().addOnSuccessListener { snapshot ->
            val currentPassword = snapshot.child("password").getValue(String::class.java)

            if (currentPassword == null) {
                btnConfirm.isEnabled = true
                btnConfirm.text = "Change Password"
                Toast.makeText(requireContext(), "Error retrieving data.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            if (currentPassword != oldPassword) {
                btnConfirm.isEnabled = true
                btnConfirm.text = "Change Password"
                Toast.makeText(requireContext(), "❌ Current password is incorrect!", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            // Password match — update dengan yang baru
            dbRef.child("password").setValue(newPassword)
                .addOnSuccessListener {
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "✅ Password changed successfully!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "✅ Password changed successfully")
                }
                .addOnFailureListener { error ->
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Change Password"
                    Toast.makeText(requireContext(), "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "❌ Update password failed: ${error.message}")
                }

        }.addOnFailureListener { error ->
            btnConfirm.isEnabled = true
            btnConfirm.text = "Change Password"
            Toast.makeText(requireContext(), "Failed to fetch data: ${error.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "❌ Fetch failed: ${error.message}")
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes, Logout") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        Log.d(TAG, "🚪 Performing logout...")

        val sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        val oldId = sharedPref.getString("studentId", "NONE")
        val oldName = sharedPref.getString("studentName", "NONE")
        Log.d(TAG, "📋 Clearing data for: $oldName ($oldId)")

        sharedPref.edit().clear().apply()
        Log.d(TAG, "✅ SharedPreferences cleared!")

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), StudentLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()

        Log.d(TAG, "✅ Logout complete!")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}