package com.embraceit.batchdrawgeolocations

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_geofence_map.*
import java.io.IOException

/**
 * Created by Umair Adil on 08/12/2016.
 */

class GeoFenceMapActivity : BaseActivity(), OnMapReadyCallback {


    private val TAG = "GeoFenceMapActivity"

    private var mMap: GoogleMap? = null
    private var camera_zoom_level = 12.0f

    //To Clear
    private var polyLinesToClear = arrayListOf<Polyline?>()
    private var markersToClear = arrayListOf<Marker>()
    private var myMarker: Marker? = null

    //States
    private var markersOnly = false

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geofence_map)

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
        val data = readAssetsXML("testFile", this)
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

    fun drawMarker(
            context: Context,
            mMap: GoogleMap,
            markerPoint: LatLng,
            index: Int,
            title: String,
            snippet: String
    ): Marker {
        var icon: Bitmap? = null

        when (index) {
            0 -> icon = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.ic_person_pin_circle_black_24dp)!!)
            99 -> icon = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.ic_marker)!!)
        }

        return mMap.addMarker(
                MarkerOptions()
                        .position(markerPoint)
                        .snippet(snippet)
                        .title(title)
                        .icon(BitmapDescriptorFactory.fromBitmap(icon))
        )
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        var bitmap: Bitmap? = null

        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)
        return bitmap
    }

    fun readAssetsXML(fileName: String, context: Context): String? {
        var xmlString: String? = null
        val am = context.assets
        try {
            val `is` = am.open(fileName)
            val length = `is`.available()
            val data = ByteArray(length)
            `is`.read(data)
            xmlString = String(data)
        } catch (e1: IOException) {
            e1.printStackTrace()
        } catch (e1: NullPointerException) {
            e1.printStackTrace()
        }
        return xmlString
    }
}
