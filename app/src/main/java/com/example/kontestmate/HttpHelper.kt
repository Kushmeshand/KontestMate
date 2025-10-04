package com.example.kontestmate.util

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object HttpHelper {
    fun getJson(url: String, callback: (JSONObject?) -> Unit) {
        thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val response = conn.inputStream.bufferedReader().readText()
                callback(JSONObject(response))
            } catch (e: Exception) {
                Log.e("HttpHelper", "Error fetching JSON: $e")
                callback(null)
            }
        }
    }
    suspend fun postJson(url: String, jsonBody: JSONObject, callback: (JSONObject?) -> Unit) {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }

            conn.outputStream.use { it.write(jsonBody.toString().toByteArray()) }

            val response = conn.inputStream.bufferedReader().readText()
            callback(JSONObject(response))
        } catch (e: Exception) {
            Log.e("HttpHelper", "Error posting JSON: $e")
            callback(null)
        }
    }

}
