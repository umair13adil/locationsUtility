package com.embraceit.batchdrawgeolocations.maps

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.graphics.Color
import android.os.Handler
import android.os.SystemClock
import android.view.animation.LinearInterpolator
import com.embraceit.batchdrawgeolocations.geofence.GeoFenceHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import io.reactivex.Flowable
import java.util.*


object MapHelper {

    var markerPoints: MutableList<LatLng>? = ArrayList()
    var lastMarker: Marker? = null
    var myMarker: Marker? = null

    @SuppressLint("MissingPermission")
    fun createMiniFences(actualLocation: Int, mMap: GoogleMap, resources: Resources) {

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


    }


    fun doForLocations(locations: Int, actualLocation: Int, filter: Boolean, mMap: GoogleMap, resources: Resources) {
        val swipedLocations = Arrays.asList(*resources.getStringArray(locations))
        val actualLocations = Arrays.asList(*resources.getStringArray(actualLocation))

        //Get center of locations
        val bounds = RouteHelper.getBounds(actualLocations)

        //Get max radius & then draw circle with that radius
        getMaxRadius(actualLocations, swipedLocations, bounds.center, filter, mMap)

        //Focus on that area
        val firstLatLng = actualLocations.first().split(",".toRegex())
        val cameraPosition = CameraPosition.Builder().target(LatLng(firstLatLng.first().toDouble(), firstLatLng.last().toDouble())).zoom(13f).build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    fun drawCircle(latLng: LatLng, radius: Double, title: String, snippet: String, mMap: GoogleMap) {
        mMap.addMarker(MarkerOptions().position(latLng).title(title).snippet(snippet))
        mMap.addCircle(CircleOptions()
                .center(latLng)
                .radius(radius)
                .strokeColor(Color.parseColor("#F0876E"))
                .fillColor(Color.parseColor("#FFF2EE")))
    }

    private fun drawPaths(locations: List<String>, color: Int, filter: Boolean, radius: Double, mMap: GoogleMap) {
        var firstLatLngSwiped = locations.first().split(",".toRegex())

        //Draw with Delay
        Flowable.fromIterable(locations)
                .concatMap({ s ->
                    Flowable.just<String>(s)
                    //.delay(1L, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                })
                .filter {
                    val location = it.split(",".toRegex())
                    val currentDistance = RouteHelper.distance(firstLatLngSwiped.first().toDouble(), firstLatLngSwiped.last().toDouble(), location.first().toDouble(), location.last().toDouble())!!
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
                }
                .subscribe()
    }

    fun drawRoutes(filter: Boolean, mMap: GoogleMap) {
        mMap.clear()

        val actualLocations = arrayListOf<String>()
        markerPoints?.forEach {
            actualLocations.add("${it.latitude},${it.longitude}")
        }

        if (actualLocations.isNotEmpty()) {
            //Get center of locations
            val bounds = RouteHelper.getBounds(actualLocations)

            //Get max radius & then draw circle with that radius
            val listOfDistance = arrayListOf<Double>()
            Flowable.fromIterable(actualLocations)
                    .concatMap({ s ->
                        Flowable.just<String>(s)
                    })
                    .doOnNext() {
                        val location = it.split(",".toRegex())
                        listOfDistance.add(RouteHelper.distance(location.first().toDouble(), location.last().toDouble(), bounds.center.latitude, bounds.center.longitude)!!)
                    }
                    .doOnComplete {
                        val radius = listOfDistance.max()!!.plus(50)

                        if (actualLocations.isNotEmpty()) {
                            drawPaths(actualLocations, Color.BLACK, filter, radius, mMap)
                            drawCircle(bounds.center, radius, "Center, Radius: ${radius.toInt()}", "", mMap)
                        }
                    }
                    .subscribe()
        }
    }

    private fun getMaxRadius(actualLocations: List<String>?, swipedLocations: List<String>?, centerLocation: LatLng, filter: Boolean, mMap: GoogleMap) {
        val listOfDistance = arrayListOf<Double>()
        Flowable.fromIterable(actualLocations)
                .concatMap({ s ->
                    Flowable.just<String>(s)
                })
                .doOnNext() {
                    val location = it.split(",".toRegex())
                    listOfDistance.add(RouteHelper.distance(location.first().toDouble(), location.last().toDouble(), centerLocation.latitude, centerLocation.longitude)!!)
                }
                .doOnComplete {
                    val radius = listOfDistance.max()!!.plus(50)
                    drawPaths(swipedLocations!!, Color.BLUE, filter, radius, mMap)
                    drawPaths(actualLocations!!, Color.GREEN, false, radius, mMap)
                    drawCircle(centerLocation, radius, "Center, Radius: ${radius.toInt()}", "", mMap)

                    GeoFenceHelper.addGeoFence(centerLocation, radius, mMap)
                }
                .subscribe()
    }

    fun drawMarker(latLng: LatLng, mMap: GoogleMap, activity: Activity) {
        // Adding new item to the ArrayList
        markerPoints?.add(latLng)

        // Creating MarkerOptions
        val options = MarkerOptions()

        // Setting the position of the marker
        options.position(latLng)
        options.draggable(true)
        options.icon(BitmapDescriptorFactory.fromBitmap(RouteHelper.getMarkerBitmapFromView(markerPoints?.size!!, activity)))

        // Add new marker to the Google Map Android API V2
        val marker = mMap.addMarker(options)
        marker.tag = markerPoints?.size

        //Set Latest marker to variable so it can be deleted later
        lastMarker = marker
    }

    fun drawMyMarker(latLng: LatLng, mMap: GoogleMap) {

        if (myMarker == null) {
            // Creating MarkerOptions
            val options = MarkerOptions()

            // Setting the position of the marker
            options.position(latLng)
            options.draggable(false)

            // Add new marker to the Google Map Android API V2
            myMarker = mMap.addMarker(options)
            myMarker?.tag = markerPoints?.size
        } else {
            animateMarker(myMarker!!, latLng, false, mMap)
        }

        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(16f).build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun animateMarker(marker: Marker, toPosition: LatLng, hideMarker: Boolean, mMap: GoogleMap) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val proj = mMap.projection
        val startPoint = proj.toScreenLocation(marker.position)
        val startLatLng = proj.fromScreenLocation(startPoint)
        val duration: Long = 500

        val interpolator = LinearInterpolator()

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = interpolator.getInterpolation(elapsed.toFloat() / duration)
                val lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude
                val lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude
                marker.setPosition(LatLng(lat, lng))

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                } else {
                    marker.isVisible = !hideMarker
                }
            }
        })
    }

}