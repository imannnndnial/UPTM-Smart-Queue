package com.example.uptmqueue

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FAQAdapter(
    private var faqList: MutableList<FAQItem>
) : RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

    class FAQViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvFAQCategory)
        val tvQuestion: TextView = itemView.findViewById(R.id.tvFAQQuestion)
        val tvAnswer: TextView = itemView.findViewById(R.id.tvFAQAnswer)
        val ivExpand: ImageView = itemView.findViewById(R.id.ivFAQExpand)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faq, parent, false)
        return FAQViewHolder(view)
    }

    override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
        val item = faqList[position]

        // Category badge
        holder.tvCategory.text = getCategoryEmoji(item.category) + " " + item.category.uppercase()

        // Question
        holder.tvQuestion.text = item.question

        // Answer - show/hide based on expanded state
        if (item.isExpanded) {
            holder.tvAnswer.visibility = View.VISIBLE
            holder.tvAnswer.text = item.answer
            holder.ivExpand.rotation = 180f
        } else {
            holder.tvAnswer.visibility = View.GONE
            holder.ivExpand.rotation = 0f
        }

        // Click to expand/collapse
        holder.itemView.setOnClickListener {
            item.isExpanded = !item.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = faqList.size

    fun updateData(newList: List<FAQItem>) {
        faqList.clear()
        faqList.addAll(newList)
        notifyDataSetChanged()
    }

    private fun getCategoryEmoji(category: String): String {
        return when(category.lowercase()) {
            "general" -> "📘"
            "payments", "student account" -> "💳"
            "scholarship" -> "🎓"
            "queue management" -> "⏱️"
            "notifications" -> "🔔"
            "collection" -> "📦"
            "technical" -> "⚙️"
            "contact" -> "📞"
            else -> "❓"
        }
    }
}