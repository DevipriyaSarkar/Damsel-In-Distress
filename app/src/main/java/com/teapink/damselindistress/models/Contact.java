package com.teapink.damselindistress.models;

import android.util.Log;

public class Contact {
    private static final String TAG = "Contact";
    public String name, phone;

    public Contact() {}
    public Contact(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void display() {
        Log.d(TAG, "Name: " + name);
        Log.d(TAG, "Phone: " + phone);
    }
}
