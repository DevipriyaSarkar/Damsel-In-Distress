package com.teapink.damselindistress.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.teapink.damselindistress.models.User;

public class LocationTrackerService extends Service {
    private final String TAG = this.getClass().getSimpleName();
    private LocationManager locationManager = null;
    private LocationListener locationListener;
    private static final int LOCATION_INTERVAL = 120000;    // interval in milliseconds
    private static final float LOCATION_DISTANCE = 50f;     // distance in meters
    private Location lastBestKnownLocation = null;

    public LocationTrackerService() {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        if (locationManager == null) {
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
        locationListener = new LocationListener();
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
        } catch (SecurityException e) {
            Log.d(TAG, "Failed to request location update, ignore ", e);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "GPS provider does not exist " + e.getMessage());
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
        } catch (SecurityException e) {
            Log.d(TAG, "Failed to request location update, ignore ", e);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Network provider does not exist, " + e.getMessage());
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
        } catch (java.lang.SecurityException e) {
            Log.d(TAG, "Failed to request location update, ignore ", e);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Passive provider does not exist " + e.getMessage());
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                Log.d(TAG, "Failed to remove location listeners, ignore ", e);
            } catch (Exception e) {
                Log.i(TAG, "Failed to remove location listeners, ignore ", e);
            }
        }
    }

    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > LOCATION_INTERVAL;
        boolean isSignificantlyOlder = timeDelta < -LOCATION_INTERVAL;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    // checks whether two providers are the same
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private void updateFirebaseDB(User user) {
        DatabaseReference databaseRef;
        databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child("location").child(user.getPhone()).setValue(user.getLocation());
    }

    private void updateSharedPref(User user) {
        SharedPreferences sharedPref = getSharedPreferences("LOGGED_USER", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);
        editor.putString("current_user", json);
        editor.apply();
    }

    public class LocationListener implements android.location.LocationListener {
        private final String TAG = "LocationListener";

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged: " + location);
            if (isBetterLocation(location, lastBestKnownLocation)) {
                // get the user information
                User user;
                SharedPreferences sp = getSharedPreferences("LOGGED_USER", MODE_PRIVATE);
                String currentUser = sp.getString("current_user", null);
                if (currentUser != null) {
                    Gson gson = new Gson();
                    user = gson.fromJson(currentUser, User.class);

                    user.getLocation().setLatitude(String.valueOf(location.getLatitude()));
                    user.getLocation().setLongitude(String.valueOf(location.getLongitude()));

                    // update location in all the DBs
                    updateFirebaseDB(user);
                    updateSharedPref(user);
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged: " + provider);
        }
    }
}