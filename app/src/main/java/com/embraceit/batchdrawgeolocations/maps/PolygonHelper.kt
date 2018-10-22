package com.embraceit.batchdrawgeolocations.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint.Align
import android.graphics.Rect
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import io.reactivex.Flowable
import java.util.*


object PolygonHelper {

    private val TAG = "PolygonHelper"

    //List of Polygons
    private val listOfPolygon1 = arrayListOf<LatLng>()
    private val listOfPolygon2 = arrayListOf<LatLng>()

    fun getNearByLocation(location: LatLng, radius: Int): LatLng {
        val random = Random()

        // Convert radius from meters to degrees
        val radiusInDegrees = (radius / 111000f).toDouble()

        val u = random.nextDouble()
        val v = random.nextDouble()
        val w = radiusInDegrees * Math.sqrt(u)
        val t = 2.0 * Math.PI * v
        val x = w * Math.cos(t)
        val y = w * Math.sin(t)

        // Adjust the x-coordinate for the shrinking of the east-west distances
        val new_x = x / Math.cos(Math.toRadians(location.latitude))

        val foundLongitude = new_x + location.longitude
        val foundLatitude = y + location.latitude
        println("Longitude: $foundLongitude  Latitude: $foundLatitude")

        return LatLng(foundLatitude, foundLongitude)
    }

    fun addText(context: Context?, map: GoogleMap?,
                location: LatLng?, text: String?, padding: Int,
                fontSize: Int): Marker? {
        var marker: Marker? = null

        if (context == null || map == null || location == null || text == null
                || fontSize <= 0) {
            return marker
        }

        val textView = TextView(context)
        textView.text = text
        textView.textSize = fontSize.toFloat()

        val paintText = textView.paint

        val boundsText = Rect()
        paintText.getTextBounds(text, 0, textView.length(), boundsText)
        paintText.textAlign = Align.CENTER

        val conf = Bitmap.Config.ARGB_8888
        val bmpText = Bitmap.createBitmap(boundsText.width() + 2 * padding, boundsText.height() + 2 * padding, conf)

        val canvasText = Canvas(bmpText)
        paintText.color = Color.BLACK

        canvasText.drawText(text, (canvasText.width / 2).toFloat(),
                (canvasText.height - padding - boundsText.bottom).toFloat(), paintText)

        val markerOptions = MarkerOptions()
                .position(location)
                .icon(BitmapDescriptorFactory.fromBitmap(bmpText))
                .anchor(0.5f, 1f)

        marker = map.addMarker(markerOptions)

        return marker
    }

    fun drawTrianglePolygonWithFence(map: GoogleMap, latLng: LatLng): Flowable<Pair<LatLng, Double>> {

        listOfPolygon2.addAll(Arrays.asList(latLng, LatLng(latLng.latitude, latLng.longitude + 0.001), LatLng(latLng.latitude + 0.001, latLng.longitude + 0.001), latLng))

        //Get center of locations
        val center = getCenterLatLng(listOfPolygon2)

        map.addPolygon(PolygonOptions()
                .addAll(listOfPolygon2)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#97F3B9FD")))

        //Get max radius & then draw circle with that radius
        return getMaxRadius(listOfPolygon2, center)
    }

    fun drawRectanglePolygonWithFence(map: GoogleMap, latLng: LatLng): Flowable<Pair<LatLng, Double>> {

        listOfPolygon1.addAll(createRectangle(latLng, 0.003, 0.001))

        //Get center of locations
        val center = getCenterLatLng(listOfPolygon1)

        map.addPolygon(PolygonOptions()
                .addAll(listOfPolygon1)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#97F3B9FD")))

        //Get max radius & then draw circle with that radius
        return getMaxRadius(listOfPolygon1, center)
    }

    /**
     * Creates a List of LatLngs that form a rectangle with the given dimensions.
     */
    private fun createRectangle(center: LatLng, halfWidth: Double, halfHeight: Double): List<LatLng> {
        return Arrays.asList(
                LatLng(center.latitude, center.longitude),
                LatLng(center.latitude - halfHeight, center.longitude + halfWidth),
                LatLng(center.latitude - halfHeight, center.longitude - halfWidth),
                LatLng(center.latitude + halfHeight, center.longitude - halfWidth),
                LatLng(center.latitude + halfHeight, center.longitude + halfWidth),
                LatLng(center.latitude, center.longitude)
        )
    }

    private fun getCenterLatLng(locations: List<LatLng>): LatLng {
        var bounds = LatLngBounds(LatLng(0.0, 0.0), LatLng(0.0, 0.0))
        try {
            val builder = LatLngBounds.Builder()

            locations.forEach {
                builder.include(it)
            }

            val tmpBounds = builder.build()
            val center = tmpBounds.center

            /** Add 2 points 1000m northEast and southWest of the center.
             * They increase the bounds only, if they are not already larger
             * than this.
             * 1000m on the diagonal translates into about 709m to each direction. */
            val northEast = RouteHelper.move(center, 709.0, 709.0)
            val southWest = RouteHelper.move(center, -709.0, -709.0)
            builder.include(southWest)
            builder.include(northEast)

            bounds = builder.build()
        } catch (e: Exception) {

        }

        return bounds.center
    }

    private fun getMaxRadius(actualLocations: List<LatLng>?, centerLocation: LatLng): Flowable<Pair<LatLng, Double>> {

        return Flowable.unsafeCreate { it ->

            val listOfDistance = arrayListOf<Double>()
            Flowable.fromIterable(actualLocations)
                    .concatMap { s ->
                        Flowable.just<LatLng>(s)
                    }
                    .doOnNext() {
                        listOfDistance.add(RouteHelper.distance(it.latitude, it.longitude, centerLocation.latitude, centerLocation.longitude)!!)
                    }
                    .doOnComplete {
                        val radius = listOfDistance.max()!!.plus(5)
                        it.onNext(Pair(centerLocation, radius))
                    }
                    .subscribe()
        }
    }

    fun isInsidePolygon(userLocation: LatLng): Pair<Boolean, Boolean> {

        val poly1 = PolyUtil.containsLocation(userLocation, listOfPolygon1, false)
        val poly2 = PolyUtil.containsLocation(userLocation, listOfPolygon2, false)

        return Pair(poly1, poly2)
    }
}