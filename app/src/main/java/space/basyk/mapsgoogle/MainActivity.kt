package space.basyk.mapsgoogle

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.loader.content.AsyncTaskLoader
import com.github.florent37.runtimepermission.kotlin.askPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(),
        OnMapReadyCallback
//        LocationListener,
//        GoogleMap.OnCameraMoveListener,
//        GoogleMap.OnCameraMoveStartedListener,
//        GoogleMap.OnCameraIdleListener
{

    private var map: GoogleMap ?= null
    lateinit var mapView: MapView
    private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    private val DEFALT_ZOOM = 15f
    private var fusedLocationProviderClient: FusedLocationProviderClient ?= null
    lateinit var tvCurrentAddress: TextView
    lateinit var btnSearch: Button
    lateinit var tf_location: EditText


    private var endLatitude = 0.0
    private var endLongitude = 0.0
    var latitude = 0.0
    var longitude = 0.0
    private var origin: MarkerOptions ?= null
    private var destination: MarkerOptions ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.map1)
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress)
        btnSearch = findViewById(R.id.btn_search)
        tf_location = findViewById(R.id.editText)

        var mapViewBundle: Bundle ?= null
        if (savedInstanceState != null){
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        btnSearch.setOnClickListener {
        CoroutineScope(Dispatchers.Main).launch {
            searchArea()
        }
        }

    }


    private fun searchArea() {
        val location = tf_location.text.toString()
        var addressList: List<Address> ?= null
        val markerOptions = MarkerOptions()
        if (location != ""){
            val geoCoder = Geocoder(applicationContext)
            try {

                addressList = geoCoder.getFromLocationName(location, 5)
            }catch (e: IOException){
                e.printStackTrace()
            }
            if (addressList != null){
                for (i in addressList.indices){
                    val myAddress = addressList[i]
                    val latLng = LatLng(myAddress.latitude, myAddress.longitude)
                    markerOptions.position(latLng)
                    map!!.addMarker(markerOptions)
                    endLatitude = myAddress.latitude
                    endLongitude = myAddress.longitude
                    map!!.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                    val mo = MarkerOptions()
                    mo.title("Distance")
                    val results = FloatArray(10)
                    Location.distanceBetween(latitude, longitude, endLatitude, endLongitude, results)
                    val s = String.format("%.1f", results[0] / 1000)
                    origin = MarkerOptions().position(LatLng(latitude, longitude)).title("HSR layout").snippet("origin")
                    destination = MarkerOptions().position(LatLng(endLatitude, endLongitude)).title(tf_location.text.toString()).snippet("Distance = $s KM")
                    map!!.addMarker(destination)
                    map!!.addMarker(origin)
                    Toast.makeText(this, "Distance = $s KM", Toast.LENGTH_SHORT).show()
                    tvCurrentAddress.text = "Distance = $s KM"

                    
                }
            }
        }
    }




    override fun onMapReady(googleMap: GoogleMap?) {
        mapView.onResume()
        map = googleMap
        askPermissionLocation()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED){
            return
        }
        map!!.isMyLocationEnabled = true
//        map!!.setOnCameraMoveListener (this)
//        map!!.setOnCameraMoveStartedListener (this)
//        map!!.setOnCameraIdleListener (this)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        askPermissionLocation()
        var mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)
        if (mapViewBundle == null){
            mapViewBundle = Bundle()
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle)
    }

    private fun askPermissionLocation() {
        askPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ){
            getCurrentLocation()
            //mapView.getMapAsync(this)
        }.onDeclined{e ->
            if(e.hasDenied()){
                e.denied.forEach{
                }
                AlertDialog.Builder(this)
                    .setMessage("Perm")
                    .setPositiveButton("Yes"){_, _ ->
                        e.askAgain()
                    }
                    .setNegativeButton("No"){dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            if (e.hasForeverDenied()){
                e.foreverDenied.forEach {

                }
                e.goToSettings()
            }
        }
    }

    private fun getCurrentLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            @SuppressLint("MissingPermission")
            val location = fusedLocationProviderClient!!.lastLocation
            location.addOnCompleteListener(object : OnCompleteListener<Location> {
                override fun onComplete(loc: Task<Location>) {
                    if (loc.isSuccessful) {
                        val currentLocation = loc.result as Location?
                        if (currentLocation != null) {
                            moveCamera(
                                LatLng(currentLocation.latitude, currentLocation.longitude),
                                DEFALT_ZOOM
                            )
                            latitude = currentLocation.latitude
                            longitude = currentLocation.longitude
                        } else {
                            askPermissionLocation()
                            //Toast.makeText(this@MainActivity, "Not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }catch (e: Exception){
            Log.e("ASD", "Exaption")
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Float) {
        map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

//    override fun onLocationChanged(location: Location) {
//        val geocoder = Geocoder(this, Locale.getDefault())
//        var adress: List<Address> ?= null
//        try {
//            adress = geocoder.getFromLocation(location.latitude, location.longitude, 1)
//        }catch (e: IOException){
//            e.printStackTrace()
//        }
//        setAddress(adress!![0])
//    }
//
//    private fun setAddress(address: Address) {
//        if (address != null){
//            if (address.getAddressLine(0) != null){
//                tvCurrentAddress!!.text = address.getAddressLine(0)
//            }
//            if (address.getAddressLine(1) != null){
//                tvCurrentAddress!!.text = tvCurrentAddress.text.toString() + address.getAddressLine(1)
//            }
//        }
//    }
//
//    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
//
//    }
//
//    override fun onProviderEnabled(provider: String) {
//
//    }
//
//    override fun onProviderDisabled(provider: String) {
//
//    }
//
//    override fun onCameraMove() {
//
//    }
//
//    override fun onCameraMoveStarted(p0: Int) {
//
//    }
//
//    override fun onCameraIdle() {
//        var addresses: List<Address> ?= null
//        val geocoder = Geocoder(this, Locale.getDefault())
//        try {
//            addresses = geocoder.getFromLocation(map!!.cameraPosition.target.latitude, map!!.cameraPosition.target.longitude, 1)
//            setAddress(addresses!![0])
//        }catch (e:IOException){
//            e.printStackTrace()
//        }
//    }

}