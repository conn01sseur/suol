package com.example.suol

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        loadData()
    }

    private fun loadData() {
        val output = findViewById<TextView>(R.id.db_output)
        output.text = "Загрузка данных с сервера..."

        Thread {
            val result = runCatching {
                ApiClient.fetchDbContent("${BuildConfig.SERVER_BASE_URL}/api/data")
            }
            runOnUiThread {
                output.text = result.fold(
                    onSuccess = { it },
                    onFailure = { "Ошибка запроса:\n${it.message}" }
                )
            }
        }.start()
    }
}