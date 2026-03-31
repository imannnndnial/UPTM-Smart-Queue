package com.example.uptmqueue

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.uptmqueue.databinding.ActivityJoinQueueBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class JoinQueueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinQueueBinding
    private lateinit var database: DatabaseReference

    private var studentId: String = ""
    private var studentName: String = ""

    private var isJoiningQueue = false
    private var loadingDialog: AlertDialog? = null

    // Track counter status
    private val counterStatus = mutableMapOf<Int, String>()

    private enum class ServiceType(
        val key: String,
        val displayName: String,
        val counterNumber: Int
    ) {
        STUDENT_ACCOUNT("student_account", "Student Account", 1),
        SCHOLARSHIP("scholarship", "Scholarship Inquiry", 3),
        COLLECTION("collection", "Collection", 2)
    }

    companion object {
        private const val TAG = "JoinQueueActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference

        getStudentDataFromIntent()
        setupToolbar()
        setupClickListeners()
        loadCounterStatuses() // ⬅️ NEW: Load counter statuses first
        setupFaqNotice()
    }

    private fun getStudentDataFromIntent() {
        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        studentName = intent.getStringExtra("STUDENT_NAME") ?: "Student"
        Log.d(TAG, "Student: $studentName (ID: $studentId)")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClickListeners() {
        binding.cardStudentAccount.setOnClickListener {
            if (!isJoiningQueue) {
                handleCardClick(ServiceType.STUDENT_ACCOUNT)
            }
        }

        binding.cardScholarship.setOnClickListener {
            if (!isJoiningQueue) {
                handleCardClick(ServiceType.SCHOLARSHIP)
            }
        }

        binding.cardCollection.setOnClickListener {
            if (!isJoiningQueue) {
                handleCardClick(ServiceType.COLLECTION)
            }
        }

        binding.tvFaqNotice.movementMethod = LinkMovementMethod.getInstance()
        binding.faqNotice.setOnClickListener {
            val intent = Intent(this, FAQActivity::class.java)
            startActivity(intent)
        }
    }

    // ⬇️ NEW: Load all counter statuses
    private fun loadCounterStatuses() {
        database.child("counters")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Load all counter statuses
                    for (counterSnapshot in snapshot.children) {
                        val counterId = counterSnapshot.key ?: continue
                        val status = counterSnapshot.child("status").getValue(String::class.java) ?: "open"

                        val counterNumber = when(counterId) {
                            "B01" -> 1
                            "B02" -> 2
                            "B03" -> 3
                            else -> continue
                        }

                        counterStatus[counterNumber] = status
                    }

                    // Update UI for all cards
                    updateCardVisuals()

                    // Load queue counts
                    loadQueueCounts()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading counter statuses: ${error.message}")
                }
            })
    }

    // ⬇️ NEW: Update card visuals based on status
    private fun updateCardVisuals() {
        updateCardStatus(
            binding.cardStudentAccount,
            binding.tvStudentAccountTitle,
            binding.tvStudentAccountDesc,
            ServiceType.STUDENT_ACCOUNT.counterNumber
        )

        updateCardStatus(
            binding.cardCollection,
            binding.tvCollectionTitle,
            binding.tvCollectionDesc,
            ServiceType.COLLECTION.counterNumber
        )

        updateCardStatus(
            binding.cardScholarship,
            binding.tvScholarshipTitle,
            binding.tvScholarshipDesc,
            ServiceType.SCHOLARSHIP.counterNumber
        )
    }

    private fun updateCardStatus(
        card: CardView,
        titleView: TextView,
        descView: TextView,
        counterNumber: Int
    ) {
        val status = counterStatus[counterNumber] ?: "open"

        if (status == "closed") {
            // ⬇️ CLOSED VISUAL EFFECTS
            card.alpha = 0.5f  // Make card semi-transparent
            card.isClickable = false
            card.isFocusable = false

            titleView.setTextColor(Color.parseColor("#666666"))  // Grey out title
            descView.setTextColor(Color.parseColor("#444444"))   // Grey out description

        } else {
            // ⬇️ OPEN (NORMAL) STATE
            card.alpha = 1.0f
            card.isClickable = true
            card.isFocusable = true

            titleView.setTextColor(Color.WHITE)
            descView.setTextColor(Color.parseColor("#9CA3AF"))  // gray_text color
        }
    }

    private fun loadQueueCounts() {
        loadQueueCount(ServiceType.STUDENT_ACCOUNT.counterNumber, binding.tvStudentAccountWaiting)
        loadQueueCount(ServiceType.SCHOLARSHIP.counterNumber, binding.tvScholarshipWaiting)
        loadQueueCount(ServiceType.COLLECTION.counterNumber, binding.tvCollectionWaiting)
    }

    private fun loadQueueCount(counterNumber: Int, textView: TextView) {
        database.child("queue").child("active")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = counterStatus[counterNumber] ?: "open"

                    if (status == "closed") {
                        // ⬇️ SHOW CLOSED STATUS
                        textView.text = "CLOSED"
                        textView.setTextColor(Color.parseColor("#EF4444"))  // Red color
                        Log.d(TAG, "Counter $counterNumber is CLOSED")
                        return
                    }

                    // ⬇️ COUNT WAITING TICKETS
                    var count = 0
                    for (ticketSnapshot in snapshot.children) {
                        val ticketStatus = ticketSnapshot.child("status").getValue(String::class.java)
                        val counter = ticketSnapshot.child("counterNumber").getValue(Int::class.java)
                        if (ticketStatus == "waiting" && counter == counterNumber) {
                            count++
                        }
                    }

                    // ⬇️ UPDATE WAITING COUNT
                    textView.text = "$count Waiting"

                    // Reset color based on counter
                    when(counterNumber) {
                        1 -> textView.setTextColor(Color.parseColor("#3B82F6"))  // Blue
                        2 -> textView.setTextColor(Color.parseColor("#10B981"))  // Green
                        3 -> textView.setTextColor(Color.parseColor("#F97316"))  // Orange
                    }

                    Log.d(TAG, "Counter $counterNumber queue count: $count")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading counter $counterNumber queue count: ${error.message}")
                }
            })
    }

    private fun setupFaqNotice() {
        val htmlText = "Check the <font color='#2D7EF7'><u>FAQ section</u></font> first; your question might already be answered there!"
        binding.tvFaqNotice.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
    }

    // ⬇️ NEW: Handle card click with status check
    private fun handleCardClick(serviceType: ServiceType) {
        val status = counterStatus[serviceType.counterNumber] ?: "open"

        if (status == "closed") {
            // ⬇️ SHOW CLOSED ALERT
            AlertDialog.Builder(this)
                .setTitle("Counter Closed")
                .setMessage("Sorry, Counter ${serviceType.counterNumber} (${serviceType.displayName}) is currently closed.\n\nPlease try again later or choose a different service.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()

            Log.w(TAG, "Counter ${serviceType.counterNumber} is closed, cannot join")
            return
        }

        // ⬇️ COUNTER IS OPEN - PROCEED
        joinQueue(serviceType)
    }

    private fun joinQueue(serviceType: ServiceType) {
        Log.d(TAG, "Attempting to join ${serviceType.displayName} queue")

        if (isJoiningQueue) {
            Log.d(TAG, "Already processing request, please wait...")
            return
        }

        isJoiningQueue = true
        showLoadingDialog("Checking queue status...")

        checkIfHasActiveTicket { hasActiveTicket, existingService ->
            if (hasActiveTicket) {
                hideLoadingDialog()
                isJoiningQueue = false

                AlertDialog.Builder(this)
                    .setTitle("Already in Queue")
                    .setMessage("You already have an active ticket for $existingService.\n\nPlease complete or cancel your current ticket before joining another queue.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .setNegativeButton("Go to My Tickets") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    .show()

                Log.w(TAG, "Student already has active ticket in $existingService")
            } else {
                addToQueue(serviceType)
            }
        }
    }

    private fun checkIfHasActiveTicket(callback: (Boolean, String?) -> Unit) {
        database.child("queue").child("active")
            .orderByChild("studentId")
            .equalTo(studentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasActiveTicket = false
                    var activeService: String? = null

                    if (snapshot.exists()) {
                        for (ticketSnapshot in snapshot.children) {
                            val status = ticketSnapshot.child("status").getValue(String::class.java)
                            if (status == "waiting") {
                                hasActiveTicket = true
                                activeService = ticketSnapshot.child("serviceCategory").getValue(String::class.java)
                                break
                            }
                        }
                    }

                    callback(hasActiveTicket, activeService)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error checking active ticket: ${error.message}")
                    callback(false, null)
                }
            })
    }

    private fun addToQueue(serviceType: ServiceType) {
        showLoadingDialog("Joining ${serviceType.displayName}...")

        val queueId = database.child("queue").child("active").push().key

        if (queueId == null) {
            hideLoadingDialog()
            isJoiningQueue = false
            Toast.makeText(this, "Failed to generate queue ID", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("queues").child(serviceType.key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date())
                    val lastResetDate = snapshot.child("lastResetDate").getValue(String::class.java) ?: ""

                    val nextNumber = if (lastResetDate != today) {
                        1
                    } else {
                        snapshot.child("nextTicketNumber").getValue(Int::class.java) ?: 1
                    }

                    // ✅ TAMBAH NI - declare formattedTicketNumber
                    val formattedTicketNumber = String.format("%03d", nextNumber)

                    val prefix = when(serviceType) {
                        ServiceType.STUDENT_ACCOUNT -> "A"
                        ServiceType.SCHOLARSHIP -> "S"
                        ServiceType.COLLECTION -> "C"
                    }
                    val queueNumber = "$prefix-$formattedTicketNumber"

                    val ticketData = hashMapOf(
                        "queueNumber" to queueNumber,
                        "studentId" to studentId,
                        "studentName" to studentName,
                        "serviceCategory" to serviceType.displayName,
                        "serviceType" to serviceType.key,
                        "status" to "waiting",
                        "createdAt" to System.currentTimeMillis(),
                        "counterNumber" to serviceType.counterNumber,
                        "ticketNumber" to nextNumber,
                        "ticketNumberFormatted" to formattedTicketNumber
                    )

                    database.child("queue").child("active").child(queueId)
                        .setValue(ticketData)
                        .addOnSuccessListener {
                            // ✅ Update BOTH nextTicketNumber AND lastResetDate
                            database.child("queues").child(serviceType.key).updateChildren(
                                mapOf(
                                    "nextTicketNumber" to nextNumber + 1,
                                    "lastResetDate" to today  // ← simpan tarikh hari ni
                                )
                            ).addOnSuccessListener {
                                hideLoadingDialog()
                                isJoiningQueue = false
                                showSuccessDialog(serviceType, formattedTicketNumber)
                            }.addOnFailureListener { e ->
                                hideLoadingDialog()
                                isJoiningQueue = false
                                Toast.makeText(this@JoinQueueActivity, "Ticket created but update failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            hideLoadingDialog()
                            isJoiningQueue = false
                            Toast.makeText(this@JoinQueueActivity, "Failed to join queue. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    hideLoadingDialog()
                    isJoiningQueue = false

                    Log.e(TAG, "Error getting queue data: ${error.message}")
                    Toast.makeText(
                        this@JoinQueueActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun showLoadingDialog(message: String = "Please wait...") {
        if (loadingDialog == null) {
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
            dialogView.findViewById<TextView>(R.id.tvLoadingMessage)?.text = message
            builder.setView(dialogView)
            builder.setCancelable(false)
            loadingDialog = builder.create()
        }
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun showSuccessDialog(serviceType: ServiceType, ticketNumber: String) {
        val prefix = when(serviceType) {
            ServiceType.STUDENT_ACCOUNT -> "A"
            ServiceType.SCHOLARSHIP -> "S"
            ServiceType.COLLECTION -> "C"
        }

        val message = """
        Service: ${serviceType.displayName}
        
        Your Ticket Number:
        #$prefix-$ticketNumber
        
        📍 Please proceed to:
        COUNTER ${serviceType.counterNumber}
        
        Check 'My Tickets' to see your queue position.
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("✅ Ticket Created!")
            .setMessage(message)
            .setPositiveButton("View My Tickets") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoadingDialog()
    }
}