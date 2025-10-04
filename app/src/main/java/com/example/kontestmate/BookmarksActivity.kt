package com.example.kontestmate.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kontestmate.BookmarkAdapter
import com.example.kontestmate.BookmarkItem
import com.example.kontestmate.DashboardActivity
import com.example.kontestmate.HomeActivity
import com.example.kontestmate.R
import com.example.kontestmate.SettingsActivity
import com.example.kontestmate.api.RetrofitInstance
import com.example.kontestmate.data.BookmarkDao
import com.example.kontestmate.data.BookmarkDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class BookmarkActivity : AppCompatActivity() {

    private lateinit var bookmarkAdapter: BookmarkAdapter
    private var allBookmarks = mutableListOf<BookmarkItem>()
    private var filteredList = mutableListOf<BookmarkItem>()
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var recyclerView: RecyclerView
    private lateinit var allButton: Button
    private lateinit var platformButton: Button
    private lateinit var contestButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)
        setupBottomNav(R.id.nav_bookmarks)

        recyclerView = findViewById(R.id.bookmarkRecyclerView)
        allButton = findViewById(R.id.filterAll)
        platformButton = findViewById(R.id.filterPlatform)
        contestButton = findViewById(R.id.filterContest)

        recyclerView.layoutManager = LinearLayoutManager(this)

        bookmarkAdapter = BookmarkAdapter(filteredList) { removed ->
            allBookmarks.remove(removed)
            filteredList.remove(removed)
            onBookmarkRemoved(removed)
            bookmarkAdapter.notifyDataSetChanged()
        }

        recyclerView.adapter = bookmarkAdapter

        allButton.setOnClickListener {
            filteredList.clear()
            filteredList.addAll(allBookmarks)
            bookmarkAdapter.notifyDataSetChanged()
        }

        platformButton.setOnClickListener {
            showPlatformFilterDialog()
        }

        contestButton.setOnClickListener {
            showContestFilterDialog()
        }

        // ✅ Initialize DAO before loading
        bookmarkDao = BookmarkDatabase.getDatabase(this).bookmarkDao()

        // ✅ Now safe to load bookmarks
        loadBookmarksFromDatabase()
    }

    private fun showPlatformFilterDialog() {
        // Create a mapping from actual platform key -> display name
        val platformMap = mapOf(
            "codeforces.com" to "Codeforces",
            "leetcode.com" to "Leetcode",
            "codechef.com" to "Codechef"
            // Add more mappings here if needed
        )

        // Get distinct platforms from your bookmarks
        val originalPlatforms = allBookmarks.map { it.platformName }.distinct()

        // Prepare display names using the map (fallback to original if not mapped)
        val displayPlatforms = originalPlatforms.map { platform ->
            platformMap[platform] ?: platform.substringBefore(".").replaceFirstChar { it.uppercase() }
        }

        // Show dialog with clean display names
        AlertDialog.Builder(this)
            .setTitle("Filter by Platform")
            .setItems(displayPlatforms.toTypedArray()) { _, which ->
                val selectedOriginal = originalPlatforms[which]  // Still use original for filtering
                filteredList.clear()
                filteredList.addAll(allBookmarks.filter {
                    it.platformName.equals(selectedOriginal, ignoreCase = true)
                })
                bookmarkAdapter.notifyDataSetChanged()
            }
            .show()
    }

    private fun showContestFilterDialog() {
        val contests = allBookmarks.map { it.contestName }.distinct().toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Filter by Contest")
            .setItems(contests) { _, which ->
                val selected = contests[which]
                filteredList.clear()
                filteredList.addAll(allBookmarks.filter {
                    it.contestName.equals(selected, ignoreCase = true)
                })
                bookmarkAdapter.notifyDataSetChanged()
            }
            .show()
    }

    private fun loadBookmarksFromDatabase() {
        lifecycleScope.launch {
            val bookmarks = bookmarkDao.getAll()
            bookmarks.forEach {
                Log.d("BookmarkCheck", "Name=${it.name}, Platform=${it.platform}, Contest=${it.contestName}, URL=${it.url}")
            }
            allBookmarks = bookmarks.map {
                BookmarkItem(
                    problemName = it.name,
                    contestName = it.contestName,
                    platformName = it.platform,
                    problemUrl = it.url, // ✅ This must be available from your model `it`
                    isBookmarked = true
                )

            }.toMutableList()
            bookmarkAdapter.updateList(allBookmarks)
        }
    }
    private fun onBookmarkRemoved(removedItem: BookmarkItem) {
        lifecycleScope.launch {
            // Map BookmarkItem → BookmarkEntity (assuming URL is unique)
            removedItem.problemUrl?.let { url ->
                bookmarkDao.deleteByUrl(url)
                Log.d("BookmarkDelete", "Deleted from DB: $url")
            }
        }
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


}
