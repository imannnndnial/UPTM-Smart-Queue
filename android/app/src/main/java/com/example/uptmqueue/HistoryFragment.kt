package com.example.uptmqueue.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uptmqueue.MainActivity
import com.example.uptmqueue.R
import com.example.uptmqueue.TicketHistoryAdapter
import com.example.uptmqueue.TicketHistoryItem
import com.example.uptmqueue.databinding.FragmentHistoryBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var historyAdapter: TicketHistoryAdapter
    private lateinit var mainActivity: MainActivity

    private var allHistory = mutableListOf<TicketHistoryItem>()
    private var currentFilter = "all" // all, completed, cancelled

    companion object {
        private const val TAG = "HistoryFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = activity as MainActivity
        database = FirebaseDatabase.getInstance().reference

        setupToolbar()
        setupRecyclerView()
        setupFilterButtons()
        setupSwipeRefresh()

        loadTicketHistory()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            mainActivity.navigateToFragment(HomeFragment(), R.id.nav_home)
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = TicketHistoryAdapter(mutableListOf())
        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupFilterButtons() {
        binding.chipAll.setOnClickListener {
            currentFilter = "all"
            updateFilterUI()
            filterHistory()
        }

        binding.chipCompleted.setOnClickListener {
            currentFilter = "completed"
            updateFilterUI()
            filterHistory()
        }

        binding.chipCancelled.setOnClickListener {
            currentFilter = "cancelled"
            updateFilterUI()
            filterHistory()
        }
    }

    private fun updateFilterUI() {
        // Reset all chips
        binding.chipAll.isChecked = currentFilter == "all"
        binding.chipCompleted.isChecked = currentFilter == "completed"
        binding.chipCancelled.isChecked = currentFilter == "cancelled"
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadTicketHistory()
        }
    }

    private fun loadTicketHistory() {
        binding.progressBar.visibility = View.VISIBLE

        database.child("tickets_history").child(mainActivity.studentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allHistory.clear()

                    for (ticketSnapshot in snapshot.children) {
                        val queueNumber = ticketSnapshot.child("queueNumber").getValue(String::class.java) ?: ""
                        val serviceName = ticketSnapshot.child("serviceCategory").getValue(String::class.java) ?: ""
                        val status = ticketSnapshot.child("status").getValue(String::class.java) ?: ""

                        var timestamp = ticketSnapshot.child("completedTimestamp").getValue(Long::class.java)
                        if (timestamp == null || timestamp == 0L) {
                            timestamp = ticketSnapshot.child("cancelledTimestamp").getValue(Long::class.java)
                        }
                        if (timestamp == null || timestamp == 0L) {
                            timestamp = ticketSnapshot.child("completedAt").getValue(Long::class.java)
                        }
                        if (timestamp == null || timestamp == 0L) {
                            timestamp = ticketSnapshot.child("createdAt").getValue(Long::class.java)
                                ?: System.currentTimeMillis()
                        }

                        val duration = ticketSnapshot.child("waitTimeMinutes").getValue(Int::class.java) ?: 0
                        val cancelledBy = ticketSnapshot.child("cancelledBy").getValue(String::class.java) ?: ""

                        allHistory.add(
                            TicketHistoryItem(
                                serviceName = serviceName,
                                ticketNumber = "#$queueNumber",
                                date = formatDate(timestamp),
                                status = status,
                                duration = if (status == "cancelled" && cancelledBy == "staff") "staff" else "${duration}m"
                            )
                        )
                    }

                    // Sort by date (newest first)
                    allHistory.reverse()

                    // Update stats
                    updateStats()

                    // Apply current filter
                    filterHistory()

                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    Log.d(TAG, "Loaded ${allHistory.size} history items")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading history: ${error.message}")
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    showEmptyState()
                }
            })
    }

    private fun filterHistory() {
        val filteredList = when (currentFilter) {
            "completed" -> allHistory.filter { it.status == "completed" }
            "cancelled" -> allHistory.filter { it.status == "cancelled" }
            else -> allHistory
        }

        if (filteredList.isEmpty()) {
            showEmptyState()
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerHistory.visibility = View.VISIBLE
            historyAdapter.updateData(filteredList.toMutableList())
        }

        Log.d(TAG, "Filtered to ${filteredList.size} items (filter: $currentFilter)")
    }

    private fun updateStats() {
        val totalTickets = allHistory.size
        val completedTickets = allHistory.count { it.status == "completed" }
        val cancelledTickets = allHistory.count { it.status == "cancelled" }

        binding.tvTotalTickets.text = totalTickets.toString()
        binding.tvCompletedTickets.text = completedTickets.toString()
        binding.tvCancelledTickets.text = cancelledTickets.toString()
    }

    private fun showEmptyState() {
        binding.recyclerHistory.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE

        val message = when (currentFilter) {
            "completed" -> "No completed tickets yet"
            "cancelled" -> "No cancelled tickets"
            else -> "No ticket history yet"
        }
        binding.tvEmptyMessage.text = message
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    override fun onResume() {
        super.onResume()
        loadTicketHistory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}