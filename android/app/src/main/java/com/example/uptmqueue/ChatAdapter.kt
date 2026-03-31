package com.example.uptmqueue

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.uptmqueue.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    // ✅ Letak formatter di luar supaya objek tidak dicipta berulang kali semasa scroll (jimat memory)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val isMyMessage = message.senderId == currentUserId

            // Show/hide appropriate layouts
            if (isMyMessage) {
                binding.layoutMyMessage.visibility = View.VISIBLE
                binding.layoutOtherMessage.visibility = View.GONE

                binding.tvMyMessage.text = message.message
                binding.tvMyTime.text = formatTime(message.timestamp)

                // Show read status
                val context = binding.root.context
                if (message.isRead) {
                    binding.tvReadStatus.text = "✓✓"
                    // ✅ Guna ContextCompat untuk elak crash pada versi Android lama
                    binding.tvReadStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_light))
                } else {
                    binding.tvReadStatus.text = "✓"
                    binding.tvReadStatus.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                }

            } else {
                binding.layoutMyMessage.visibility = View.GONE
                binding.layoutOtherMessage.visibility = View.VISIBLE

                binding.tvOtherMessage.text = message.message
                binding.tvOtherTime.text = formatTime(message.timestamp)

                // ✅ Letak nama yang jelas jika ia adalah mesej auto-reply dari sistem
                if (message.senderId == "system") {
                    binding.tvSenderName.text = "Sistem Bursary"
                } else {
                    binding.tvSenderName.text = message.senderName
                }
            }
        }

        private fun formatTime(timestamp: Long): String {
            return timeFormatter.format(Date(timestamp))
        }
    }
}