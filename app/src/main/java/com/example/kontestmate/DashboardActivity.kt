package com.example.kontestmate

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Button
import androidx.lifecycle.lifecycleScope
import com.example.kontestmate.activities.BookmarkActivity
import com.example.kontestmate.api.RetrofitClientCF
import com.example.kontestmate.models.UserInfoResponse
import com.example.kontestmate.models.UserSubmissionsResponse
import com.example.kontestmate.util.HttpHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.apply
import kotlin.concurrent.thread
import org.jsoup.Jsoup
import android.widget.Button
import android.widget.EditText

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvCurrentRating: TextView
    private lateinit var tvMaxRating: TextView
    private lateinit var tvQuestionsSolved: TextView
    private lateinit var usernamesMap: Map<Platform, String>
    private lateinit var etNotes: EditText
    private lateinit var btnClearNotes: Button

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        fetchDailyTip()
        // Bind views
        tvUsername = findViewById(R.id.tvUsername)
        tvCurrentRating = findViewById(R.id.tvCurrentRating)
        tvMaxRating = findViewById(R.id.tvMaxRating)
        tvQuestionsSolved = findViewById(R.id.tvQuestionsSolved)
        val btnLeetCode = findViewById<LinearLayout>(R.id.btnLeetCode)
        val btnCodeforces = findViewById<LinearLayout>(R.id.btnCodeforces)
        val btnCodechef = findViewById<LinearLayout>(R.id.btnCodechef)

        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        // Initialize views
        etNotes = findViewById(R.id.etNotes)
        btnClearNotes = findViewById(R.id.btnClearNotes)

// Use existing prefs or create new SharedPreferences
        val notesPrefs = getSharedPreferences("NotesPrefs", MODE_PRIVATE)
        val todayKey = "notes_${getTodayDate()}"

// Load saved notes for today
        etNotes.setText(notesPrefs.getString(todayKey, ""))

// Save notes as user types
        etNotes.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                notesPrefs.edit().putString(todayKey, s.toString()).apply()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

// Clear button functionality
        btnClearNotes.setOnClickListener {
            etNotes.setText("")
            notesPrefs.edit().remove(todayKey).apply()
            Toast.makeText(this, "Notes cleared!", Toast.LENGTH_SHORT).show()
        }

        btnRefresh.setOnClickListener {
            refreshDashboard()
        }
        btnLeetCode.setOnClickListener {
            selectPlatform(Platform.LEETCODE)
        }

        btnCodeforces.setOnClickListener {
            selectPlatform(Platform.CODEFORCES)
        }

        btnCodechef.setOnClickListener {
            selectPlatform(Platform.CODECHEF)
        }

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val codeforcesUsername = prefs.getString("codeforces_username", "") ?: ""
        val leetcodeUsername = prefs.getString("leetcode_username", "") ?: ""
        val codechefUsername = prefs.getString("codechef_username", "") ?: ""

