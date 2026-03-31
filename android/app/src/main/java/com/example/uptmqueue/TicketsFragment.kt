package com.example.uptmqueue.fragments

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
import com.example.uptmqueue.databinding.FragmentTicketsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketsFragment : Fragment() {

    private var _binding: FragmentTicketsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var mainActivity: MainActivity

    private var activeTicketId: String? = null
    private var activeServiceType: String? = null
    private var queueListener: ValueEventListener? = null

    companion object {
        private const val TAG = "TicketsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = activity as MainActivity
        database = FirebaseDatabase.getInstance().reference

        setupToolbar()
        setupClickListeners()
        setupSwipeRefresh()

        loadActiveTicket()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            mainActivity.navigateToFragment(HomeFragment(), R.id.nav_home)
        }
    }

    private fun setupClickListeners() {
        binding.tvViewInLine.setOnClickListener {
            if (activeServiceType != null) {
                Toast.makeText(requireContext(), "Opening queue line...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            showCancelDialog()
        }

        binding.ivMoreOptions.setOnClickListener {
            Toast.makeText(requireContext(), "More options", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }

    private fun refreshData() {
        loadActiveTicket()
        binding.swipeRefresh.isRefreshing = false
    }

    private fun loadActiveTicket() {
        Log.d(TAG, "Loading active ticket for student: ${mainActivity.studentId}")

        database.child("queue").child("active")
            .orderByChild("studentId")
            .equalTo(mainActivity.studentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (ticketSnapshot in snapshot.children) {
                            val status = ticketSnapshot.child("status").getValue(String::class.java)
                            if (status == "waiting" || status == "serving") {
                                activeTicketId = ticketSnapshot.key
                                activeServiceType = ticketSnapshot.child("serviceType").getValue(String::class.java)

                                Log.d(TAG, "Found active ticket: $activeTicketId (Status: $status)")

                                displayActiveTicket(ticketSnapshot)
                                setupRealTimeUpdates()
                                return
                            }
                        }
                    }

                    showNoActiveTicket()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading active ticket: ${error.message}")
                    showNoActiveTicket()
                }
            })
    }

    private fun displayActiveTicket(ticketSnapshot: DataSnapshot) {
        val queueNumber = ticketSnapshot.child("queueNumber").getValue(String::class.java) ?: "---"
        val serviceName = ticketSnapshot.child("serviceCategory").getValue(String::class.java) ?: "Service"
        val timestamp = ticketSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
        val counterNumber = ticketSnapshot.child("counterNumber").getValue(Int::class.java) ?: 0

        binding.cardActiveTicket.visibility = View.VISIBLE
        binding.noActiveTicketLayout.visibility = View.GONE

        binding.tvServiceName.text = serviceName.uppercase()
        binding.tvCounterBadge.text = "COUNTER $counterNumber"
        binding.tvTicketNumber.text = "#$queueNumber"

        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val joinedTime = dateFormat.format(Date(timestamp))
        binding.tvJoinedTime.text = "JOINED\n$joinedTime"

        Log.d(TAG, "Displayed active ticket: #$queueNumber at Counter $counterNumber")
    }

    private fun setupRealTimeUpdates() {
        removeRealTimeListeners()

        queueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || activeTicketId == null) {
                    removeRealTimeListeners()
                    return
                }

                database.child("queue").child("active").child(activeTicketId!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(ticketSnapshot: DataSnapshot) {
                            if (!isAdded || activeTicketId == null) return

                            if (!ticketSnapshot.exists()) {
                                removeRealTimeListeners()
                                showNoActiveTicket()
                                return
                            }

                            val myTimestamp = ticketSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                            val myCounter = ticketSnapshot.child("counterNumber").getValue(Int::class.java) ?: 0

                            database.child("queue").child("active")
                                .orderByChild("createdAt")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(allQueuesSnapshot: DataSnapshot) {
                                        var position = 0

                                        for (queue in allQueuesSnapshot.children) {
                                            val qStatus = queue.child("status").getValue(String::class.java)
                                            val qTimestamp = queue.child("createdAt").getValue(Long::class.java) ?: 0L
                                            val qCounter = queue.child("counterNumber").getValue(Int::class.java) ?: 0

                                            if (qStatus == "waiting" && qCounter == myCounter && qTimestamp < myTimestamp) {
                                                position++
                                            }
                                        }

                                        if (!isAdded) return

                                        binding.tvPosition.text = String.format("%02d", position)
                                        binding.tvPositionLabel.text = if (position > 0) "ahead of you" else "IT'S YOUR TURN!"

                                        val waitTime = position * 3
                                        binding.tvWaitTime.text = waitTime.toString()

                                        Log.d(TAG, "REAL-TIME UPDATE: Position = $position, Wait = $waitTime mins")

                                        val myStatus = ticketSnapshot.child("status").getValue(String::class.java)

                                        if (myStatus == "serving") {
                                            showYourTurnNotification()
                                        } else if (position == 0) {
                                            if (!isAdded) return
                                            binding.tvPosition.text = "00"
                                            binding.tvPositionLabel.text = "YOU'RE NEXT!"
                                            binding.tvWaitTime.text = "0"
                                        } else if (position <= 2) {
                                            showYourTurnSoonNotification()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e(TAG, "Error calculating position: ${error.message}")
                                    }
                                })
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Error getting ticket: ${error.message}")
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error listening to queue: ${error.message}")
            }
        }

        database.child("queue").child("active").addValueEventListener(queueListener!!)
    }

    private fun showYourTurnSoonNotification() {
        binding.tvPositionLabel.text = "almost your turn!"
    }

    private fun showYourTurnNotification() {
        binding.tvPosition.text = "00"
        binding.tvPositionLabel.text = "IT'S YOUR TURN!"
        binding.tvWaitTime.text = "0"

        if (isAdded) {
            AlertDialog.Builder(requireContext())
                .setTitle("Your Turn!")
                .setMessage("Please proceed to the counter now.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
        }
    }

    private fun showNoActiveTicket() {
        binding.cardActiveTicket.visibility = View.GONE
        binding.noActiveTicketLayout.visibility = View.VISIBLE
        Log.d(TAG, "No active ticket found")
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Ticket")
            .setMessage("Are you sure you want to cancel your ticket? This action cannot be undone.")
            .setPositiveButton("Yes, Cancel") { dialog, _ ->
                cancelTicket()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun cancelTicket() {
        if (activeTicketId == null) {
            Toast.makeText(requireContext(), "No active ticket to cancel", Toast.LENGTH_SHORT).show()
            return
        }

        removeRealTimeListeners()

        database.child("queue").child("active").child(activeTicketId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showNoActiveTicket()
                        return
                    }

                    val historyData = snapshot.value as? HashMap<String, Any> ?: return

                    historyData["status"] = "cancelled"
                    historyData["completedTimestamp"] = System.currentTimeMillis()
                    historyData["cancelledTimestamp"] = System.currentTimeMillis()
                    historyData["waitTimeMinutes"] = 0

                    // ✅ Save history DULU, baru delete bila dah confirm saved
                    database.child("tickets_history")
                        .child(mainActivity.studentId)
                        .child(activeTicketId!!)
                        .setValue(historyData)
                        .addOnSuccessListener {
                            // ✅ History dah saved, baru boleh delete dari queue
                            database.child("queue").child("active").child(activeTicketId!!)
                                .removeValue()
                                .addOnSuccessListener {
                                    activeTicketId = null
                                    activeServiceType = null
                                    Toast.makeText(requireContext(), "Ticket cancelled", Toast.LENGTH_SHORT).show()
                                    showNoActiveTicket()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Failed to remove queue: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to save history: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun removeRealTimeListeners() {
        queueListener?.let { listener ->
            try {
                database.child("queue").child("active").removeEventListener(listener)
                Log.d(TAG, "Removed listener")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing listener: ${e.message}")
            }
        }
        queueListener = null
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeRealTimeListeners()
        _binding = null
    }
}