package com.embraceit.batchdrawgeolocations

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_maps.*
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    val TAG = MapsActivity::class.java.simpleName
    private lateinit var mMap: GoogleMap
    val locationValidator by lazy { LocationValidator() }
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

        val cameraPosition = CameraPosition.Builder().target(LatLng(55.58446, 12.304166)).zoom(16f).build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        doForLocations(R.array.locations_real_time, R.array.locations_expected, false)
    }

    private fun doForLocations(locations: Int, actualLocation: Int, filter: Boolean) {
        val swipedLocations = Arrays.asList(*resources.getStringArray(locations))
        val actualLocations = Arrays.asList(*resources.getStringArray(actualLocation))

        //Get center of locations
        val bounds = getBounds(actualLocations)

        //Get max radius & then draw circle with that radius
        getMaxRadius(actualLocations, swipedLocations, bounds.center, filter)

        //Focus on that area
        val firstLatLng = actualLocations.first().split(",".toRegex())
        val cameraPosition = CameraPosition.Builder().target(LatLng(firstLatLng.first().toDouble(), firstLatLng.last().toDouble())).zoom(13f).build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun getBounds(locations: List<String>): LatLngBounds {
        var bounds = LatLngBounds(LatLng(0.0, 0.0), LatLng(0.0, 0.0))
        try {
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

            bounds = builder.build()
        } catch (e: Exception) {

        }

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
                    Flowable.just<String>(s).delay(1L, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
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
                    Log.i(TAG, separate1.first() + " .... " + separate1.last())
                    if (color == Color.BLUE) {
                        if (locationValidator.validateNewValue(LatLng(separate1.first().toDouble(), separate1.last().toDouble()))) {
                            val lastLoc = locationValidator.lastLocation();
                            if (lastLoc != null) {
                                mMap.addPolyline(PolylineOptions()
                                        .add(LatLng(lastLoc.latitude, lastLoc.longitude),
                                                LatLng(separate1.first().toDouble(), separate1.last().toDouble()))
                                        .width(5f).color(color).geodesic(true))

                            }
                        }
                    } else {
                        mMap.addPolyline(PolylineOptions()
                                .add(LatLng(firstLatLngSwiped.first().toDouble(), firstLatLngSwiped.last().toDouble()),
                                        LatLng(separate1.first().toDouble(), separate1.last().toDouble()))
                                .width(5f).color(color).geodesic(true))

                        firstLatLngSwiped = it.split(",".toRegex())

                        //var zoomLevel = getZoomLevel(LatLng(firstLatLngSwiped.first().toDouble(), firstLatLngSwiped.last().toDouble()), mMap, 15000)
                    }
                }
                .subscribe()
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

    private fun getMaxRadius(actualLocations: List<String>?, swipedLocations: List<String>?, centerLocation: LatLng, filter: Boolean) {
        val listOfDistance = arrayListOf<Double>()
        Flowable.fromIterable(actualLocations)
                .concatMap({ s ->
                    Flowable.just<String>(s)
                })
                .doOnNext() {
                    val location = it.split(",".toRegex())
                    listOfDistance.add(distance(location.first().toDouble(), location.last().toDouble(), centerLocation.latitude, centerLocation.longitude)!!)
                }
                .doOnComplete {
                    val radius = listOfDistance.max()!!.plus(50)
                    drawPaths(swipedLocations!!, Color.BLUE, filter, radius)
                    drawPaths(actualLocations!!, Color.GREEN, false, radius)

                    txt_info?.text = "Radius: ${radius.toInt()}\nGreen: Actual Route\nBlue: Swiped Route"

                    drawCircle(centerLocation, radius, "Center, Radius: ${radius.toInt()}", "")
                }
                .subscribe()
    }
}
