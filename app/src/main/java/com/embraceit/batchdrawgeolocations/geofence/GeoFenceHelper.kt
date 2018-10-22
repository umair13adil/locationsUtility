package com.embraceit.batchdrawgeolocations.geofence

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.widget.Toast
import com.embraceit.batchdrawgeolocations.maps.MapHelper
import com.embraceit.batchdrawgeolocations.utils.GEOFENCE_EXPIRATION_IN_MILLISECONDS
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

object GeoFenceHelper {

    private val TAG = "GeoFenceHelper"

    private val geofenceList = arrayListOf<Geofence>()
    private lateinit var pendingIntent: PendingIntent

    fun init(activity: Activity): GeofencingClient {
        return LocationServices.getGeofencingClient(activity)
    }

    fun stop(activity: Activity, array: ArrayList<String>) {
        try {
            LocationServices.getGeofencingClient(activity).removeGeofences(array)
            LocationServices.getGeofencingClient(activity).removeGeofences(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun geoFenceListener(geofencingClient: GeofencingClient, activity: Activity) {
        geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent(activity))?.run {
            addOnSuccessListener {
                Toast.makeText(activity, "Geofences Added!", Toast.LENGTH_LONG).show()
            }
            addOnFailureListener {
                // Failed to add geofences
                it.printStackTrace()
                Toast.makeText(activity, "Unable to add geofence!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    private fun geofencePendingIntent(activity: Activity): PendingIntent {
        val intent = Intent(activity, GeofenceTransitionsIntentService::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        pendingIntent = PendingIntent.getService(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return pendingIntent
    }

    fun addGeoFence(location: LatLng, radius: Double, mMap: GoogleMap, id: String) {
        geofenceList.add(Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(id)

                // Set the circular region of this geofence.
                .setCircularRegion(
                        location.latitude,
                        location.longitude,
                        radius.toFloat()
                )
                .setLoiteringDelay(0)
                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS.toLong())

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)

                // Create the geofence.
                .build())

        MapHelper.drawCircle(location, radius, "GeoFenced", "", mMap)
    }
}