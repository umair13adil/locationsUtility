package com.embraceit.batchdrawgeolocations;

/**
 * Created by Umair Adil on 05/04/2017.
 */

import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LocationValidator {


    @NonNull
    private static LocationValidator ourInstance = new LocationValidator();
    private String TAG = LocationValidator.class.getSimpleName();

    /* Max distance per second upto 10 seconds */
    private float THRESHOLD_DISTANCE_1 = 20;

    /* Max distance per second upto 30 seconds*/
    private float THRESHOLD_DISTANCE_2 = 15;

    /* Max distance per second above 30 seconds*/
    private float THRESHOLD_DISTANCE_3 = 10;

    /* Max size of error list to swap it with normal list*/
    private int THRESHOLD_SIZE = 5;

    private List<LocationTime> normalList = new ArrayList<LocationTime>();
    private List<LocationTime> errorList = new ArrayList<LocationTime>();

    public boolean validateNewValue(Location location) {
        Boolean result = true;
        if (normalList.size() <= 0) {
            normalList.add(new LocationTime(location, new Date()));
            errorList.clear();
        } else {
            LocationTime lastLoc = normalList.get(normalList.size() - 1);
            float distance = Math.abs(location.distanceTo(lastLoc.location));
            long timepast = lastLoc.secondsTillNow();
            if (isValueSpike(timepast, distance)) {

                //is error
                validateSpikeValue(location);
                result = false;
            } else {

                //Its a normal value
                normalList.add(new LocationTime(location, new Date()));
                if (normalList.size() > THRESHOLD_SIZE) {
                    normalList.remove(0);
                }
                errorList.clear();
            }
        }
        return result;
    }

    public Location lastLocation() {
        if (normalList.size() <= 0) {
            return null;
        } else if (normalList.size() == 1) {
            return normalList.get(normalList.size() - 1).location;
        } else {
            return normalList.get(normalList.size() - 2).location;
        }
    }

    public boolean validateNewValue(LatLng latLng) {
        Boolean result = true;
        Location location = new Location("");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        return validateNewValue(location);

    }

    private void validateSpikeValue(Location loc) {
        if (errorList.size() <= 0) {
            errorList.add(new LocationTime(loc, new Date()));
        } else {
            LocationTime lastLoc = errorList.get(errorList.size() - 1);
            float distance = Math.abs(loc.distanceTo(lastLoc.location));
            long timepast = lastLoc.secondsTillNow();
            if (isValueSpike(timepast, distance)) {

               /* value is not in range with the spike value.*/
                errorList.clear();
                errorList.add(new LocationTime(loc, new Date()));
            } else {

                errorList.add(new LocationTime(loc, new Date()));
                if (errorList.size() >= THRESHOLD_SIZE) {

                /* swap error list with normal list.*/
                    List<LocationTime> temp = new ArrayList<LocationTime>();
                    temp.addAll(normalList);
                    normalList.clear();
                    normalList.addAll(errorList);
                    errorList.addAll(temp);
                }
            }
        }
    }

    private boolean isValueSpike(long timepast, float distance) {
        Log.i(TAG,  "distance: " + distance);
        
        return ((timepast <= 10 && distance > (THRESHOLD_DISTANCE_1 * timepast))
                || (timepast <= 30 && distance > (THRESHOLD_DISTANCE_2 * timepast))
                || (distance > (THRESHOLD_DISTANCE_3 * timepast)));
    }

    private class LocationTime {
        LocationTime(Location location, Date time) {
            this.location = location;
            this.time = time;
        }

        long secondsTillNow() {
            return (System.currentTimeMillis() - this.time.getTime()) / 1000;
        }

        Location location;
        Date time;
    }
}

