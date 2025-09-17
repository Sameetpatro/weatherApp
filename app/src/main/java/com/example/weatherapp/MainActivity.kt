package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.utils.Constants
import com.google.android.gms.location.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private val REQUEST_LOCATION_CODE = 200
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Log.d("LOCATION_FLOW", "onCreate started")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()) {
//            Log.e("LOCATION_FLOW", "Location services are disabled")
            Toast.makeText(this, "Location is NOT enabled", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
//            Log.d("LOCATION_FLOW", "Location services enabled, requesting permission")
            requestPermission()
        }
    }


    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            showRequestDialog()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_LOCATION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            requestLocationData()
//            Log.d("PERMISSION_DEBUG", "Request code: $requestCode, Result: ${grantResults.joinToString()}")

        } else {
            Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
//        Log.d("LOCATION_FLOW", "requestLocationData() called")

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
//            Log.e("LOCATION_FLOW", "No permission to access location")
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        mFusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    val lat = p0.lastLocation?.latitude
                    val lon = p0.lastLocation?.longitude

//                    Log.d("LOCATION_FLOW", "Location callback triggered. Lat: $lat, Lon: $lon")
//                    Log.d("LOCATION_CHECK", "Lat: $lat, Lon: $lon")

                    if (lat != null && lon != null) {
//                        Log.d("API_DEBUG", "Calling weather API with lat: $lat, lon: $lon")
                        getLocationWeatherDetails(lat, lon)
                    } else {
//                        Log.e("LOCATION_FLOW", "Location is null")
                    }
                }
            },
            Looper.getMainLooper()
        )
    }


    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (!Constants.isNetworkAvailable(this)) {
            Toast.makeText(this, "Network Connection is NOT Available", Toast.LENGTH_SHORT).show()
//            Log.e("API_ERROR", "No network connection.")
            return
        }

        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val serviceAPI = retrofit.create(WeatherServiceAPI::class.java)

//        Log.d("API_DEBUG", "Requesting weather for lat: $latitude, lon: $longitude")

        val call = serviceAPI.getWeatherDetails(latitude, longitude, Constants.API_KEY, Constants.METRIC_UNIT)

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val weather = response.body()!!

                    // Log entire response to see raw data
//                    Log.d("API_RESPONSE", "Raw weather data: ${weather}")

                    updateUI(weather)
                } else {
                    val errorBody = response.errorBody()?.string()
//                    Log.e("API_ERROR", "Code: ${response.code()}, Error: $errorBody")
                    Toast.makeText(this@MainActivity, "Error Code: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }


            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
//                Log.e("API_FAILURE", "Error: ${t.message}", t)
                Toast.makeText(this@MainActivity, "API Call Failed: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }


    @SuppressLint("SetTextI18n")
    private fun updateUI(weather: WeatherResponse) {
        // Weather condition (clear sky, rain, etc.)
        val condition = weather.weather.firstOrNull()?.description ?: "N/A"
        findViewById<TextView>(R.id.conditionText).text = condition

        // Location (City name)
        findViewById<TextView>(R.id.locationText).text = weather.name ?: "Unknown Location"

        // Date
        findViewById<TextView>(R.id.dateText).text = convertTime(weather.dt.toLong())

        // Temperatures
        findViewById<TextView>(R.id.tempText).text = "${weather.main.temp}°C"
        findViewById<TextView>(R.id.maxTemp).text = "Max: ${weather.main.temp_max}°C"
        findViewById<TextView>(R.id.minTemp).text = "Min: ${weather.main.temp_min}°C"

        // Humidity, Pressure, Wind
        findViewById<TextView>(R.id.humidityValue).text = "${weather.main.humidity}%"
        findViewById<TextView>(R.id.pressureValue).text = "${weather.main.pressure} hPa"
        findViewById<TextView>(R.id.windValue).text = "${weather.wind.speed} m/s"

        // Sunrise & Sunset
        findViewById<TextView>(R.id.sunriseTime).text = convertTime(weather.sys.sunset.toLong())
        findViewById<TextView>(R.id.sunsetTime).text = convertTime(weather.sys.sunrise.toLong())

    }


    private fun convertTime(time: Long?): String {
        if (time == null || time <= 0) return "N/A"  // 🚨 Avoid crashing

        return try {
            val date = Date(time * 1000L) // API gives seconds, we need milliseconds
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            sdf.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
//            Log.e("TIME_CONVERT", "Error converting time: ${e.message}")
            "N/A"
        }
    }



    private fun showRequestDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Needed")
            .setMessage("This permission is needed for accessing location. Please enable it in settings.")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("CLOSE") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}


