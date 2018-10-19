package com.embraceit.batchdrawgeolocations

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.embraceit.batchdrawgeolocations.geofence.GeoFenceHelper
import com.embraceit.batchdrawgeolocations.maps.MapHelper
import com.embraceit.batchdrawgeolocations.maps.MapHelper.drawRoutes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.michaelflisar.rxbus2.RxBusBuilder
import com.michaelflisar.rxbus2.rx.RxBusMode
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : BaseActivity(), OnMapReadyCallback, GoogleMap.OnMarkerDragListener {

    val TAG = MapsActivity::class.java.simpleName
    private lateinit var mMap: GoogleMap
    var selectedCase = 0
    lateinit var geofencingClient: GeofencingClient

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        geofencingClient = GeoFenceHelper.init(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btn_filter.setOnClickListener {
            mMap.clear()
            txt_info?.text = ""

            when (selectedCase) {
                R.id.case1 -> {
                    MapHelper.drawRoutes(true, mMap)
                }
                R.id.case2 -> {
                    MapHelper.doForLocations(R.array.locations_1, R.array.locations_actual_1, true, mMap, resources)
                }
                R.id.case3 -> {
                    MapHelper.doForLocations(R.array.locations_2, R.array.locations_actual_2, true, mMap, resources)
                }
                R.id.case4 -> {
                    MapHelper.doForLocations(R.array.locations_3, R.array.locations_actual_3, true, mMap, resources)
                }
                R.id.case5 -> {
                    MapHelper.doForLocations(R.array.locations_4, R.array.locations_actual_4, true, mMap, resources)
                }
            }
        }

        btn_clear.setOnClickListener {
            mMap.clear()
            MapHelper.markerPoints?.clear()
            MapHelper.lastMarker?.remove()
            txt_info?.text = ""
        }

        //Simulate by default
        selectedCase = R.id.case1

        RxBusBuilder.create(String::class.java)
                .withMode(RxBusMode.Main)
                .subscribe { data ->
                    Toast.makeText(this, data, Toast.LENGTH_LONG).show()
                }

        RxBusBuilder.create(Location::class.java)
                .withMode(RxBusMode.Main)
                .subscribe { data ->
                    MapHelper.drawMyMarker(LatLng(data.latitude, data.longitude), mMap)
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
                drawRoutes(true, mMap)
            }
            R.id.case2 -> {
                selectedCase = R.id.case2
                MapHelper.doForLocations(R.array.locations_1, R.array.locations_actual_1, false, mMap, resources)
                //MapHelper.createMiniFences(R.array.locations_actual_1, mMap, resources)
            }
            R.id.case3 -> {
                selectedCase = R.id.case3
                MapHelper.doForLocations(R.array.locations_2, R.array.locations_actual_2, false, mMap, resources)
                //MapHelper.createMiniFences(R.array.locations_actual_2, mMap, resources)
            }
            R.id.case4 -> {
                selectedCase = R.id.case4
                MapHelper.doForLocations(R.array.locations_3, R.array.locations_actual_3, false, mMap, resources)
                //MapHelper.createMiniFences(R.array.locations_actual_3, mMap, resources)
            }
            R.id.case5 -> {
                selectedCase = R.id.case5
                MapHelper.doForLocations(R.array.locations_4, R.array.locations_actual_4, true, mMap, resources)
                //MapHelper.createMiniFences(R.array.locations_actual_4, mMap, resources)
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
        mMap.setOnMarkerDragListener(this)

        val latLng = LatLng(55.58446, 12.304166)
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(16f).build()

        GeoFenceHelper.addGeoFence(latLng, 200.0, mMap)
        GeoFenceHelper.geoFenceListener(geofencingClient, this)

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        /*mMap.setOnMarkerClickListener { marker ->

            if (marker.tag != null && selectedCase == R.id.case1) {
                marker.remove()
                if (markerPoints != null && markerPoints?.size!! > 0)
                    markerPoints?.removeAt(markerPoints?.size!! - 1)
            }
            true
        }

        mMap.setOnPolygonClickListener {
            it.remove()
        }

        mMap.setOnMapClickListener { latLng ->

            if (selectedCase == R.id.case1) {
                drawMarker(latLng, mMap, this)
                drawRoutes(false, mMap)
            }
        }

        Toast.makeText(this, "Put some markers on Map to start simulating behaviour.", Toast.LENGTH_LONG).show()*/
    }


    override fun onMarkerDragStart(marker: Marker) {}

    override fun onMarkerDrag(marker: Marker) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
    }

    override fun onMarkerDragEnd(marker: Marker) {
        var position = marker.tag as Int
        position -= 1
        MapHelper.markerPoints?.set(position, marker.position)
    }
}
