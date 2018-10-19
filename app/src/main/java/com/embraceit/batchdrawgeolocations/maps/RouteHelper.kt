package com.embraceit.batchdrawgeolocations.maps

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.embraceit.batchdrawgeolocations.R
import com.embraceit.batchdrawgeolocations.utils.badgeLayout.BadgeTextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.math.BigDecimal

object RouteHelper {

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
    fun move(startLL: LatLng, toNorth: Double, toEast: Double): LatLng {
        val lonDiff = meterToLongitude(toEast, startLL.latitude)
        val latDiff = meterToLatitude(toNorth)
        return LatLng(startLL.latitude + latDiff, startLL.longitude + lonDiff)
    }

    fun meterToLongitude(meterToEast: Double, latitude: Double): Double {
        val latArc = Math.toRadians(latitude)
        val radius = Math.cos(latArc) * EARTHRADIUS
        val rad = meterToEast / radius
        return Math.toDegrees(rad)
    }

    fun meterToLatitude(meterToNorth: Double): Double {
        val rad = meterToNorth / EARTHRADIUS
        return Math.toDegrees(rad)
    }

    fun getMarkerBitmapFromView(count: Int, activity: Activity): Bitmap {

        val customMarkerView = (activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.view_custom_marker, null)

        BadgeTextView.update(activity, customMarkerView as FrameLayout, BadgeTextView.Builder()
                //.textBackgroundColor(array[new Random().nextInt(array.length)])
                .textColor(Color.WHITE))
        BadgeTextView.getBadgeTextView(customMarkerView as FrameLayout).setBadgeCount(count)
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight())
        customMarkerView.buildDrawingCache()
        val returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN)
        val drawable = customMarkerView.getBackground()
        if (drawable != null)
            drawable.draw(canvas)
        customMarkerView.draw(canvas)
        return returnedBitmap
    }

    fun getBounds(locations: List<String>): LatLngBounds {
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


}