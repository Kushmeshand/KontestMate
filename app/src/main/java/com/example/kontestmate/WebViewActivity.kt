package com.example.kontestmate

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.kontestmate.data.BookmarkDatabase
import com.example.kontestmate.models.BookmarkEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WebViewActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var bookmarkFab: FloatingActionButton

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.contestWebView)
        bookmarkFab = findViewById(R.id.bookmarkFab)

        val platform = intent.getStringExtra("platform") ?: "Unknown"
        val contestName = intent.getStringExtra("contestName") ?: "Unknown"
        val url = intent.getStringExtra("url") ?: ""

        val fixedUrl = com.example.kontestmate.utils.getCorrectContestUrl(platform, contestName, url)

        if (fixedUrl.isBlank()) {
            Toast.makeText(this, "Invalid contest URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false // Load in the same WebView
            }
        }

        webView.loadUrl(fixedUrl)

        bookmarkFab.setOnClickListener {
            val currentUrl = webView.url ?: url ?: ""
            val rawTitle = webView.title
            val title = if (!rawTitle.isNullOrBlank()) rawTitle else "Untitled Problem"

            val index = "${platform}_${contestName}_${title.hashCode()}"

            val bookmark = BookmarkEntity(
                index = index,
                name = title,
                contestName = contestName,
                rating = null,
                url = url, // ✅ pass the problem URL here
                platform = platform
            )


            lifecycleScope.launch {
                val dao = BookmarkDatabase.getDatabase(applicationContext).bookmarkDao()

                withContext(Dispatchers.IO) {
                    dao.insertBookmark(bookmark)
                }

                Toast.makeText(this@WebViewActivity, "Bookmarked: $title", Toast.LENGTH_SHORT).show()
            }
        }
        fun onBackPressed() {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                super.onBackPressed()
            }
        }


    }
}
