package com.embraceit.batchdrawgeolocations.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.support.v4.content.ContextCompat
import com.embraceit.batchdrawgeolocations.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Created by umair on 2019-09-13 16:04
 * for batchDraw
 */

/*
 * This will convert drawable resource to bitmap.
 * Used for drawing custom markers on map.
 */
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

/*
 * Add marker to map.
 */
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

/*
 * Returns 'true' if connected to internet.
 */
fun isConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}