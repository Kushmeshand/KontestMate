package com.example.kontestmate

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import java.util.*
import androidx.appcompat.widget.SwitchCompat
import com.example.kontestmate.activities.BookmarkActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.time.Duration.Companion.minutes


class SettingsActivity : AppCompatActivity() {

    private lateinit var potdSwitch: SwitchCompat
    private lateinit var contestSwitch: SwitchCompat
    private lateinit var codeforcesConnect: Button
    private lateinit var leetcodeConnect: Button
    private lateinit var codechefConnect: Button
    private lateinit var reminderButton: Button
    private lateinit var saveSettingsButton: Button

    private lateinit var sharedPrefs: SharedPreferences
    private var reminderHour: Int = -1
    private var reminderMinute: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupBottomNav(R.id.nav_settings)

        sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val leetUser = sharedPrefs.getString("leetcode_username", null)
        val cfUser = sharedPrefs.getString("codeforces_username", null)
        val ccUser = sharedPrefs.getString("codechef_username", null)

        leetUser?.let { updateSubtitle("leetcode", it) }
        cfUser?.let { updateSubtitle("codeforces", it) }
        ccUser?.let { updateSubtitle("codechef", it) }

        potdSwitch = findViewById(R.id.switch_potd)
        contestSwitch = findViewById(R.id.switch_contest)
        codeforcesConnect = findViewById(R.id.btn_codeforces)
        leetcodeConnect = findViewById(R.id.btn_leetcode)
        codechefConnect = findViewById(R.id.btn_codechef)
        reminderButton = findViewById(R.id.text_reminder_value)
        saveSettingsButton = findViewById(R.id.btn_save_settings)
        // Load saved values
        potdSwitch.isChecked = sharedPrefs.getBoolean("potd_enabled", false)
        contestSwitch.isChecked = sharedPrefs.getBoolean("contest_enabled", false)
        val savedOffset = sharedPrefs.getInt("contest_reminder_minutes", 10)
        reminderButton.text = getLabelForMinutes(savedOffset)

        potdSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit {
                putBoolean("potd_enabled", isChecked)
            }

            if (isChecked) {
                if (reminderHour >= 0 && reminderMinute >= 0) {
                    schedulePOTDNotification(reminderHour, reminderMinute)
                    Toast.makeText(this, "POTD Notifications Enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please set a reminder time first", Toast.LENGTH_SHORT).show()
                    potdSwitch.isChecked = false
                }
            } else {
                cancelPOTDNotification()
                Toast.makeText(this, "POTD Notifications Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        contestSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit {
                putBoolean("contest_enabled", isChecked)
            }
            Toast.makeText(this, "Contest Notifications ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        codeforcesConnect.setOnClickListener { connectPlatform("Codeforces") }
        leetcodeConnect.setOnClickListener { connectPlatform("LeetCode") }
        codechefConnect.setOnClickListener { connectPlatform("CodeChef") }

        reminderButton.setOnClickListener {
            showReminderOffsetDialog()
        }

        saveSettingsButton.setOnClickListener {
            sharedPrefs.edit {
                putBoolean("potd_enabled", potdSwitch.isChecked)
                putBoolean("contest_enabled", contestSwitch.isChecked)
                if (reminderHour >= 0 && reminderMinute >= 0) {
                    putInt("reminder_hour", reminderHour)
                    putInt("reminder_minute", reminderMinute)
                }
            }

            if (potdSwitch.isChecked) {
                schedulePOTDNotification(reminderHour, reminderMinute)
            }

            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectPlatform(platform: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter $platform Username")

        val input = EditText(this)
        input.hint = "$platform Username"
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val username = input.text.toString().trim()

            if (username.isNotEmpty()) {
                sharedPrefs.edit {
                    putString("${platform.lowercase()}_username", username)
                }
                Toast.makeText(this, "$platform username saved", Toast.LENGTH_SHORT).show()
                updateSubtitle(platform, username)
            } else {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
    private fun updateSubtitle(platform: String, username: String) {
        when (platform.lowercase()) {
            "leetcode" -> {
                val subtitle = findViewById<TextView>(R.id.text_leetcode_subtitle)
                subtitle.text = "Connected as @$username"
            }
            "codeforces" -> {
                val subtitle = findViewById<TextView>(R.id.text_codeforces_subtitle)
                subtitle.text = "Connected as @$username"
            }
            "codechef" -> {
                val subtitle = findViewById<TextView>(R.id.text_codechef_subtitle)
                subtitle.text = "Connected as @$username"
            }
        }
    }

    private fun showReminderOffsetDialog() {
        val options = arrayOf("10 minutes before", "20 minutes before", "30 minutes before", "45 minutes before", "1 hour before")
        val minutes = arrayOf(10, 20, 30, 45, 60)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Reminder Time")

        builder.setItems(options) { _, which ->
            val selectedOffset = minutes[which]

            sharedPrefs.edit {
                putInt("contest_reminder_minutes", selectedOffset) // ✅ use same key as HomeActivity
            }


            reminderButton.text = options[which]
            Toast.makeText(this, "Reminder set for ${options[which]}", Toast.LENGTH_SHORT).show()
        }

        builder.show()
    }


    private fun schedulePOTDNotification(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, POTDReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelPOTDNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, POTDReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }
    private fun setupBottomNav(currentItemId: Int) {
        val navView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        navView.selectedItemId = currentItemId

        navView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    if (currentItemId != R.id.nav_home) {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_dashboard -> {
                    if (currentItemId != R.id.nav_dashboard) {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_bookmarks -> {
                    if (currentItemId != R.id.nav_bookmarks) {
                        startActivity(Intent(this, BookmarkActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_settings -> {
                    if (currentItemId != R.id.nav_settings) {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }
    private fun getLabelForMinutes(minutes: Int): String {
        return when (minutes) {
            10 -> "10 minutes before"
            20 -> "20 minutes before"
            30 -> "30 minutes before"
            45 -> "45 minutes before"
            60 -> "1 hour before"
            else -> "$minutes minutes before"
        }
    }

}
