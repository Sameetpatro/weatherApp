package com.example.weatherapp.model

data class WeatherResponse(
    var coord: Coord,
    val weather: MutableList<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Int,
    val sys: Sys,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int

) {
}