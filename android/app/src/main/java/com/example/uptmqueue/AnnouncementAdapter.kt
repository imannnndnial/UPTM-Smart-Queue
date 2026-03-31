package com.example.uptmqueue

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.uptmqueue.R

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val type: String = "info" // info, warning, urgent
)

class AnnouncementAdapter(
    private val announcements: List<Announcement>
) : RecyclerView.Adapter<AnnouncementAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.announcementCard)
        val tvTitle: TextView = view.findViewById(R.id.tvAnnouncementTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvAnnouncementMessage)
        val tvTime: TextView = view.findViewById(R.id.tvAnnouncementTime)
        val colorBar: View = view.findViewById(R.id.viewColorBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_announcement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val announcement = announcements[position]

        holder.tvTitle.text = announcement.title
        holder.tvMessage.text = announcement.message
        holder.tvTime.text = getTimeAgo(announcement.timestamp)

        // Color bar based on type
        val color = when (announcement.type) {
            "urgent" -> holder.itemView.context.getColor(android.R.color.holo_red_light)
            "warning" -> holder.itemView.context.getColor(android.R.color.holo_orange_light)
            else -> holder.itemView.context.getColor(R.color.primary_blue)
        }
        holder.colorBar.setBackgroundColor(color)
    }

    override fun getItemCount() = announcements.size

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60000
        val hours = diff / 3600000
        val days = diff / 86400000

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            else -> "${days}d ago"
        }
    }
}