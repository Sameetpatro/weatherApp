package com.example.weatherapp

import com.example.weatherapp.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call

interface WeatherServiceAPI {

    @GET("2.5/weather")
    fun getWeatherDetails(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") appID: String,
        @Query("units") metric: String

    ): Call<WeatherResponse>
}