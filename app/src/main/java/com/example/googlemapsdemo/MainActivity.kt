package com.example.googlemapsdemo

import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var currentType = "banks" // Default type

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val RADIUS = 1500
        private const val API_KEY = "AIzaSyA26wHYreDmo3EefTUbPP7gwnPBH82hdf8"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Buttons for different location types
        val btnBanks: Button = findViewById(R.id.btnBanks)
        val btnRestaurant: Button = findViewById(R.id.btnRestaurants)
        val btnHotel: Button = findViewById(R.id.btnHotels)

        btnBanks.setOnClickListener {
            currentType = "banks"  // Set the type to banks
            clearMarkers()
            fetchNearbyPlaces(lastLocation.latitude, lastLocation.longitude, currentType)
        }

        btnRestaurant.setOnClickListener {
            currentType = "restaurant"  // Set the type to restaurant
            clearMarkers()
            fetchNearbyPlaces(lastLocation.latitude, lastLocation.longitude, currentType)
        }

        btnHotel.setOnClickListener {
            currentType = "hotels"  // Set the type to hotels
            clearMarkers()
            Toast.makeText(this, "Hotels", Toast.LENGTH_SHORT).show()
            fetchNearbyPlaces(lastLocation.latitude, lastLocation.longitude, currentType)
        }
    }

    private fun placeMarkerOnMap(location: LatLng, title: String) {
        val markerOptions = MarkerOptions().position(location).title(title)
        map.addMarker(markerOptions)
    }

    private fun clearMarkers() {
        map.clear() // Clear all markers before adding new ones
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        map.isMyLocationEnabled = true
        fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng, "You are here")
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))

                // Fetch nearby places after getting current location
                fetchNearbyPlaces(location.latitude, location.longitude, currentType)
            }
        }
    }

    private fun fetchNearbyPlaces(latitude: Double, longitude: Double, type: String) {
        // Construct the Places API URL
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=$latitude,$longitude" +
                "&radius=$RADIUS" +
                "&type=$type" +
                "&key=$API_KEY"

        // Start an AsyncTask to make the network request
        GetNearbyPlacesTask().execute(url)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)
        setUpMap()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        return false
    }

    // AsyncTask to fetch the nearby places
    private inner class GetNearbyPlacesTask : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg urls: String?): String? {
            val urlString = urls[0]
            var result: String? = null

            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val inputStream = connection.inputStream
                val reader = InputStreamReader(inputStream)
                val response = reader.readText()
                result = response
            } catch (e: Exception) {
                Log.e("MapsActivity", "Error fetching nearby places", e)
            }

            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            if (result != null) {
                try {
                    val jsonObject = JSONObject(result)
                    val resultsArray = jsonObject.getJSONArray("results")

                    // Loop through the results and add markers for each place
                    for (i in 0 until resultsArray.length()) {
                        val place = resultsArray.getJSONObject(i)
                        val name = place.getString("name")
                        val lat = place.getJSONObject("geometry").getJSONObject("location").getDouble("lat")
                        val lng = place.getJSONObject("geometry").getJSONObject("location").getDouble("lng")
                        val location = LatLng(lat, lng)

                        placeMarkerOnMap(location, name)
                    }

                } catch (e: Exception) {
                    Log.e("MapsActivity", "Error parsing Places API response", e)
                }
            } else {
                Toast.makeText(this@MainActivity, "Failed to fetch nearby places", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
