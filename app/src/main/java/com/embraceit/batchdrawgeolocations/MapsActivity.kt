package com.embraceit.batchdrawgeolocations

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import io.reactivex.Flowable
import kotlinx.android.synthetic.main.activity_maps.*
import java.math.BigDecimal
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    val TAG = MapsActivity::class.java.simpleName
    private lateinit var mMap: GoogleMap
    var selectedCase = 0

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btn_filter.setOnClickListener {
            mMap.clear()

            when (selectedCase) {
                R.id.case1 -> {
                    doForLocations(R.array.locations_1, R.array.locations_actual_1, true)
                }
                R.id.case2 -> {
                    doForLocations(R.array.locations_2, R.array.locations_actual_2, true)
                }
                R.id.case3 -> {
                    doForLocations(R.array.locations_3, R.array.locations_actual_3, true)
                }
                R.id.case4 -> {
                    createMiniFences(R.array.locations_actual_3)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        mMap.clear()

        when (item?.itemId) {
            R.id.case1 -> {
                selectedCase = R.id.case1
                doForLocations(R.array.locations_1, R.array.locations_actual_1, false)
            }
            R.id.case2 -> {
                selectedCase = R.id.case2
                doForLocations(R.array.locations_2, R.array.locations_actual_2, false)
            }
            R.id.case3 -> {
                selectedCase = R.id.case3
                doForLocations(R.array.locations_3, R.array.locations_actual_3, false)
            }
            R.id.case4 -> {
                selectedCase = R.id.case4
                createMiniFences(R.array.locations_actual_3)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        selectedCase = R.id.case1
        doForLocations(R.array.locations_1, R.array.locations_actual_1, false)
    }

    @SuppressLint("MissingPermission")
    private fun createMiniFences(actualLocation: Int) {

        val locationsActual = Arrays.asList(*resources.getStringArray(actualLocation))

        val firstLocation = locationsActual.first().split(",".toRegex())
        val cameraPosition = CameraPosition.Builder().target(LatLng(firstLocation.first().toDouble(), firstLocation.last().toDouble())).zoom(13f).build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        Flowable.fromIterable(locationsActual)
                .concatMap({ s ->
                    Flowable.just<String>(s)
                    //.delay(1L, SECONDS, AndroidSchedulers.mainThread())
                })
                .doOnNext() {
                    val location = it.split(",".toRegex())

                    mMap.addCircle(CircleOptions()
                            .center(LatLng(location.first().toDouble(), location.last().toDouble()))
                            .radius(30.0)
                            .strokeColor(Color.parseColor("#F0876E"))
                            //.fillColor(Color.parseColor("#FFF2EE"))
                    )
                }
                .subscribe()

        /*geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences added
                // ...
            }
            addOnFailureListener {
                // Failed to add geofences
                it.printStackTrace()
            }
        }*/
    }

    private fun doForLocations(locations: Int, actualLocation: Int, filter: Boolean) {
        val swipedLocations = Arrays.asList(*resources.getStringArray(locations))
        val actualLocations = Arrays.asList(*resources.getStringArray(actualLocation))

        val actualMiles = getActualMiles(actualLocations)
        val maxMiles = getMaxMiles(actualLocations)

        val bounds = getBounds(actualLocations)

        val additionalMiles = getAdditionalMiles(actualLocations, bounds.center)
        var radius = 0.0

        if (maxMiles!! > additionalMiles!!) {
            radius = maxMiles
        } else {
            radius = maxMiles.plus(additionalMiles).plus(100)
        }

        drawPaths(swipedLocations, Color.BLUE, filter, radius)
        drawPaths(actualLocations, Color.GREEN, false, radius)

        drawCircle(bounds.center, radius, "Center, Radius: ${radius.toInt()}", "Actual Miles: ${actualMiles?.toInt()} miles, Max Miles: ${maxMiles.toInt()}")

        Log.i(TAG, "Actual Miles: $actualMiles miles,  Radius: $radius, Max Miles: $maxMiles, Additional: $additionalMiles")

        //createMiniFences(actualLocation)
    }

    private fun getBounds(locations: List<String>): LatLngBounds {

        val builder = LatLngBounds.Builder()

        locations.forEach {
            val firstLatLng = it.split(",".toRegex())

            var latitude = 0.0
            var longitude = 0.0

            latitude += firstLatLng.first().toDouble()
            longitude += firstLatLng.last().toDouble()

            builder.include(LatLng(latitude, longitude))
        }

        val tmpBounds = builder.build()
        val center = tmpBounds.center

        /** Add 2 points 1000m northEast and southWest of the center.
         * They increase the bounds only, if they are not already larger
         * than this.
         * 1000m on the diagonal translates into about 709m to each direction. */
        val northEast = move(center, 709.0, 709.0)
        val southWest = move(center, -709.0, -709.0)
        builder.include(southWest)
        builder.include(northEast)

        val bounds = builder.build()

        return bounds
    }

    private fun drawCircle(latLng: LatLng, radius: Double, title: String, snippet: String) {
        mMap.addMarker(MarkerOptions().position(latLng).title(title).snippet(snippet))
        mMap.addCircle(CircleOptions()
                .center(latLng)
                .radius(radius)
                .strokeColor(Color.parseColor("#F0876E"))
                .fillColor(Color.parseColor("#FFF2EE")))
    }

    private fun drawPaths(locations: List<String>, color: Int, filter: Boolean, radius: Double) {
        var firstLatLngSwiped = locations.first().split(",".toRegex())

        //Draw with Delay
        Flowable.fromIterable(locations)
                .concatMap({ s ->
                    Flowable.just<String>(s)
                    //.delay(1L, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                })
                .filter {
                    val location = it.split(",".toRegex())
                    val currentDistance = distance(firstLatLngSwiped.first().toDouble(), firstLatLngSwiped.last().toDouble(), location.first().toDouble(), location.last().toDouble())!!
                    if (currentDistance >= radius && filter) {
                        return@filter false
                    }
                    return@filter true
                }
                .doOnNext() {
                    val separate1 = it.split(",".toRegex())
                    //Log.i(TAG, separate1.first() + " .... " + separate1.last())

                    mMap.addPolyline(PolylineOptions()
                            .add(LatLng(firstLatLngSwiped.first().toDouble(), firstLatLngSwiped.last().toDouble()),
                                    LatLng(separate1.first().toDouble(), separate1.last().toDouble()))
                            .width(5f).color(color).geodesic(true))

                    firstLatLngSwiped = it.split(",".toRegex())

                    //var zoomLevel = getZoomLevel(LatLng(firstLatLngSwiped.first().toDouble(), firstLatLngSwiped.last().toDouble()), mMap, 15000)
                    val cameraPosition = CameraPosition.Builder().target(LatLng(firstLatLngSwiped.first().toDouble(), firstLatLngSwiped.last().toDouble())).zoom(13f).build()
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
                .subscribe()
    }

    private fun getZoomLevel(loc: LatLng, mGoogleMap: GoogleMap, radiusInMeters: Int): Float {

        val circle = mGoogleMap.addCircle(CircleOptions().center(loc).radius(radiusInMeters.toDouble()).strokeColor(Color.TRANSPARENT))
        circle.isVisible = true
        var zoomLevel = 15f

        val radius = circle.radius
        val scale = radius / 500
        zoomLevel = (16 - Math.log(scale) / Math.log(2.0)).toFloat()

        circle.remove()
        return zoomLevel - 0.5f
    }

    private fun computeCentroid(points: List<String>): LatLng {
        var latitude = 0.0
        var longitude = 0.0
        val n = points.size

        for (point in points) {
            val firstLatLng = point.split(",".toRegex())

            latitude += firstLatLng.first().toDouble()
            longitude += firstLatLng.last().toDouble()
        }

        return LatLng(latitude / n, longitude / n)
    }

    fun getActualMiles(locations: List<String>?): Double? {

        var miles = 0.0
        try {

            if (locations != null && locations.size > 1) {
                for (i in 0 until locations.size - 1) {

                    val initial = locations.get(i)
                    val dest = locations.get(i + 1)

                    if (initial != null && dest != null) {
                        try {

                            val firstLatLng = initial.split(",".toRegex())
                            val secondLatLng = dest.split(",".toRegex())

                            val lat1 = firstLatLng.first().toDouble()
                            val long1 = firstLatLng.last().toDouble()

                            val lat2 = secondLatLng.first().toDouble()
                            val long2 = secondLatLng.last().toDouble()

                            miles += distance(lat1, long1, lat2, long2)!!
                        } catch (e: Exception) {
                            //e.printStackTrace();
                        }

                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return miles
    }

    fun getMaxMiles(locations: List<String>?): Double? {

        var miles = 0.0
        try {

            if (locations != null && locations.size > 1) {

                val initial = locations.first()
                val dest = locations.last()

                if (initial != null && dest != null) {
                    try {

                        val firstLatLng = initial.split(",".toRegex())
                        val secondLatLng = dest.split(",".toRegex())

                        val lat1 = firstLatLng.first().toDouble()
                        val long1 = firstLatLng.last().toDouble()

                        val lat2 = secondLatLng.first().toDouble()
                        val long2 = secondLatLng.last().toDouble()

                        miles += distance(lat1, long1, lat2, long2)!!
                    } catch (e: Exception) {
                        //e.printStackTrace();
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return miles
    }

    fun getAdditionalMiles(locations: List<String>?, center: LatLng): Double? {

        var latitude = 0.0
        var longitude = 0.0
        var miles = 0.0
        try {

            if (locations != null && locations.size > 1) {
                try {
                    val firstLatLng = locations.first().split(",".toRegex())
                    latitude = firstLatLng.first().toDouble()
                    longitude = firstLatLng.last().toDouble()
                } catch (e: Exception) {
                    //e.printStackTrace();
                }
                miles += distance(latitude, longitude, center.latitude, center.longitude)!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return miles
    }

    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double? {

        var dist = 0.00f
        if (lat1 + lon1 > 0.0 && lat2 + lon2 > 0.0) {
            try {
                val startPoint = Location("locationA")
                startPoint.latitude = lat1
                startPoint.longitude = lon1

                val endPoint = Location("locationB")
                endPoint.latitude = lat2
                endPoint.longitude = lon2

                dist = startPoint.distanceTo(endPoint)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        return round(dist, 2).toDouble() // output distance, in meters
    }

    fun round(d: Float, decimalPlace: Int): Float {
        var bd = BigDecimal(java.lang.Float.toString(d))
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP)
        return bd.toFloat()
    }

    private val EARTHRADIUS = 6366198.0
    /**
     * Create a new LatLng which lies toNorth meters north and toEast meters
     * east of startLL
     */
    private fun move(startLL: LatLng, toNorth: Double, toEast: Double): LatLng {
        val lonDiff = meterToLongitude(toEast, startLL.latitude)
        val latDiff = meterToLatitude(toNorth)
        return LatLng(startLL.latitude + latDiff, startLL.longitude + lonDiff)
    }

    private fun meterToLongitude(meterToEast: Double, latitude: Double): Double {
        val latArc = Math.toRadians(latitude)
        val radius = Math.cos(latArc) * EARTHRADIUS
        val rad = meterToEast / radius
        return Math.toDegrees(rad)
    }

    private fun meterToLatitude(meterToNorth: Double): Double {
        val rad = meterToNorth / EARTHRADIUS
        return Math.toDegrees(rad)
    }
}
