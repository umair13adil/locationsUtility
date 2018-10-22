package com.embraceit.batchdrawgeolocations

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.embraceit.batchdrawgeolocations.geofence.GeoFenceHelper
import com.embraceit.batchdrawgeolocations.geofence.GeofenceTransitionsIntentService
import com.embraceit.batchdrawgeolocations.maps.MapHelper
import com.embraceit.batchdrawgeolocations.maps.PolygonHelper
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.michaelflisar.rxbus2.RxBusBuilder
import com.michaelflisar.rxbus2.rx.RxBusMode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_fence.*

class GeoFenceActivity : BaseActivity(), OnMapReadyCallback {

    val TAG = GeoFenceActivity::class.java.simpleName
    private lateinit var mMap: GoogleMap

    lateinit var geofencingClient: GeofencingClient

    companion object {
        private var fenceCreated = false
    }

    //GeoFenceIDs
    val ID_CURRENT_FENCE = "MyLocation"
    val ID_POLY1_FENCE = "Polygon1"
    val ID_POLY2_FENCE = "Polygon2"

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fence)

        //Remove geofences if any
        GeoFenceHelper.stop(this, arrayListOf(ID_CURRENT_FENCE, ID_POLY1_FENCE, ID_POLY2_FENCE))

        geofencingClient = GeoFenceHelper.init(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        RxBusBuilder.create(String::class.java)
                .withMode(RxBusMode.Main)
                .subscribe { data ->
                    Toast.makeText(this, data, Toast.LENGTH_LONG).show()

                    if (data.contains("Entered")) {
                        txt_fence_1.setTextColor(ContextCompat.getColor(this, R.color.colorGreen))
                    } else if (data.contains("Exited")) {
                        txt_fence_1.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
                    } else if (data.contains("Dwelled")) {
                        txt_fence_1.setTextColor(ContextCompat.getColor(this, R.color.colorGreen))
                    }
                    txt_fence_1.text = data
                }

        RxBusBuilder.create(Location::class.java)
                .withMode(RxBusMode.Main)
                .subscribe { data ->
                    val currentLocation = LatLng(data.latitude, data.longitude)
                    MapHelper.drawMyMarker(currentLocation, mMap)
                    MapHelper.moveCamera(currentLocation, mMap)

                    Toast.makeText(this, "Current Location: ${data.latitude},${data.longitude}", Toast.LENGTH_SHORT).show()

                    doOnFirstLocationUpdate(currentLocation)

                    val polyVerify = PolygonHelper.isInsidePolygon(currentLocation)

                    if (polyVerify.first) {
                        txt_fence_2.text = "User is within $ID_POLY1_FENCE area!"
                        txt_fence_2.setTextColor(ContextCompat.getColor(this, R.color.colorGreen))
                    } else {
                        txt_fence_2.text = "User is outside of $ID_POLY1_FENCE area!"
                        txt_fence_2.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
                    }

                    if (polyVerify.second) {
                        txt_fence_3.text = "User is within $ID_POLY2_FENCE area!"
                        txt_fence_3.setTextColor(ContextCompat.getColor(this, R.color.colorGreen))
                    } else {
                        txt_fence_3.text = "User is outside of $ID_POLY2_FENCE area!"
                        txt_fence_3.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
                    }
                }
    }

    private fun doOnFirstLocationUpdate(currentLocation: LatLng) {

        if (mCurrentLocation != null && !fenceCreated) {


            PolygonHelper.addText(this, mMap, currentLocation, ID_CURRENT_FENCE, 5, 15)

            val latLng = LatLng(mCurrentLocation?.latitude!!, mCurrentLocation?.longitude!!)

            //Add geofence on current location
            GeoFenceHelper.addGeoFence(latLng, 100.0, mMap, ID_CURRENT_FENCE)

            fenceCreated = true

            val nearBy1 = PolygonHelper.getNearByLocation(currentLocation, 600)
            PolygonHelper.addText(this, mMap, nearBy1, ID_POLY1_FENCE, 5, 15)

            PolygonHelper.drawRectanglePolygonWithFence(mMap, nearBy1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                            onNext = {
                                GeoFenceHelper.addGeoFence(it.first, it.second, mMap, ID_POLY1_FENCE)
                            },
                            onError = {
                                it.printStackTrace()
                            }
                    )

            val nearBy2 = PolygonHelper.getNearByLocation(currentLocation, 300)
            PolygonHelper.addText(this, mMap, nearBy2, ID_POLY2_FENCE, 5, 15)

            PolygonHelper.drawTrianglePolygonWithFence(mMap, nearBy2)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                            onNext = {
                                GeoFenceHelper.addGeoFence(it.first, it.second, mMap, ID_POLY2_FENCE)
                            },
                            onError = {
                                it.printStackTrace()
                            }
                    )

            //Setup Listener
            GeoFenceHelper.geoFenceListener(geofencingClient, this)
            txt_fence_1.text = "Geofences Activated!"
        }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        mMap.clear()
        MapHelper.markerPoints?.clear()
        MapHelper.lastMarker?.remove()
        stopService(Intent(this, GeofenceTransitionsIntentService::class.java))
        GeoFenceHelper.stop(this, arrayListOf(ID_CURRENT_FENCE, ID_POLY1_FENCE, ID_POLY2_FENCE))
    }
}