package com.example.kontestmate

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class BookmarkAdapter(
    private var items: MutableList<BookmarkItem>,
    private val onBookmarkRemoved: (BookmarkItem) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    inner class BookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val problemName: TextView = itemView.findViewById(R.id.problemTitle)
        val contestName: TextView = itemView.findViewById(R.id.contestName)
        val platformIcon: ImageView = itemView.findViewById(R.id.platformIcon)

        val starIcon: ImageView = itemView.findViewById(R.id.bookmarkToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.bookmark_item, parent, false)
        return BookmarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val item = items[position]

        holder.problemName.text = item.problemName

        holder.contestName.text = if (!item.contestName.isNullOrBlank()) {
            item.contestName
        } else {
            "Practice Problem"
        }



        val platform = item.platformName.lowercase()
        Log.d("BookmarkBind", "Platform: ${item.platformName}, Contest: ${item.contestName}")

// ✅ Correct platform logo assignment
        val platformKey = platform.substringBefore(".").lowercase()
        holder.platformIcon.setImageResource(
            when (platformKey) {
                "codeforces" -> R.drawable.codeforceslogo
                "leetcode" -> R.drawable.leetcode
                "codechef" -> R.drawable.codecheflogo
                else -> R.drawable.normal
            }
        )


        // Set star icon (remove option shown)
        holder.starIcon.setImageResource(R.drawable.bookmarkremove)

        // ✅ Remove logic - position safe
        holder.starIcon.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION && currentPosition < items.size) {
                val removedItem = items.removeAt(currentPosition)
                notifyItemRemoved(currentPosition)
                notifyItemRangeChanged(currentPosition, items.size)
                onBookmarkRemoved(removedItem)
                Toast.makeText(
                    holder.itemView.context,
                    "Removed from bookmarks: ${removedItem.problemName}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Open problem URL on click
        holder.itemView.setOnClickListener {
            item.problemUrl?.let { url ->
                val context = holder.itemView.context
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<BookmarkItem>) {
        items = newList.toMutableList()
        notifyDataSetChanged()
    }
}
