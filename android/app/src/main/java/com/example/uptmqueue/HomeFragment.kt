package com.example.uptmqueue.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uptmqueue.AnnouncementAdapter
import com.example.uptmqueue.Announcement
import com.example.uptmqueue.ChatActivity
import com.example.uptmqueue.FAQActivity
import com.example.uptmqueue.JoinQueueActivity
import com.example.uptmqueue.MainActivity
import com.example.uptmqueue.R
import com.example.uptmqueue.databinding.FragmentHomeBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var mainActivity: MainActivity
    private lateinit var announcementAdapter: AnnouncementAdapter
    private val announcements = mutableListOf<Announcement>()

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = activity as MainActivity
        database = FirebaseDatabase.getInstance().reference

        setupWelcomeSection()
        setupClickListeners()
        setupAnnouncementRecyclerView()
        loadQueueStatus()
        loadAnnouncements()
    }

    private fun setupAnnouncementRecyclerView() {
        announcementAdapter = AnnouncementAdapter(announcements)
        binding.rvAnnouncements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = announcementAdapter
        }
    }

    private fun loadAnnouncements() {
        database.child("announcements")
            .orderByChild("timestamp")
            .limitToLast(3)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return

                    announcements.clear()

                    for (child in snapshot.children) {
                        val announcement = child.getValue(Announcement::class.java)
                        announcement?.let { announcements.add(it) }
                    }

                    // Sort latest first
                    announcements.sortByDescending { it.timestamp }

                    if (announcements.isEmpty()) {
                        binding.tvAnnouncementLoading.text = "No announcements yet"
                        binding.tvAnnouncementLoading.visibility = View.VISIBLE
                        binding.rvAnnouncements.visibility = View.GONE
                    } else {
                        binding.tvAnnouncementLoading.visibility = View.GONE
                        binding.rvAnnouncements.visibility = View.VISIBLE
                        announcementAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading announcements: ${error.message}")
                }
            })
    }

    // ... rest of your existing code unchanged
    private fun setupWelcomeSection() {
        val firstName = mainActivity.studentName.split(" ").firstOrNull() ?: mainActivity.studentName
        binding.tvWelcome.text = "Hello, $firstName Welcome Back"
    }

    private fun setupClickListeners() {
        binding.cardLiveChat.setOnClickListener {
            startActivity(Intent(requireContext(), ChatActivity::class.java))
        }
        binding.cardPayment.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://epay.kptm.edu.my/")))
        }
        binding.cardFAQ.setOnClickListener {
            startActivity(Intent(requireContext(), FAQActivity::class.java))
        }
        binding.cardStudentId.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://digitalid.kptm.edu.my:8443/signin.php")))
        }
        binding.btnJoinQueue.setOnClickListener {
            val intent = Intent(requireContext(), JoinQueueActivity::class.java).apply {
                putExtra("STUDENT_ID", mainActivity.studentId)
                putExtra("STUDENT_NAME", mainActivity.studentName)
            }
            startActivity(intent)
        }
        binding.cardTuitionQueue.setOnClickListener {
            Toast.makeText(requireContext(), "Viewing Tuition & Fees Queue Details...", Toast.LENGTH_SHORT).show()
        }
        binding.cardFinancialQueue.setOnClickListener {
            Toast.makeText(requireContext(), "Viewing Financial Aid Queue Details...", Toast.LENGTH_SHORT).show()
        }
        binding.ivProfilePic.setOnClickListener {
            mainActivity.navigateToFragment(ProfileFragment(), R.id.nav_profile)
        }
    }

    private var queueListener: ValueEventListener? = null

    private fun loadQueueStatus() {
        queueListener?.let {
            database.child("queue").child("active").removeEventListener(it)
        }

        queueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                var counter1Count = 0
                var counter2Count = 0
                var counter3Count = 0

                for (ticket in snapshot.children) {
                    val status = ticket.child("status").getValue(String::class.java)
                    val counterNumber = ticket.child("counterNumber").getValue(Int::class.java) ?: 0

                    if (status == "waiting" || status == "serving") {
                        when (counterNumber) {
                            1 -> counter1Count++
                            2 -> counter2Count++
                            3 -> counter3Count++
                        }
                    }
                }

                binding.tvTuitionCurrent.text = "$counter1Count waiting"
                binding.tvTuitionWaitTime.text = "${counter1Count * 3} min"
                binding.tvCollectionCurrent.text = "$counter2Count waiting"
                binding.tvCollectionWaitTime.text = "${counter2Count * 3} min"
                binding.tvFinancialCurrent.text = "$counter3Count waiting"
                binding.tvFinancialWaitTime.text = "${counter3Count * 3} min"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error: ${error.message}")
            }
        }

        database.child("queue").child("active").addValueEventListener(queueListener!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        queueListener?.let {
            database.child("queue").child("active").removeEventListener(it)
        }
        _binding = null
    }
}