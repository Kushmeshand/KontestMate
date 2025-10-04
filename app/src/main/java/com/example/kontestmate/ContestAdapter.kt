package com.example.kontestmate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.kontestmate.activities.ProblemActivity
import java.text.SimpleDateFormat
import java.util.*
import com.example.kontestmate.utils.getCorrectContestUrl

class ContestAdapter(
    private val context: Context,
    private var contests: MutableList<ContestItem> = mutableListOf()
    
) : RecyclerView.Adapter<ContestAdapter.ContestViewHolder>() {

    private val timerMap: MutableMap<Int, CountDownTimer> = mutableMapOf()

    inner class ContestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contestName: TextView = itemView.findViewById(R.id.contestName)
        val contestDate: TextView = itemView.findViewById(R.id.contestDate)
        val contestTime: TextView = itemView.findViewById(R.id.contestTime)
        val platformLogo: ImageView = itemView.findViewById(R.id.platformLogo)
        val moreInfo: TextView = itemView.findViewById(R.id.moreInfo)
        val setReminderBtn: ImageView = itemView.findViewById(R.id.reminderIcon)
        val countdownTimer: TextView = itemView.findViewById(R.id.countdownText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contest, parent, false)
        return ContestViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContestViewHolder, position: Int) {
        val contest = contests[position]

        holder.contestName.text = contest.name
        holder.contestDate.text = contest.date
        holder.contestTime.text = contest.time
        holder.platformLogo.setImageResource(contest.platformLogoResId)

        // Reminder icon state
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val isReminderSet = prefs.getBoolean("reminder_${contest.name}", false)
        contest.isReminderSet = isReminderSet
        holder.setReminderBtn.setImageResource(
            if (isReminderSet) R.drawable.bellring else R.drawable.bell
        )

        // Toggle reminder
        holder.setReminderBtn.setOnClickListener {
            val isSet = prefs.getBoolean("reminder_${contest.name}", false)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val startTime = sdf.parse(contest.startTime)?.time ?: return@setOnClickListener

            if (isSet) {
                (context as? HomeActivity)?.cancelContestReminder(contest.name, startTime)
                prefs.edit().putBoolean("reminder_${contest.name}", false).apply()
                contest.isReminderSet = false
                holder.setReminderBtn.setImageResource(R.drawable.bell)
                Toast.makeText(context, "Reminder removed for ${contest.name}", Toast.LENGTH_SHORT).show()
            } else {
                (context as? HomeActivity)?.setContestReminder(contest.name, startTime)
                prefs.edit().putBoolean("reminder_${contest.name}", true).apply()
                contest.isReminderSet = true
                holder.setReminderBtn.setImageResource(R.drawable.bellring)
                Toast.makeText(context, "Reminder set for ${contest.name}", Toast.LENGTH_SHORT).show()
            }
        }

        // Cancel any previous timer for this ViewHolder
        timerMap[holder.hashCode()]?.cancel()
        timerMap.remove(holder.hashCode())
        holder.countdownTimer.text = ""

        // Countdown logic
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val startTime = sdf.parse(contest.startTime)?.time ?: 0L
            val endTime = startTime + contest.durationSeconds * 1000
            val currentTime = System.currentTimeMillis()

            // Cancel any existing timer to prevent multiple timers on recycled views
            timerMap[holder.hashCode()]?.cancel()

            if (currentTime < startTime) {
                val millisUntilStart = startTime - currentTime
                val timer = object : CountDownTimer(millisUntilStart, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val totalSeconds = millisUntilFinished / 1000
                        val days = totalSeconds / (24 * 60 * 60)
                        val hours = (totalSeconds % (24 * 60 * 60)) / (60 * 60)
                        val minutes = (totalSeconds % (60 * 60)) / 60
                        val seconds = totalSeconds % 60

                        val timeParts = mutableListOf<String>()
                        if (days > 0) timeParts.add("${days}d")
                        if (hours > 0 || days > 0) timeParts.add("${hours}h")
                        if (minutes > 0 || hours > 0 || days > 0) timeParts.add("${minutes}m")
                        timeParts.add("${seconds}s")

                        holder.countdownTimer.text = "⏳ Starts in: ${timeParts.joinToString(" ")}"
                    }

                    override fun onFinish() {
                        holder.countdownTimer.text = "🚀 Live"
                    }
                }
                timer.start()
                timerMap[holder.hashCode()] = timer

            } else if (currentTime in startTime..endTime) {
                holder.countdownTimer.text = "🚀 Live"
            } else {
                holder.countdownTimer.text = "✅ Ended"
            }

        } catch (e: Exception) {
            holder.countdownTimer.text = ""
        }



        // Open contest URL
        holder.moreInfo.setOnClickListener {
            try {
                val correctUrl = getCorrectContestUrl(contest.platform, contest.name, contest.url)
                val intent = Intent(context, WebViewActivity::class.java).apply {
                    putExtra("url", correctUrl)
                    putExtra("platform", contest.platform)
                    putExtra("contestName", contest.name)
                }
                context.startActivity(intent)

            } catch (e: Exception) {
                Toast.makeText(context, "Invalid contest URL", Toast.LENGTH_SHORT).show()
            }
        }



        // Open Codeforces problem list
        holder.itemView.setOnClickListener {
            if (contest.platform.contains("codeforces", ignoreCase = true)) {
                val extractedId = extractCodeforcesContestId(contest.href ?: contest.url)
                Log.d("ContestAdapter", "Extracted ID: $extractedId from href=${contest.href}, url=${contest.url}")

                if (extractedId > 0) {
                    val intent = Intent(context, ProblemActivity::class.java).apply {
                        putExtra("contestId", extractedId)
                        putExtra("contestName", contest.name)
                    }
                    Log.d("ContestAdapter", "Sending contestId=$extractedId to ProblemActivity")

                    context.startActivity(intent)

                } else {
                    Toast.makeText(context, "Could not extract Codeforces contest ID", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Problems available only for Codeforces contests", Toast.LENGTH_SHORT).show()
            }
        }


    }
    private fun extractCodeforcesContestId(url: String?): Int {
        if (url.isNullOrEmpty()) return -1

        return try {
            // Match /contest(s)?/XXXX to handle both /contest/2122 and /contests/2122
            val regex = Regex("/contests?/(\\d+)")
            val match = regex.find(url)
            match?.groupValues?.get(1)?.toIntOrNull() ?: -1
        } catch (e: Exception) {
            -1
        }
    }



    override fun onViewRecycled(holder: ContestViewHolder) {
        timerMap[holder.hashCode()]?.cancel()
        timerMap.remove(holder.hashCode())
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = contests.size

    fun updateData(newData: List<ContestItem>) {
        contests.clear()
        contests.addAll(newData)
        notifyDataSetChanged()
    }
}
