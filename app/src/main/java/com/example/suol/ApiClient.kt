package com.example.suol

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val TIMEOUT_MS = 10_000

    fun fetchDbContent(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) "HTTP $status\n$body" else formatResponse(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun formatResponse(body: String): String {
        val trimmed = body.trim()
        return try {
            val array = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> JSONObject(trimmed).optJSONArray("data")
                else -> null
            }

            if (array == null) {
                body
            } else {
                buildString {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        append("Запись ").append(i + 1).append('\n')
                        val keys = item.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            append("  ").append(key).append(": ").append(item.get(key)).append('\n')
                        }
                        append('\n')
                    }
                }
            }
        } catch (e: Exception) {
            "Ошибка разбора ответа:\n$body"
        }
    }
}