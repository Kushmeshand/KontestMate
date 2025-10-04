package com.example.kontestmate

import android.content.Context
import android.content.SharedPreferences

class SettingsPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    fun setSwitchState(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getSwitchState(key: String): Boolean {
        return prefs.getBoolean(key, false)
    }

    fun savePlatformEmail(platform: String, email: String) {
        prefs.edit().putString(platform, email).apply()
    }

    fun getPlatformEmail(platform: String): String? {
        return prefs.getString(platform, null)
    }

    fun saveReminderTime(hour: Int, minute: Int) {
        prefs.edit().putInt("reminder_hour", hour).apply()
        prefs.edit().putInt("reminder_minute", minute).apply()
    }

    fun getReminderHour(): Int = prefs.getInt("reminder_hour", 9)
    fun getReminderMinute(): Int = prefs.getInt("reminder_minute", 0)
}