package com.example.uptmqueue

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uptmqueue.databinding.ActivityChatBinding
import com.google.firebase.database.*
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var database: DatabaseReference
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private var chatId: String? = null
    private var studentId: String? = null
    private var studentName: String? = null
    private var staffId: String? = null
    private var staffName: String? = null
    private var counter: String? = null

    // ⏰ 48 JAM AUTO ARCHIVE
    private val ARCHIVE_AFTER_MILLIS = 48 * 60 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        studentId = sharedPref.getString("studentId", "")
        studentName = sharedPref.getString("studentName", "Student")

        database = FirebaseDatabase.getInstance().reference

        setupRecyclerView()
        checkOfficeHours()
        setupListeners()
        checkExistingChat()
    }

    private fun checkOfficeHours() {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        val isWeekday = dayOfWeek in 2..6
        val isOfficeHours = hourOfDay in 8..16

        if (!isWeekday || !isOfficeHours) {
            showOfficeClosedUI()
        } else {
            showOfficeOpenUI()
        }
    }

    private fun showOfficeClosedUI() {
        binding.btnSend.isEnabled = false
        binding.btnSend.alpha = 0.5f
        binding.etMessage.isEnabled = false
        binding.etMessage.hint = "Office hours: Mon-Fri, 9AM-5PM"
        binding.tvOnlineText.text = "Offline"
        binding.tvOnlineText.setTextColor(ContextCompat.getColor(this, R.color.error_red))

        val nextOpenTime = getNextOpeningTime()
        Toast.makeText(
            this,
            "Chat service is currently closed.\nAvailable: Mon-Fri, 9AM-5PM\n$nextOpenTime",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showOfficeOpenUI() {
        binding.btnSend.isEnabled = true
        binding.btnSend.alpha = 1f
        binding.etMessage.isEnabled = true
        binding.etMessage.hint = "Type your message..."
        binding.tvOnlineText.text = "Online"
        binding.tvOnlineText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
    }

    private fun getNextOpeningTime(): String {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        return when {
            dayOfWeek == Calendar.SATURDAY -> "Next available: Monday, 9AM"
            dayOfWeek == Calendar.SUNDAY -> "Next available: Monday, 9AM"
            hourOfDay < 8 -> "Opens today at 9AM"
            hourOfDay >= 17 -> {
                if (dayOfWeek == Calendar.FRIDAY) "Next available: Monday, 9AM"
                else "Next available: Tomorrow, 9AM"
            }
            else -> "Available now"
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages, studentId ?: "")
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSend.setOnClickListener {
            if (!isWithinOfficeHours()) {
                Toast.makeText(this, "Chat is only available Mon-Fri, 9AM-5PM", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendMessage()
        }

        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            if (isWithinOfficeHours()) {
                sendMessage()
            } else {
                Toast.makeText(this, "Chat is only available Mon-Fri, 9AM-5PM", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun isWithinOfficeHours(): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        return dayOfWeek in 2..6 && hourOfDay in 8..16
    }

    private fun checkExistingChat() {
        database.child("chats")
            .orderByChild("studentId")
            .equalTo(studentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (chatSnapshot in snapshot.children) {
                            val chat = chatSnapshot.getValue(Chat::class.java)

                            if (chat?.status == "active" && chat.archivedByStudent != true) {
                                // ✅ Check umur chat — kalau dah 48 jam, archive
                                val chatAge = System.currentTimeMillis() - chat.createdAt
                                if (chatAge >= ARCHIVE_AFTER_MILLIS) {
                                    archiveChatSilently(chat.chatId)
                                    createNewChat()
                                } else {
                                    chatId = chat.chatId
                                    staffId = chat.staffId
                                    staffName = chat.staffName
                                    counter = chat.counter
                                    updateUI()
                                    loadMessages()
                                    checkAndSendStatusUpdate()
                                }
                                return
                            }
                        }
                    }
                    createNewChat()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun checkAndSendStatusUpdate() {
        if (!isWithinOfficeHours()) return

        database.child("messages").child(chatId!!)
            .orderByChild("timestamp")
            .limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lastMsg = snapshot.children.firstOrNull()
                        ?.getValue(ChatMessage::class.java) ?: return

                    val isOfficeClosedMsg = lastMsg.senderId == "system" &&
                            (lastMsg.message.contains("office hours", ignoreCase = true) ||
                                    lastMsg.message.contains("9AM", ignoreCase = true))

                    if (isOfficeClosedMsg) sendAutomaticGreeting()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ✅ ARCHIVE SECARA SENYAP - staff still boleh baca
    private fun archiveChatSilently(id: String) {
        val updates = mapOf(
            "archivedByStudent" to true,
            "archivedAt" to System.currentTimeMillis()
        )
        database.child("chats").child(id).updateChildren(updates)
    }

    private fun createNewChat() {
        val newChatId = database.child("chats").push().key ?: return

        staffId = "staff_001"
        staffName = "Bursar Staff"
        counter = "B01"
        chatId = newChatId

        val chat = Chat(
            chatId = newChatId,
            studentId = studentId ?: "",
            studentName = studentName ?: "Student",
            staffId = staffId,
            staffName = staffName,
            counter = counter,
            status = "active",
            lastMessage = "",
            lastMessageTime = System.currentTimeMillis(),
            lastMessageBy = "student",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            unreadCount = mapOf("student" to 0, "staff" to 0),
            archivedByStudent = false,
            archivedAt = 0
        )

        database.child("chats").child(newChatId).setValue(chat)
            .addOnSuccessListener {
                updateUI()
                loadMessages()

                if (isWithinOfficeHours()) {
                    sendAutomaticGreeting()
                } else {
                    sendOfficeClosedMessage()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create chat", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendAutomaticGreeting() {
        val greetingMessage = ChatMessage(
            messageId = database.child("messages").child(chatId!!).push().key ?: "",
            chatId = chatId ?: "",
            senderId = staffId ?: "",
            senderType = "staff",
            message = "Assalamualaikum! Welcome to the Bursar Help Center. How can I help you today?",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        database.child("messages").child(chatId!!).child(greetingMessage.messageId).setValue(greetingMessage)
    }

    private fun sendOfficeClosedMessage() {
        val closedMessage = ChatMessage(
            messageId = database.child("messages").child(chatId!!).push().key ?: "",
            chatId = chatId ?: "",
            senderId = "system",
            senderType = "staff",
            senderName = "Bursar Staff",
            message = "Thank you for contacting us. Our office hours are Monday - Friday, 9AM - 5PM. Please send your message and we will respond during office hours.",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        database.child("messages").child(chatId!!).child(closedMessage.messageId).setValue(closedMessage)
    }

    private fun updateUI() {
        binding.tvStaffName.text = "Bursar Support"
        binding.tvCounterInfo.text = "Bursar Office"
    }

    private fun loadMessages() {
        database.child("messages").child(chatId!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages.clear()
                    for (msgSnapshot in snapshot.children) {
                        val message = msgSnapshot.getValue(ChatMessage::class.java)
                        message?.let { messages.add(it) }
                    }
                    messages.sortBy { it.timestamp }
                    chatAdapter.notifyDataSetChanged()

                    if (messages.isNotEmpty()) {
                        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                    }

                    markMessagesAsRead()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Error loading messages", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()

        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        if (chatId == null) {
            Toast.makeText(this, "Chat not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        val messageId = database.child("messages").child(chatId!!).push().key ?: return

        val message = ChatMessage(
            messageId = messageId,
            chatId = chatId ?: "",
            senderId = studentId ?: "",
            senderType = "student",
            senderName = studentName ?: "Student",
            message = messageText,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        val studentMessageCount = messages.count { it.senderType == "student" }
        val isFirstMessage = studentMessageCount == 0

        database.child("messages").child(chatId!!).child(messageId).setValue(message)
            .addOnSuccessListener {
                val updates = mapOf(
                    "lastMessage" to messageText,
                    "lastMessageTime" to System.currentTimeMillis(),
                    "lastMessageBy" to "student",
                    "updatedAt" to System.currentTimeMillis(),
                    "unreadCount/staff" to ServerValue.increment(1)
                )
                database.child("chats").child(chatId!!).updateChildren(updates)
                binding.etMessage.text.clear()

                if (isFirstMessage) sendAutoReplyMessage()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendAutoReplyMessage() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed

            val autoReplyMessage = ChatMessage(
                messageId = database.child("messages").child(chatId!!).push().key ?: "",
                chatId = chatId ?: "",
                senderId = "system",
                senderType = "staff",
                senderName = "Bursar Office",
                message = "Thank you for your message. Please wait, we will respond during office hours",
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            database.child("messages").child(chatId!!).child(autoReplyMessage.messageId).setValue(autoReplyMessage)
        }, 1500)
    }

    private fun markMessagesAsRead() {
        database.child("messages").child(chatId!!)
            .orderByChild("senderType")
            .equalTo("staff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (msgSnapshot in snapshot.children) {
                        msgSnapshot.ref.child("isRead").setValue(true)
                    }
                    database.child("chats").child(chatId!!).child("unreadCount").child("student").setValue(0)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        chatId?.let {
            database.child("messages").child(it).removeEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {}
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}

data class Chat(
    val chatId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val staffId: String? = null,
    val staffName: String? = null,
    val counter: String? = null,
    val status: String = "active",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastMessageBy: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val unreadCount: Map<String, Int> = mapOf(),
    val archivedByStudent: Boolean = false,
    val archivedAt: Long = 0
)

data class ChatMessage(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderType: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false
)