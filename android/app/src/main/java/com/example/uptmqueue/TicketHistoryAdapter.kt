package com.example.uptmqueue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.uptmqueue.databinding.ItemTicketHistoryBinding

class TicketHistoryAdapter(
    private var historyList: MutableList<TicketHistoryItem>
) : RecyclerView.Adapter<TicketHistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(private val binding: ItemTicketHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TicketHistoryItem) {
            binding.apply {
                tvHistoryServiceName.text = item.serviceName
                tvHistoryTicketNumber.text = "${item.ticketNumber} • ${item.date}"
                tvHistoryStatus.text = item.status.capitalize()
                tvHistoryDuration.text = "Duration: ${item.duration}"

                // Set status icon and colors based on status
                when (item.status.lowercase()) {
                    "completed" -> {
                        statusIconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            itemView.context.getColor(R.color.green_bg)
                        )
                        ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                        ivStatusIcon.setColorFilter(
                            itemView.context.getColor(R.color.green_accent),
                            android.graphics.PorterDuff.Mode.SRC_IN
                        )
                        tvHistoryStatus.setTextColor(itemView.context.getColor(R.color.green_accent))
                    }
                    "cancelled" -> {
                        statusIconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            itemView.context.getColor(R.color.gray_bg)
                        )
                        ivStatusIcon.setImageResource(R.drawable.ic_cancel_circle)
                        ivStatusIcon.setColorFilter(
                            itemView.context.getColor(R.color.gray_icon),
                            android.graphics.PorterDuff.Mode.SRC_IN
                        )
                        tvHistoryStatus.setTextColor(itemView.context.getColor(R.color.gray_icon))

                        tvHistoryDuration.text = if (item.duration == "staff") "By Staff" else "By Student"
                    }
                    else -> {
                        statusIconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            itemView.context.getColor(R.color.primary_blue_bg)
                        )
                        ivStatusIcon.setImageResource(R.drawable.ic_info)
                        ivStatusIcon.setColorFilter(
                            itemView.context.getColor(R.color.primary_blue),
                            android.graphics.PorterDuff.Mode.SRC_IN
                        )
                        tvHistoryStatus.setTextColor(itemView.context.getColor(R.color.primary_blue))
                    }
                }

                // Click listener for details
                cardHistory.setOnClickListener {
                    // TODO: Show ticket details
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemTicketHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(historyList[position])
    }

    override fun getItemCount(): Int = historyList.size

    fun updateData(newList: List<TicketHistoryItem>) {
        historyList.clear()
        historyList.addAll(newList)
        notifyDataSetChanged()
    }
}

// Data class for history items - PENTING NI!
data class TicketHistoryItem(
    val serviceName: String,
    val ticketNumber: String,
    val date: String,
    val status: String,
    val duration: String
)

// Extension function for String capitalization
fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}