// Store for later
        usernamesMap = mapOf(
            Platform.CODEFORCES to codeforcesUsername,
            Platform.LEETCODE to leetcodeUsername,
            Platform.CODECHEF to codechefUsername
        )
        Log.d("Dashboard", "LeetCode username: ${usernamesMap[Platform.LEETCODE]}")
        Log.d("Dashboard", "CodeChef username: ${usernamesMap[Platform.CODECHEF]}")
        // Fetch Codeforces data
        selectPlatform(selectedPlatform)  // This uses the stored username
        setupBottomNav(R.id.nav_dashboard)
    }
    private enum class Platform {
        CODEFORCES, LEETCODE, CODECHEF
    }

    private var selectedPlatform: Platform = Platform.CODEFORCES
    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectPlatform(platform: Platform) {
        selectedPlatform = platform
        updateTabUI()

        val username = usernamesMap[platform]

        if (username.isNullOrBlank()) {
            tvUsername.text = "👤 Username: Not connected"
            tvCurrentRating.text = "📊 Current Rating: --"
            tvMaxRating.text = "🏆 Max Rating: --"
            tvQuestionsSolved.text = "✅ Questions Solved: --"
            Toast.makeText(this, "Please connect your ${platform.name.lowercase()} profile in Settings", Toast.LENGTH_SHORT).show()
        } else {
            when (platform) {
                Platform.CODEFORCES -> fetchCodeforcesData(username)
                Platform.LEETCODE -> fetchLeetCodeData(username)
                Platform.CODECHEF -> fetchCodeChefData(username)
            }
        }

        // POTD
        when (platform) {
            Platform.LEETCODE -> fetchLeetCodePOTD()
            Platform.CODEFORCES -> fetchCodeforcesPOTD()
            Platform.CODECHEF -> fetchCodeChefPOTD()
        }
    }


    private fun getTodayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }


    private fun fetchLeetCodeData(username: String) {
        Log.d("API_CALL", "Fetching LeetCode data for $username")
        val url = "https://competeapi.vercel.app/user/leetcode/$username"
        HttpHelper.getJson(url) { json ->
            runOnUiThread {
                if (json != null) {
                    val data = json.optJSONObject("data")
                    val matchedUser = data?.optJSONObject("matchedUser")
                    val submitStats = matchedUser?.optJSONObject("submitStats")
                    val acSubmissionNum = submitStats?.optJSONArray("acSubmissionNum")
                    var totalSolved = 0
                    if (acSubmissionNum != null) {
                        for (i in 0 until acSubmissionNum.length()) {
                            val item = acSubmissionNum.optJSONObject(i)
                            if (item.optString("difficulty") == "All") {
                                totalSolved = item.optInt("count")
                                break
                            }
                        }
                    }

                    val contestRanking = data.optJSONObject("userContestRanking")
                    val rating = contestRanking?.optDouble("rating", 0.0) ?: 0.0
                    val contests = contestRanking?.optInt("attendedContestsCount", 0) ?: 0

                    tvUsername.text = "👤 Username: $username"
                    tvCurrentRating.text = "📊 Rating: ${rating.toInt()}"
                    tvMaxRating.text = "🏆 Contests Attended: $contests"
                    tvQuestionsSolved.text = "✅ Questions Solved: $totalSolved"
                } else {
                    Toast.makeText(this, "Failed to load LeetCode data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun fetchCodeChefData(username: String) {
        Log.d("API_CALL", "Fetching CodeChef data for $username")
        val url = "https://competeapi.vercel.app/user/codechef/$username"

        HttpHelper.getJson(url) { json ->
            runOnUiThread {
                if (json != null) {
                    Log.d("API_RESPONSE", "CodeChef JSON: $json")

                    try {
                        val currentRating = json.optString("rating", "N/A")
                        val ratingNumber = json.optInt("rating_number", 0)
                        val maxRank = json.optInt("max_rank", 0)

                        // problemsSolved not available, so we skip or mark as N/A
                        tvUsername.text = "👤 Username: $username"
                        tvCurrentRating.text = "📊 Current Rating: $currentRating ($ratingNumber)"
                        tvMaxRating.text = "🏆 Max Rank: $maxRank"
                        tvQuestionsSolved.text = "✅ Questions Solved: N/A"
                    } catch (e: Exception) {
                        Log.e("PARSE_ERROR", "Error parsing CodeChef data: ${e.message}")
                        Toast.makeText(this, "Error loading CodeChef data", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this, "Failed to load CodeChef data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDailyTip() {
        val tvDailyTip = findViewById<TextView>(R.id.tvDailyTip)

        val dsaTips = listOf(
            "Use a HashMap when you need to count frequencies or track occurrences efficiently.",
            "When you see 'k largest/smallest elements', think of using a Heap (PriorityQueue).",
            "Two pointers are perfect for sorted arrays or linked lists — saves O(n²) loops.",
            "Prefix sums help answer range sum queries in O(1) after O(n) preprocessing.",
            "Use a Stack for problems involving 'next greater element' or balanced parentheses.",
            "Sliding window is ideal for subarray problems involving size or sum constraints.",
            "Binary Search isn’t just for arrays — apply it on answer range when conditions are monotonic.",
            "When traversing trees, remember: DFS = recursion or stack, BFS = queue.",
            "In graphs, use BFS for shortest path in unweighted graphs, Dijkstra for weighted graphs.",
            "Use Union-Find (Disjoint Set) for connectivity problems like 'connected components'.",
            "Dynamic Programming = recursion + memoization or iterative table-filling.",
            "Try sorting before brute forcing — it often simplifies logic and reduces complexity.",
            "If problem involves intervals, sort by start/end and use greedy techniques.",
            "When asked for combinations or subsets, think backtracking.",
            "Use modulo operations carefully — especially in DP problems to prevent overflow.",
            "In DP, define states clearly: identify what changes at each step.",
            "When merging intervals, always sort by start and track overlapping ranges.",
            "For matrix traversal, predefine directions array like [(1,0),(-1,0),(0,1),(0,-1)].",
            "Always check edge cases: empty array, single element, large inputs.",
            "If you see recursion depth issues, convert to iterative using your own stack.",
            "When you see repetitive subproblems, it’s a DP hint.",
            "Greedy works only if local optimal choices lead to global optimum — test this first.",
            "For bit manipulation, remember: x & (x - 1) clears the lowest set bit.",
            "Use counting sort when range of numbers is small compared to n.",
            "For tree diameter problems, two BFS/DFS passes solve it easily.",
            "When using HashMaps in Kotlin, prefer getOrDefault() or computeIfAbsent().",
            "Sort characters and compare for anagram problems instead of checking each manually.",
            "For longest substring without repeating characters — use a HashSet with sliding window.",
            "Always visualize recursion trees for complex backtracking problems.",
            "Remember: optimize space in DP by only keeping current and previous rows when possible."
        )

        // Pick a random tip every time
        val randomTip = dsaTips.random()

        tvDailyTip.text = "💡 DSA Tip: $randomTip"
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshDashboard() {
        // Reload user stats for the currently selected platform
        selectPlatform(selectedPlatform)

        // Refresh LeetCode Problem of the Day
        fetchLeetCodePOTD()

        // Refresh CodeChef Problem of the Day
        fetchCodeChefPOTD()

        // Refresh the daily DSA tip
        fetchDailyTip()

        Toast.makeText(this, "Dashboard refreshed!", Toast.LENGTH_SHORT).show()
    }
    private fun fetchCodeforcesPOTD() {
        val tvTitle = findViewById<TextView>(R.id.tvPotdTitle)
        val tvSummary = findViewById<TextView>(R.id.tvPotdSummary)
        val tvHint = findViewById<TextView>(R.id.tvPotdHint)

        val url = "https://codeforces.com/api/contest.list?gym=false"

        HttpHelper.getJson(url) { json ->
            runOnUiThread {
                if (json != null) {
                    try {
                        val contests = json.optJSONArray("result")
                        if (contests != null && contests.length() > 0) {
                            val lastContest = contests.getJSONObject(0)
                            val contestId = lastContest.optInt("id")
                            val contestName = lastContest.optString("name", "Codeforces Contest")

                            // Pick a random problem A-E
                            val problemIndex = ('A'..'E').random()

                            tvTitle.text = "📌 POTD: Problem $problemIndex"
                            tvSummary.text = "Contest: $contestName"

                            val link = "https://codeforces.com/contest/$contestId/problem/$problemIndex"
                            tvHint.text = "🔗 Open Problem"
                            tvHint.setOnClickListener {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(link)
                                startActivity(intent)
                            }
                        } else {
                            tvTitle.text = "No contests found"
                            tvSummary.text = ""
                            tvHint.text = ""
                        }
                    } catch (e: Exception) {
                        tvTitle.text = "Error loading POTD."
                        tvSummary.text = ""
                        tvHint.text = ""
                        e.printStackTrace()
                    }
                } else {
                    tvTitle.text = "Error loading POTD."
                    tvSummary.text = ""
                    tvHint.text = ""
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchCodeChefPOTD() {
        // 40 hardcoded CodeChef problems with URLs
        val problems = listOf(
            "FLOW001" to "https://www.codechef.com/problems/FLOW001",
            "START01" to "https://www.codechef.com/problems/START01",
            "HS08TEST" to "https://www.codechef.com/problems/HS08TEST",
            "INTEST" to "https://www.codechef.com/problems/INTEST",
            "TSORT" to "https://www.codechef.com/problems/TSORT",
            "FRUITS" to "https://www.codechef.com/problems/FRUITS",
            "LAPIN" to "https://www.codechef.com/problems/LAPIN",
            "RECIPE" to "https://www.codechef.com/problems/RECIPE",
            "PALL01" to "https://www.codechef.com/problems/PALL01",
            "FLOW007" to "https://www.codechef.com/problems/FLOW007",
            "AMR15A" to "https://www.codechef.com/problems/AMR15A",
            "POTHOLE" to "https://www.codechef.com/problems/POTHOLE",
            "FLOW006" to "https://www.codechef.com/problems/FLOW006",
            "TRICOIN" to "https://www.codechef.com/problems/TRICOIN",
            "CHEFROUT" to "https://www.codechef.com/problems/CHEFROUT",
            "EKO" to "https://www.codechef.com/problems/EKO",
            "PRIME1" to "https://www.codechef.com/problems/PRIME1",
            "ARITH2" to "https://www.codechef.com/problems/ARITH2",
            "BITMAP" to "https://www.codechef.com/problems/BITMAP",
            "COINS" to "https://www.codechef.com/problems/COINS",
            "GCDQ" to "https://www.codechef.com/problems/GCDQ",
            "QTREE" to "https://www.codechef.com/problems/QTREE",
            "FLOW013" to "https://www.codechef.com/problems/FLOW013",
            "CHEFSTLT" to "https://www.codechef.com/problems/CHEFSTLT",
            "FRK" to "https://www.codechef.com/problems/FRK",
            "CSUM" to "https://www.codechef.com/problems/CSUM",
            "ARRAYSUB" to "https://www.codechef.com/problems/ARRAYSUB",
            "MULTHREE" to "https://www.codechef.com/problems/MULTHREE",
            "SMALLFACT" to "https://www.codechef.com/problems/SMALLFACT",
            "SUBS" to "https://www.codechef.com/problems/SUBS",
            "FLOW008" to "https://www.codechef.com/problems/FLOW008",
            "MUL" to "https://www.codechef.com/problems/MUL",
            "ADADISH" to "https://www.codechef.com/problems/ADADISH",
            "CHEFPAPA" to "https://www.codechef.com/problems/CHEFPAPA",
            "SMIL" to "https://www.codechef.com/problems/SMIL",
            "NUMGAME1" to "https://www.codechef.com/problems/NUMGAME1",
            "BIGSALE" to "https://www.codechef.com/problems/BIGSALE",
            "GROWING" to "https://www.codechef.com/problems/GROWING",
            "CLICKS" to "https://www.codechef.com/problems/CLICKS",
            "FRUITBAT" to "https://www.codechef.com/problems/FRUITBAT"
        )

        // Pick a random problem based on today's date (same problem for the day)
        val today = java.time.LocalDate.now().toEpochDay().toInt()
        val randomIndex = today % problems.size
        val (title, link) = problems[randomIndex]

        val tvPotd = findViewById<TextView>(R.id.tvPotdTitle) // replace with your actual POTD TextView
        val tvSummary = findViewById<TextView>(R.id.tvPotdSummary)
        val tvHint = findViewById<TextView>(R.id.tvPotdHint)

        tvPotd.text = "🔥 CodeChef POTD: $title"
        tvSummary.text = "Solve this problem today!"

        tvHint.text = "🔗 Open Problem"
        tvHint.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            startActivity(intent)
        }
    }



    private fun fetchLeetCodePOTD() {
        val tvTitle = findViewById<TextView>(R.id.tvPotdTitle)
        val tvSummary = findViewById<TextView>(R.id.tvPotdSummary)
        val tvHint = findViewById<TextView>(R.id.tvPotdHint)

        val url = "https://alfa-leetcode-api.onrender.com/daily"
        Log.d("LeetCodePOTD", "Fetching LeetCode POTD from URL: $url")

        HttpHelper.getJson(url) { json ->
            runOnUiThread {
                if (json != null) {
                    Log.d("LeetCodePOTD", "Response JSON: $json")
                    try {
                        // JSON keys are at top level, no 'question' object
                        val title = json.optString("questionTitle", "Unknown Problem")
                        val link = json.optString("questionLink", "")
                        val difficulty = json.optString("difficulty", "--")
                        val tagsArray = json.optJSONArray("topicTags")
                        val tags = (0 until (tagsArray?.length() ?: 0)).joinToString {
                            tagsArray?.optJSONObject(it)?.optString("name") ?: ""
                        }

                        Log.d("LeetCodePOTD", "Title: $title, Link: $link, Difficulty: $difficulty, Tags: $tags")

                        tvTitle.text = "🔥 LeetCode POTD: $title"
                        tvSummary.text = "Difficulty: $difficulty | Tags: $tags"

                        if (!link.isNullOrEmpty()) {
                            tvHint.text = "🔗 Open Problem"
                            tvHint.setOnClickListener {
                                Log.d("LeetCodePOTD", "Opening link: $link")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                startActivity(intent)
                            }
                        } else {
                            Log.w("LeetCodePOTD", "No link found for this problem")
                            tvHint.text = ""
                        }

                    } catch (e: Exception) {
                        Log.e("LeetCodePOTD", "Exception while parsing JSON", e)
                        tvTitle.text = "Error loading POTD."
                        tvSummary.text = ""
                        tvHint.text = ""
                    }
                } else {
                    Log.e("LeetCodePOTD", "Received null JSON from API")
                    tvTitle.text = "Error loading POTD."
                    tvSummary.text = ""
                    tvHint.text = ""
                }
            }
        }
    }

    private fun updateTabUI() {
        val selectedBg = R.drawable.tab_selected
        val unselectedBg = R.drawable.tab_unselected
        val selectedColor = resources.getColor(android.R.color.white)
        val unselectedColor = resources.getColor(android.R.color.darker_gray)

        // Codeforces
        val btnCodeforces = findViewById<LinearLayout>(R.id.btnCodeforces)
        val codeforcesText = btnCodeforces.findViewById<TextView>(R.id.textCodeforces)
        btnCodeforces.setBackgroundResource(if (selectedPlatform == Platform.CODEFORCES) selectedBg else unselectedBg)
        codeforcesText.setTextColor(if (selectedPlatform == Platform.CODEFORCES) selectedColor else unselectedColor)

// LeetCode
        val btnLeetCode = findViewById<LinearLayout>(R.id.btnLeetCode)
        val leetCodeText = btnLeetCode.findViewById<TextView>(R.id.textleetcode)
        btnLeetCode.setBackgroundResource(if (selectedPlatform == Platform.LEETCODE) selectedBg else unselectedBg)
        leetCodeText.setTextColor(if (selectedPlatform == Platform.LEETCODE) selectedColor else unselectedColor)

// CodeChef
        val btnCodechef = findViewById<LinearLayout>(R.id.btnCodechef)
        val codechefText = btnCodechef.findViewById<TextView>(R.id.textCodechef)
        btnCodechef.setBackgroundResource(if (selectedPlatform == Platform.CODECHEF) selectedBg else unselectedBg)
        codechefText.setTextColor(if (selectedPlatform == Platform.CODECHEF) selectedColor else unselectedColor)

    }
    private fun loadLeetCodeDataPlaceholder() {
        tvUsername.text = "👤 Username: leet_user"
        tvCurrentRating.text = "📊 Current Rating: N/A"
        tvMaxRating.text = "🏆 Max Rating: N/A"
        tvQuestionsSolved.text = "✅ Questions Solved: --"
    }

    private fun loadCodeChefDataPlaceholder() {
        tvUsername.text = "👤 Username: chef_user"
        tvCurrentRating.text = "📊 Current Rating: --"
        tvMaxRating.text = "🏆 Max Rating: --"
        tvQuestionsSolved.text = "✅ Questions Solved: --"
    }

    private fun fetchCodeforcesData(username: String) {
        // Get user info
        RetrofitClientCF.api.getUserInfo(username).enqueue(object : Callback<UserInfoResponse> {
            override fun onResponse(call: Call<UserInfoResponse>, response: Response<UserInfoResponse>) {
                if (response.isSuccessful && response.body()?.status == "OK") {
                    val user = response.body()?.result?.first()
                    tvUsername.text = "👤 Username: ${user?.handle}"
                    tvCurrentRating.text = "📊 Current Rating: ${user?.rating ?: "--"}"
                    tvMaxRating.text = "🏆 Max Rating: ${user?.maxRating ?: "--"}"
                } else {
                    Toast.makeText(this@DashboardActivity, "Failed to load user info", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                Toast.makeText(this@DashboardActivity, "Network error while loading user info", Toast.LENGTH_SHORT).show()
            }
        })

        // Get submissions for solved count
        RetrofitClientCF.api.getUserStatus(username).enqueue(object : Callback<UserSubmissionsResponse> {
            override fun onResponse(call: Call<UserSubmissionsResponse>, response: Response<UserSubmissionsResponse>) {
                if (response.isSuccessful && response.body()?.status == "OK") {
                    val solvedSet = mutableSetOf<String>()
                    response.body()?.result?.forEach { submission ->
                        if (submission.verdict == "OK") {
                            val problemId = "${submission.problem.contestId}-${submission.problem.index}"
                            solvedSet.add(problemId)
                        }
                    }
                    tvQuestionsSolved.text = "✅ Questions Solved: ${solvedSet.size}"
                } else {
                    Toast.makeText(this@DashboardActivity, "Failed to load submissions", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserSubmissionsResponse>, t: Throwable) {
                Toast.makeText(this@DashboardActivity, "Network error while loading submissions", Toast.LENGTH_SHORT).show()
            }
        })
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
                R.id.nav_dashboard -> true
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
}
