package com.embraceit.batchdrawgeolocations

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.embraceit.batchdrawgeolocations.utils.drawMarker
import com.embraceit.batchdrawgeolocations.utils.readAssetsXML
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.activity_coordinates_mapper.*

/**
 * Created by Umair Adil on 08/12/2016.
 */

class CoordinatesMapperActivity : BaseActivity(), OnMapReadyCallback {


    private val TAG = "CoordinatesMapper"

    private var mMap: GoogleMap? = null
    private var camera_zoom_level = 12.0f

    //To Clear
    private var polyLinesToClear = arrayListOf<Polyline?>()
    private var markersToClear = arrayListOf<Marker>()
    private var myMarker: Marker? = null

    //States
    private var markersOnly = false

    //Test File From Assets Directory
    private val TEST_FILE_NAME = "testFile"

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coordinates_mapper)

        //Attach map fragment
        showMap()


        btn_clear.setOnClickListener {
            clearMap()
        }

        switch3.setOnCheckedChangeListener { compoundButton, b ->
            markersOnly = b
            clearMap()
            loadLocationDataFromFile()
        }
    }

    private fun clearMap() {

        polyLinesToClear.forEach {
            it?.remove()
        }

        myMarker?.remove()

        markersToClear.forEach {
            it.remove()
        }
    }

    private fun loadLocationDataFromFile() {
        val data = readAssetsXML(TEST_FILE_NAME, this)
        val lines: List<String> = data?.reader()?.readLines()!!

        val locationData = arrayListOf<LatLng>()

        lines.forEach {

            var dataRead = it

            if (dataRead.contains(Regex(";"))) {
                dataRead = dataRead.replace(";", ",")
            }

            val d1 = dataRead.split(",")

            try {
                locationData.add(LatLng(d1[0].toDouble(), d1[1].toDouble()))
            } catch (e: Exception) {

            }
        }

        val filtered = cleanUpLocationData(locationData)

        if (!markersOnly) {
            val locationsLatLng = PolylineOptions().addAll(filtered).color(Color.MAGENTA).width(5.0f)
            val p1 = mMap?.addPolyline(locationsLatLng)
            polyLinesToClear.add(p1)
            createMarkers(filtered)
        } else {
            createMarkers(filtered)
        }

        if (filtered.isNotEmpty())
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(filtered.first(), camera_zoom_level))
    }

    private fun createMarkers(filtered: List<LatLng>) {
        mMap?.let { mMap ->
            this.let {

                filtered.forEach { latLng1 ->

                    //Draw marker on map
                    val m = drawMarker(it,
                            mMap,
                            latLng1,
                            0,
                            "",
                            "")

                    markersToClear.add(m)
                }
            }
        }
    }

    private fun cleanUpLocationData(data: List<LatLng>): List<LatLng> {

        val filteredList = data.distinctBy {
            Pair(it.latitude, it.longitude)
        }

        var deletedCount = 0

        if (filteredList.isNotEmpty() && filteredList.size < data.size) {

            val remaining = data.toSet().minus(filteredList.toSet())

            //Delete non-distinct objects
            if (remaining.isNotEmpty())
                deletedCount = remaining.toList().size
        }

        Log.i(TAG, "Actual List: ${data.size}, Filtered: ${filteredList.size}, Deleted: $deletedCount")

        return filteredList
    }


    /*
     * Call back of google map's ready.
     * When map is ready, only then markers will be added.
     */
    override fun onMapReady(mMap: GoogleMap) {
        this.mMap = mMap
        loadLocationDataFromFile()
    }

    /*
     * This will add google map to Framelayout.
     */
    private fun showMap() {
        val fragment = SupportMapFragment.newInstance(GoogleMapOptions().zoomControlsEnabled(true).zoomGesturesEnabled(true).tiltGesturesEnabled(true))
        supportFragmentManager.beginTransaction()
                .add(R.id.map_container, fragment)
                .commit()
        fragment?.getMapAsync(this)
    }
}
