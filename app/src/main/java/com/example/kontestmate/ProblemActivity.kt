package com.example.kontestmate.activities

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kontestmate.R
import com.example.kontestmate.adapters.ProblemAdapter
import com.example.kontestmate.api.RetrofitInstance
import com.example.kontestmate.data.BookmarkDao
import com.example.kontestmate.data.BookmarkDatabase
import com.example.kontestmate.models.Problem
import com.example.kontestmate.models.toBookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProblemActivity : AppCompatActivity() {

    private lateinit var problemAdapter: ProblemAdapter
    private val problems = mutableListOf<Problem>()
    private lateinit var bookmarkDao: BookmarkDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_problem_list)

        // Initialize DB
        bookmarkDao = BookmarkDatabase.getDatabase(this).bookmarkDao()

        // Get contest name and ID
        val contestName = intent.getStringExtra("contestName")
        val contestId = intent.getIntExtra("contestId", -1)

        // Set title
        val contestTitle1 = findViewById<TextView>(R.id.contestTitle1)
        contestTitle1.text = contestName ?: "Problems"

        // Setup RecyclerView
        setupRecyclerView()

        if (contestId != -1) {
            fetchProblems(contestId)
        } else {
            Toast.makeText(this, "Invalid Contest ID!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.problemRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Setup adapter with bookmark toggle logic
        problemAdapter = ProblemAdapter(problems) { problem, isBookmarked ->
            lifecycleScope.launch(Dispatchers.IO) {
                if (isBookmarked) {
                    val bookmarkEntity = problem.toBookmarkEntity(platform = "Codeforces")
                    bookmarkDao.insertBookmark(bookmarkEntity)

                } else {
                    bookmarkDao.deleteBookmark(problem.toBookmarkEntity("Codeforces"))
                }
            }
        }

        recyclerView.adapter = problemAdapter
    }

    private fun fetchProblems(contestId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getContestProblems(contestId, 1, 1000, true)
                val apiProblems = response.result.problems
                val contestName = response.result.contest.name

                if (apiProblems.isNullOrEmpty()) {
                    Toast.makeText(this@ProblemActivity, "No problems found!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                problems.clear()
                problems.addAll(apiProblems.map {
                    val problemUrl = "https://codeforces.com/contest/$contestId/problem/${it.index}"
                    Problem(
                        index = it.index,
                        name = it.name,
                        rating = it.rating,
                        contestName = contestName,
                        platform = "Codeforces",
                        tags = it.tags,
                        url = problemUrl // ✅ now added
                    )
                })


                problemAdapter.notifyDataSetChanged()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@ProblemActivity,
                    "Failed to fetch problems!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
