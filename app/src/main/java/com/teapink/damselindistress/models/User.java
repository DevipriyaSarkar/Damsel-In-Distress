package com.teapink.damselindistress.models;

import android.util.Log;

public class User {
    private static final String TAG = "User";
    @SuppressWarnings("WeakerAccess")   // field cannot be private for Firebase to access
    public String phone;
    @SuppressWarnings("WeakerAccess")
    public Info info;
    @SuppressWarnings("WeakerAccess")
    public Location location;

    public User() {}

    public User(String phone, Info info, Location location) {
        this.phone = phone;
        this.info = info;
        this.location = location;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public Info getInfo() {
        return info;
    }

    public Location getLocation() {
        return location;
    }

    public void display() {
        Log.d(TAG, "Phone: " + phone);
        info.display();
        location.display();
    }


    public static class Info {
        private static final String TAG = "Info";
        @SuppressWarnings("WeakerAccess")   // field cannot be private for Firebase to access
        public String name;
        @SuppressWarnings("WeakerAccess")
        public String password;

        public Info() {}
        public Info(String name, String password) {
            this.name = name;
            this.password = password;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public String getPassword() {
            return password;
        }

        public void display() {
            Log.d(TAG, "User Name: " + name);
        }
    }

    public static class Location {
        private static final String TAG = "Location";
        @SuppressWarnings("WeakerAccess")   // field cannot be private for Firebase to access
        public String latitude;
        @SuppressWarnings("WeakerAccess")
        public String longitude;
        @SuppressWarnings("WeakerAccess")
        public boolean alertAllowed;   // user has agreed to receive SMS if someone nearby is in danger

        public Location() {
            this.latitude = "null";
            this.longitude = "null";
            this.alertAllowed = true;
        }
        public Location(String latitude, String longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }

        public void setLongitude(String longitude) {
            this.longitude = longitude;
        }

        public void setAlertAllowed(boolean alertAllowed) {
            this.alertAllowed = alertAllowed;
        }

        public String getLatitude() {
            return latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public boolean hasAlertAllowed() {
            return alertAllowed;
        }

        public void display() {
            Log.d(TAG, "Latitude: " + latitude);
            Log.d(TAG, "Longitude: " + longitude);
            Log.d(TAG, "Alert Allowed: " + alertAllowed);
        }
    }

}
