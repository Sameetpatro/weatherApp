package com.example.weatherapp.utils

import android.content.Context
import android.location.Location
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

object FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()

    // Save user data first time
    fun saveUserData(context: Context, name: String, email: String, location: Location) {
        val userId = email.replace(".", "_")  // simple user id
        val userData = hashMapOf(
            "name" to name,
            "email" to email,
            "firstLocation" to "${location.latitude},${location.longitude}",
            "createdAt" to Date()
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                println("✅ User data saved successfully")
            }
            .addOnFailureListener {
                println("❌ Error saving user data: ${it.message}")
            }
    }

    // Save location update with time
    fun logLocationUpdate(email: String, location: Location) {
        val userId = email.replace(".", "_")
        val updateData = hashMapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "time" to Date()
        )

        db.collection("users").document(userId)
            .collection("locations")
            .add(updateData)
            .addOnSuccessListener {
                println("📍 Location logged")
            }
            .addOnFailureListener {
                println("⚠️ Failed to log location: ${it.message}")
            }
    }
}
