package com.embraceit.batchdrawgeolocations.utils


private val PACKAGE_NAME = "com.embraceit.batchdrawgeolocations"

val GEOFENCES_ADDED_KEY = "$PACKAGE_NAME.GEOFENCES_ADDED_KEY"

/**
 * Used to set an expiration time for a geofence. After this amount of time Location Services
 * stops tracking the geofence.
 */
val GEOFENCE_EXPIRATION_IN_HOURS: Long = 12

/**
 * For this sample, geofences expire after twelve hours.
 */
val GEOFENCE_EXPIRATION_IN_MILLISECONDS = 60 * 1000
val GEOFENCE_RADIUS_IN_METERS = 1609f // 1 mile, 1.6 km