package com.teapink.damselindistress.models;

public class User {
    public String phone;
    public Info info;
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


    public static class Info {
        public String name;
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
    }

    public static class Location {
        public String latitude;
        public String longitude;
        public boolean allowNotification;   // user has agreed to receive SMS if someone nearby is in danger

        public Location() {
            this.latitude = "null";
            this.longitude = "null";
            this.allowNotification = true;
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

        public void setAllowNotification(boolean allowNotification) {
            this.allowNotification = allowNotification;
        }

        public String getLatitude() {
            return latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public boolean isAllowNotification() {
            return allowNotification;
        }
    }

}
