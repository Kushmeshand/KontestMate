package com.example.kontestmate

data class ContestItem(
    var id: Int,
    val platform: String,
    val platformLogoResId: Int,
    val name: String,
    val date: String,
    val time: String,
    val url: String,            // this should be used in fallback if href is null
    val startTime: String,      // for convertUtcToLocal()
    val href: String? = null,
    var isReminderSet: Boolean = false, // you can later persist this
// contest page link, may be null
    var durationSeconds: Long = 0L

)



