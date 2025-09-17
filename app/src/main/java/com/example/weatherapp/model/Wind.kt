package com.example.weatherapp.model

data class Wind(
    val speed: Double,
    val deg: Int,
    val gust: Double? = null
) {

}
