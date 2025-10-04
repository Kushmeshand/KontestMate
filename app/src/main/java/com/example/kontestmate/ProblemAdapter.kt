package com.example.kontestmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kontestmate.R
import com.example.kontestmate.models.Problem

class ProblemAdapter(
    private val problems: List<Problem>,
    private val onBookmarkToggle: (Problem, Boolean) -> Unit // pass Problem + new state
) : RecyclerView.Adapter<ProblemAdapter.ProblemViewHolder>() {

    class ProblemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.problemName1)
        val difficulty: TextView = view.findViewById(R.id.problemDifficulty1)
        val problemTags: TextView = view.findViewById(R.id.problemTags)
        val bookmarkIcon: ImageView = view.findViewById(R.id.bookmarkIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProblemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.problem_item_layout, parent, false)
        return ProblemViewHolder(view)
    }

    override fun getItemCount(): Int = problems.size

    override fun onBindViewHolder(holder: ProblemViewHolder, position: Int) {
        val problem = problems[position]
        holder.name.text = "${problem.index}. ${problem.name}"
        holder.difficulty.text = problem.rating?.let { "★ $it" } ?: "★ -"

        holder.problemTags.text = if (problem.tags.isNullOrEmpty()) {
            "Tags: N/A"
        } else {
            "Tags: ${problem.tags.joinToString(", ")}"
        }

        holder.bookmarkIcon.setImageResource(
            if (problem.isBookmarked) R.drawable.bookmarkremove else R.drawable.bookmark
        )

        holder.bookmarkIcon.setOnClickListener {
            val newBookmarkState = !problem.isBookmarked
            problem.isBookmarked = newBookmarkState
            onBookmarkToggle(problem, newBookmarkState) // callback to Activity/Fragment
            notifyItemChanged(position)
        }
    }
}
