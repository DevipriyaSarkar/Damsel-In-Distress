package com.teapink.damselindistress;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.IntentCompat;
import android.util.Log;


public class SensorService extends IntentService implements SensorEventListener {

    final String TAG = this.getClass().getSimpleName();
    final int SHAKE_THRESHOLD = 8;
    final float NOISE = (float) 2.0;
    float mLastX, mLastY, mLastZ;
    long lastUpdate = 0;
    boolean mInitialized;
    SensorManager mSensorManager;
    Sensor mAccelerometer;

    public SensorService() {
        super("SensorService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mInitialized = false;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long curTime = System.currentTimeMillis();
        // only allow one update every 10000 ms.
        if ((curTime - lastUpdate) > 10000) {
            long diffTime = (curTime - lastUpdate);

            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            if (!mInitialized) {
                mLastX = x;
                mLastY = y;
                mLastZ = z;
                mInitialized = true;
            } else {
                float deltaX = Math.abs(mLastX - x);
                float deltaY = Math.abs(mLastY - y);
                float deltaZ = Math.abs(mLastZ - z);
                if (deltaX < NOISE) deltaX = (float) 0.0;
                if (deltaY < NOISE) deltaY = (float) 0.0;
                if (deltaZ < NOISE) deltaZ = (float) 0.0;

                float speed = Math.abs(deltaX + deltaY + deltaZ) / diffTime * 10000;
                Log.d(TAG, "Shake detected with speed " + speed);

                if (speed > SHAKE_THRESHOLD) {
                    Log.d(TAG, "Time Difference " + (curTime - lastUpdate));
                    lastUpdate = curTime;
                    Intent intent = new Intent(this, MainActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("CALLED_FROM", "SensorService");
                    intent.putExtras(bundle);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                mLastX = x;
                mLastY = y;
                mLastZ = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // can be safely ignored for this application
    }
}
