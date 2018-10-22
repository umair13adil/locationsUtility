package com.embraceit.batchdrawgeolocations

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.support.annotation.NonNull
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.michaelflisar.rxbus2.RxBus
import com.michaelflisar.rxbus2.interfaces.IRxBusQueue
import com.michaelflisar.rxbus2.rx.RxDisposableManager
import io.reactivex.processors.BehaviorProcessor
import org.reactivestreams.Publisher


abstract class BaseActivity : AppCompatActivity(), IRxBusQueue {

    private val TAG = "BaseActivity"
    private val mResumedProcessor = BehaviorProcessor.createDefault(false)

    private lateinit var locationCallback: LocationCallback
    /**
     * Code used in requesting runtime permissions.
     */
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

    /**
     * Constant used in the location settings dialog.
     */
    private val REQUEST_CHECK_SETTINGS = 0x1

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 40000

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2

    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    /**
     * Provides access to the Location Settings API.
     */
    private var mSettingsClient: SettingsClient? = null

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private var mLocationRequest: LocationRequest? = null

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private var mLocationSettingsRequest: LocationSettingsRequest? = null

    /**
     * Represents a geographical location.
     */
    var mCurrentLocation: Location? = null

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private var mRequestingLocationUpdates: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                mCurrentLocation = locationResult?.lastLocation
                RxBus.get().withSendToSuperClasses(true).send(mCurrentLocation)
                Log.i(TAG, "Location: ${mCurrentLocation.toString()}")
            }
        }

        createLocationRequest()
        buildLocationSettingsRequest()
    }

    override fun onResume() {
        super.onResume()
        mResumedProcessor.onNext(true)
        if (mRequestingLocationUpdates)
            startLocationUpdates()
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest?.interval = UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest?.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS

        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onPause() {
        mResumedProcessor.onNext(false)
        super.onPause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        RxDisposableManager.unsubscribe(this)
        super.onDestroy()
    }

    override fun isBusResumed(): Boolean {
        return mResumedProcessor.value!!
    }

    override fun getResumeObservable(): Publisher<Boolean> {
        return mResumedProcessor
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this@BaseActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>,
                                            @NonNull grantResults: IntArray) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates")
                    startLocationUpdates()
                }
            } else {
                Log.e(TAG, "Permission denied")
            }
        }
    }

    /**
     * Uses a [com.google.android.gms.location.LocationSettingsRequest.Builder] to build
     * a [com.google.android.gms.location.LocationSettingsRequest] that is used for checking
     * if a device has the needed location settings.
     */
    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> Log.i(TAG, "User agreed to make required location settings changes.")
                Activity.RESULT_CANCELED -> {
                    Log.i(TAG, "User chose not to make required location settings changes.")
                    mRequestingLocationUpdates = false
                }
            }// Nothing to do. startLocationupdates() gets called in onResume again.
        }
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Begin by checking if the device has the necessary location settings.
            mSettingsClient?.checkLocationSettings(mLocationSettingsRequest)
                    ?.addOnSuccessListener(this) {
                        Log.i(TAG, "All location settings are satisfied.")

                        mFusedLocationClient?.requestLocationUpdates(mLocationRequest,
                                locationCallback, Looper.myLooper())

                    }
                    ?.addOnFailureListener(this) { e ->
                        val statusCode = (e as ApiException).statusCode
                        when (statusCode) {
                            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " + "location settings ")
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    val rae = e as ResolvableApiException
                                    rae.startResolutionForResult(this@BaseActivity, REQUEST_CHECK_SETTINGS)
                                } catch (sie: IntentSender.SendIntentException) {
                                    Log.i(TAG, "PendingIntent unable to execute request.")
                                }
                            }
                            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                                val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                                Log.e(TAG, errorMessage)
                                Toast.makeText(this@BaseActivity, errorMessage, Toast.LENGTH_LONG).show()
                                mRequestingLocationUpdates = false
                            }
                        }
                    }
        } else {
            requestPermissions()
        }
    }

}