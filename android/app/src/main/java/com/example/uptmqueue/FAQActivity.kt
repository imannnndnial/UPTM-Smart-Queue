package com.example.uptmqueue

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uptmqueue.databinding.ActivityFaqBinding

class FAQActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaqBinding
    private lateinit var faqAdapter: FAQAdapter
    private var allFAQs = mutableListOf<FAQItem>()

    companion object {
        private const val TAG = "FAQActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadFAQs()
        setupCategoryFilters()
        setupChatSupport()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        faqAdapter = FAQAdapter(mutableListOf())
        binding.recyclerFAQ.apply {
            layoutManager = LinearLayoutManager(this@FAQActivity)
            adapter = faqAdapter
        }
    }

    private fun loadFAQs() {
        allFAQs = mutableListOf(
            FAQItem(
                category = "General",
                question = "What is the Bursary Smart Queue system?",
                answer = "The Bursary Smart Queue is a virtual queue system that allows you to join queues remotely and track your position in real-time. You can join from anywhere on campus and receive notifications when it's your turn."
            ),
            FAQItem(
                category = "General",
                question = "How do I join the virtual queue?",
                answer = "Simply tap 'Join Virtual Queue' on the home screen, select the service you need (Student Account, Scholarship, Collection, or General Inquiries), and you'll receive a ticket number instantly."
            ),
            FAQItem(
                category = "Queue Management",
                question = "Can I join multiple queues at once?",
                answer = "No, you can only be in one queue at a time. You must complete or cancel your current ticket before joining another queue."
            ),
            FAQItem(
                category = "Queue Management",
                question = "How accurate is the wait time estimate?",
                answer = "Wait times are calculated based on an average of 3 minutes per person. Actual wait times may vary depending on the complexity of each inquiry."
            ),
            FAQItem(
                category = "Queue Management",
                question = "Can I cancel my ticket?",
                answer = "Yes! Go to 'My Tickets', view your active ticket, and tap 'Cancel'. However, you cannot rejoin the same queue immediately after cancelling."
            ),
            FAQItem(
                category = "Queue Management",
                question = "What happens if I miss my turn?",
                answer = "If you don't arrive at the counter within 5 minutes of your turn, your ticket will be automatically cancelled and moved to the end of the queue."
            ),
            FAQItem(
                category = "Notifications",
                question = "Will I get notified when it's my turn?",
                answer = "Yes! You'll receive a push notification when you're 2 people away from the counter and another notification when it's your turn."
            ),
            FAQItem(
                category = "Student Account",
                question = "What can I do at the Student Account counter?",
                answer = "Student Account handles: tuition fee inquiries, payment verification, outstanding balance checks, receipt printing, and account statements."
            ),
            FAQItem(
                category = "Student Account",
                question = "What documents do I need for fee inquiries?",
                answer = "Bring your Student ID and any payment receipts or transaction references if you're inquiring about specific payments."
            ),
            FAQItem(
                category = "Scholarship",
                question = "What scholarship services are available?",
                answer = "Scholarship services include: application assistance, eligibility checks, merit scholarship inquiries, external funding information, and scholarship renewal."
            ),
            FAQItem(
                category = "Scholarship",
                question = "When can I apply for scholarships?",
                answer = "Merit-based scholarships open at the start of each semester. Check the announcements section on the home page for exact dates and deadlines."
            ),
            FAQItem(
                category = "Collection",
                question = "What can I collect from the Bursary?",
                answer = "You can collect: refund cheques, allowance payouts, scholarship disbursements, and any official financial documents."
            ),
            FAQItem(
                category = "Collection",
                question = "Do I need to bring anything for collection?",
                answer = "Yes, you must bring your Student ID card. For cheque collection, you may also need to sign acknowledgment forms."
            ),
            FAQItem(
                category = "Technical",
                question = "The app isn't updating my queue position. What should I do?",
                answer = "Try pulling down to refresh on the My Tickets page. If the issue persists, check your internet connection or restart the app."
            ),
            FAQItem(
                category = "Technical",
                question = "I didn't receive my notification. Why?",
                answer = "Make sure notifications are enabled in your phone settings. Go to Settings > Apps > UPTM Queue > Notifications and ensure they are turned on."
            ),
            FAQItem(
                category = "Contact",
                question = "What are the Bursary operating hours?",
                answer = "Monday - Thursday: 8:00 AM - 5:00 PM\nFriday: 8:00 AM - 12:00 PM, 2:00 PM - 5:00 PM\nClosed on weekends and public holidays."
            ),
            FAQItem(
                category = "Contact",
                question = "Who can I contact for more help?",
                answer = "For technical issues with the app, email: support@uptm.edu.my\nFor bursary inquiries, email: bursary@uptm.edu.my\nOr visit the Bursary counter in person during operating hours."
            )
        )

        // Show all FAQs initially
        faqAdapter.updateData(allFAQs)
    }

    private fun setupCategoryFilters() {
        binding.chipAllTopics.setOnClickListener {
            filterFAQs("All")
        }

        binding.chipPayments.setOnClickListener {
            filterFAQs("Student Account")
        }

        binding.chipScholarship.setOnClickListener {
            filterFAQs("Scholarship")
        }

        binding.chipQueue.setOnClickListener {
            filterFAQs("Queue Management")
        }

        binding.chipTechnical.setOnClickListener {
            filterFAQs("Technical")
        }
    }

    private fun filterFAQs(category: String) {
        val filteredList = if (category == "All") {
            allFAQs
        } else {
            allFAQs.filter { it.category == category }
        }

        faqAdapter.updateData(filteredList)

        // Scroll to top after filter
        binding.recyclerFAQ.smoothScrollToPosition(0)
    }

    private fun setupChatSupport() {
        binding.btnChatSupport.setOnClickListener {
            // Open email client or live chat
            openChatSupport()
        }
    }

    private fun openChatSupport() {
        val intent = Intent(this, ChatActivity::class.java)
        startActivity(intent)
    }
}

// Data class for FAQ items
data class FAQItem(
    val category: String,
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)