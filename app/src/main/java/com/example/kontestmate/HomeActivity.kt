package com.example.kontestmate

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kontestmate.activities.BookmarkActivity
import com.example.kontestmate.network.RetrofitClient
import com.example.kontestmate.network.model.CListResponse
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var upcomingTab: androidx.appcompat.widget.AppCompatTextView
    private lateinit var pastTab: androidx.appcompat.widget.AppCompatTextView
    private lateinit var contestRecyclerView: RecyclerView
    private lateinit var contestAdapter: ContestAdapter
    private var showingUpcoming = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()
        setupBottomNav()
        setupTabs()

        contestRecyclerView.layoutManager = LinearLayoutManager(this)
        contestAdapter = ContestAdapter(this, mutableListOf())
        contestRecyclerView.adapter = contestAdapter

        askNotificationPermissionIfNeeded()
        loadContests(showingUpcoming)
    }

    private fun initViews() {
        upcomingTab = findViewById(R.id.upcomingTab)
        pastTab = findViewById(R.id.pastTab)
        contestRecyclerView = findViewById(R.id.contestRecyclerView)
    }

    private fun setupTabs() {
        upcomingTab.setOnClickListener {
            if (!showingUpcoming) {
                showingUpcoming = true
                updateTabStyles()
                loadContests(true)
            }
        }

        pastTab.setOnClickListener {
            if (showingUpcoming) {
                showingUpcoming = false
                updateTabStyles()
                loadContests(false)
            }
        }
    }

    private fun updateTabStyles() {
        if (showingUpcoming) {
            upcomingTab.setTextColor(getColor(R.color.white))
            upcomingTab.setBackgroundResource(R.drawable.tab_selected)
            pastTab.setTextColor(getColor(R.color.gray))
            pastTab.setBackgroundResource(R.drawable.tab_unselected)
        } else {
            pastTab.setTextColor(getColor(R.color.white))
            pastTab.setBackgroundResource(R.drawable.tab_selected)
            upcomingTab.setTextColor(getColor(R.color.gray))
            upcomingTab.setBackgroundResource(R.drawable.tab_unselected)
        }
    }

    private fun loadContests(isUpcoming: Boolean) {
        val username = "kushmes"
        val apiKey = "8782df1654cb46a1adbd542af11069c8f64c8f7d"
        val resourceIds = listOf(1, 2, 63, 102).joinToString(",")
        val sdfUTC = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val nowUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time
        val startOfToday = startOfDay(nowUTC)
        val formattedStart = sdfUTC.format(startOfToday)


        val call = if (isUpcoming) {
            RetrofitClient.apiService.getUpcomingContests(username, apiKey, formattedStart, "start", resourceIds)
        } else {
            RetrofitClient.apiService.getPastContests(username, apiKey, formattedStart, "-end", resourceIds)
        }


        call.enqueue(object : Callback<CListResponse> {
            override fun onResponse(call: Call<CListResponse>, response: Response<CListResponse>) {
                if (response.isSuccessful) {
                    val contests = response.body()?.objects ?: emptyList()

                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val nowUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time

                    val filtered = contests.filter {
                        try {
                            val contestStart = sdf.parse(it.start)
                            val contestEnd = sdf.parse(it.end)

                            if (contestStart != null && contestEnd != null) {
                                val now = nowUTC

                                if (isUpcoming) {
                                    // Upcoming tab: Show if contest is upcoming OR currently running
                                    contestEnd.after(now)
                                } else {
                                    // Past tab: Show only if contest has ended
                                    contestEnd.before(now)
                                }
                            } else false
                        } catch (e: Exception) {
                            false
                        }
                    }


                    val mappedList = filtered.map {
                        val (date, time) = convertUtcToLocal(it.start)
                        ContestItem(
                            id = it.id,
                            platform = it.resource,
                            platformLogoResId = getLogoForSite(it.resource.lowercase(Locale.ROOT)),
                            name = it.event,
                            date = date,
                            time = time,
                            url = it.href ?: "https://clist.by",
                            startTime = it.start,
                            href = it.href
                        )
                    }

                    contestAdapter.updateData(mappedList)

                } else {
                    Log.e("API_ERROR", "Response not successful: ${response.code()}")
                    Toast.makeText(this@HomeActivity, "Failed to load contests", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CListResponse>, t: Throwable) {
                Log.e("API_ERROR", "Network call failed: ${t.message}", t)
                Toast.makeText(this@HomeActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }

        })
    }

    private fun startOfDay(date: Date): Date {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }

    private fun convertUtcToLocal(utcTime: String): Pair<String, String> {
        return try {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = utcFormat.parse(utcTime) ?: return Pair("", "")
            val localDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            val localTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
            Pair(localDate, localTime)
        } catch (e: Exception) {
            Log.e("TimeConversion", "Failed to convert time: ${e.message}")
            Pair("", "")
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun setContestReminder(contestName: String, contestStartTimeMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "Allow exact alarms in settings.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            return
        }

        // ⏰ Get selected reminder minutes (default: 10)
        val reminderMinutes = getSharedPreferences("settings", MODE_PRIVATE)
            .getInt("contest_reminder_minutes", 10)

        val reminderTimeMillis = contestStartTimeMillis - (reminderMinutes * 60 * 1000L)
        if (reminderTimeMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "Reminder time already passed", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("title", "Upcoming Contest")
            putExtra("message", "$contestName starts in $reminderMinutes minutes!")
        }

        val requestCode = contestName.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent)

        Toast.makeText(this, "Reminder set $reminderMinutes min before $contestName", Toast.LENGTH_SHORT).show()
    }

    private fun getLogoForSite(resource: String): Int {
        return when {
            "codeforces" in resource -> R.drawable.codeforceslogo
            "leetcode" in resource -> R.drawable.leetcode
            "atcoder" in resource -> R.drawable.atcoder
            "codechef" in resource -> R.drawable.codecheflogo
            else -> R.drawable.normal
        }
    }

    private fun setupBottomNav() {
        val navView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        navView.selectedItemId = R.id.nav_home

        navView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> true
                R.id.nav_bookmarks -> {
                    startActivity(Intent(this, BookmarkActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    fun cancelContestReminder(contestName: String, contestStartTimeMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = (contestStartTimeMillis / 1000).toInt()

        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        val prefs = getSharedPreferences("reminders", MODE_PRIVATE)
        prefs.edit().putBoolean("reminder_$contestName", false).apply()
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Notifications won't work without permission", Toast.LENGTH_SHORT).show()
            }
        }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
