package com.example.suol

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var descInput: EditText
    private lateinit var addButton: Button
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nameInput = findViewById(R.id.name_input)
        descInput = findViewById(R.id.desc_input)
        addButton = findViewById(R.id.add_button)
        output = findViewById(R.id.db_output)

        addButton.setOnClickListener { addItem() }
        loadData()
    }

    private fun addItem() {
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            output.text = "Введите имя"
            return
        }
        val description = descInput.text.toString().trim()

        addButton.isEnabled = false
        output.text = "Отправка на сервер..."
        Thread {
            val result = runCatching {
                ApiClient.postItem("${BuildConfig.SERVER_BASE_URL}/api/data", name, description)
            }
            runOnUiThread {
                addButton.isEnabled = true
                nameInput.text.clear()
                descInput.text.clear()
                output.text = result.fold(
                    onSuccess = { "Добавлено: $it\n" },
                    onFailure = { "Ошибка добавления:\n${it.message}\n" }
                )
                loadData()
            }
        }.start()
    }

    private fun loadData() {
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