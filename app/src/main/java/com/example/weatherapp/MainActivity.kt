package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
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
//import com.example.weatherapp.utils.FirestoreHelper
import com.example.weatherapp.utils.FirestoreHelper
import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.utils.Constants
import com.google.android.gms.location.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
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

    // ✅ Store user data received from FirstLaunchActivity
    private var userName: String? = null
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // ✅ Receive userName and userEmail passed from FirstLaunchActivity
        userName = intent.getStringExtra("user_name")
        userEmail = intent.getStringExtra("user_email")

        // If null, also try from SharedPreferences (backup)
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        if (userName == null) userName = prefs.getString("user_name", "Unknown User")
        if (userEmail == null) userEmail = prefs.getString("user_email", "No Email")

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        //Firebase declaration
        FirebaseApp.initializeApp(this)
        val db = FirebaseFirestore.getInstance()
        Log.d("Firestore", "Firestore Initialized: $db")

        // ✅ Update greeting message with user's name
        val greetingTextView = findViewById<TextView>(R.id.greetingText)
        greetingTextView.text = "Hey ${userName ?: "User"}, How's your day?"


        if (!isLocationEnabled()) {
            Toast.makeText(this, "Location is NOT enabled", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
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
        } else {
            Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
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

                    if (lat != null && lon != null) {
                        getLocationWeatherDetails(lat, lon)
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {

        if (!Constants.isNetworkAvailable(this)) {
            Toast.makeText(this, "Network Connection is NOT Available", Toast.LENGTH_SHORT).show()
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val serviceAPI = retrofit.create(WeatherServiceAPI::class.java)

        val call = serviceAPI.getWeatherDetails(latitude, longitude, Constants.API_KEY, Constants.METRIC_UNIT)

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val weather = response.body()!!

                    // ✅ Update UI
                    updateUI(weather)

                    // ✅ Save user + weather info to Firestore
                    saveUserWeatherData(weather, latitude, longitude)
                } else {
                    Toast.makeText(this@MainActivity, "Error Code: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "API Call Failed: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(weather: WeatherResponse) {
        val condition = weather.weather.firstOrNull()?.description ?: "N/A"
        findViewById<TextView>(R.id.conditionText).text = condition
        findViewById<TextView>(R.id.locationText).text = weather.name ?: "Unknown Location"
        findViewById<TextView>(R.id.dateText).text = convertTime(weather.dt.toLong())
        findViewById<TextView>(R.id.tempText).text = "${weather.main.temp}°C"
        findViewById<TextView>(R.id.maxTemp).text = "Max: ${weather.main.temp_max}°C"
        findViewById<TextView>(R.id.minTemp).text = "Min: ${weather.main.temp_min}°C"
        findViewById<TextView>(R.id.humidityValue).text = "${weather.main.humidity}%"
        findViewById<TextView>(R.id.pressureValue).text = "${weather.main.pressure} hPa"
        findViewById<TextView>(R.id.windValue).text = "${weather.wind.speed} m/s"
        findViewById<TextView>(R.id.sunriseTime).text = convertTime(weather.sys.sunset.toLong())
        findViewById<TextView>(R.id.sunsetTime).text = convertTime(weather.sys.sunrise.toLong())
    }

    private fun convertTime(time: Long?): String {
        if (time == null || time <= 0) return "N/A"
        return try {
            val date = Date(time * 1000L)
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            sdf.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
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

    // ✅ New function: save data to Firestore
    private fun saveUserWeatherData(weather: WeatherResponse, lat: Double, lon: Double) {
        // Create a Location object
        val location = Location("").apply {
            latitude = lat
            longitude = lon
        }

        // ✅ Save user data to Firestore
        FirestoreHelper.saveUserData(
            context = this,
            name = userName ?: "Unknown",
            email = userEmail ?: "No Email",
            location = location
        )

        // ✅ Log location updates
        FirestoreHelper.logLocationUpdate(
            email = userEmail ?: "No Email",
            location = location
        )
    }

}
