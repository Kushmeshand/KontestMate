package com.example.kontestmate.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    fun getMillisFromUtc(utcTime: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.parse(utcTime)?.time ?: 0L
    }

    fun convertUtcToLocal(utcTime: String): Pair<String, String> {
        try {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            utcFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = utcFormat.parse(utcTime)

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            dateFormat.timeZone = TimeZone.getDefault()
            timeFormat.timeZone = TimeZone.getDefault()

            return Pair(
                dateFormat.format(date ?: Date()),
                timeFormat.format(date ?: Date())
            )
        } catch (e: Exception) {
            return Pair("Unknown", "Unknown")
        }
    }
}
