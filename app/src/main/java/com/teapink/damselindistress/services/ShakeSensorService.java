package com.teapink.damselindistress.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import com.teapink.damselindistress.activities.MainActivity;
import com.teapink.damselindistress.utilities.ShakeDetector;

public class ShakeSensorService extends Service {

    private final String TAG = this.getClass().getSimpleName();
    private SensorManager mSensorManager;
    private ShakeDetector mShakeDetector;

    public ShakeSensorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
                if (count == 3) {
                    Log.d(TAG, "Shake Count:" + count);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED +
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD +
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON +
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                    intent.putExtra("CALLED_FROM", TAG);
                    startActivity(intent);
                }

            }
        });
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mSensorManager.unregisterListener(mShakeDetector);
        super.onDestroy();
    }

}