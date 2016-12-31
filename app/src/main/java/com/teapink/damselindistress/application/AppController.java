package com.teapink.damselindistress.application;

import android.app.Application;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class AppController extends Application {

    private static final String TAG = AppController.class.getSimpleName();
    public static final String START_PHONE_VERIFICATION_URL = "https://api.authy.com/protected/json/phones/verification/start";
    public static final String VERIFY_CODE_URL = "https://api.authy.com/protected/json/phones/verification/check";
    public static final String TWILIO_API_KEY = "<YOUR_TWILIO_API_KEY>";
    public static final String GOOGLE_MAPS_DISTANCE_MATRIX_API_KEY = "<YOUR_DISTANCE_MATRIX_API_KEY>";
    public static final int SOCKET_TIMEOUT_MS = 5000;

    private RequestQueue mRequestQueue;

    private static AppController mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public static synchronized AppController getInstance() {
        return mInstance;
    }

    private RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }
}
