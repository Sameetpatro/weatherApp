package com.example.weatherapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FirstLaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_launch)

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        btnContinue.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please enter name and email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Save locally in SharedPreferences
            val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("user_name", name)
                .putString("user_email", email)
                .putBoolean("first_run_done", true)
                .apply()

            // ✅ Show consent message
            Toast.makeText(this, "You will receive weather updates on $email", Toast.LENGTH_LONG).show()

            // ✅ Pass both name & email to MainActivity via Intent extras
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("user_name", name)
                putExtra("user_email", email)
            }

            startActivity(intent)
            finish() // close first screen
        }
    }
}
