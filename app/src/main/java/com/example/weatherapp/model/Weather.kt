package com.example.weatherapp.model

import android.accessibilityservice.GestureDescription

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
) {

}
