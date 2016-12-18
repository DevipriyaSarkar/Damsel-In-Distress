package com.teapink.damselindistress;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    private static final int WAIT_TIME = 2000;
    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if it's first launch
        final SharedPreferences sp = getSharedPreferences("FIRST_LAUNCH", Context.MODE_PRIVATE);
        final int firstLaunch = sp.getInt("firstLaunch", 1);

        if (firstLaunch == 1) {
            setContentView(R.layout.activity_splash);

            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("firstLaunch", 0);
            editor.commit();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
            }, WAIT_TIME);

/*            Runnable onUi = new Runnable() {
                @Override
                public void run() {
                    // this will run on the main UI thread
                    Intent mainIntent = new Intent(LoadingScreenActivity.this,ProfileData.class);
                    LoadingScreenActivity.this.startActivity(mainIntent);
                    LoadingScreenActivity.this.finish();
                }
            };

            Runnable background = new Runnable() {
                @Override
                public void run() {
                    // This is the delay
                    Thread.Sleep( WAIT_TIME );
                    System.out.println("Going to Profile Data");
                    uiHandler.post( onUi );
                }
            };

            new Thread( background ).start();*/


        } else {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
    }
}
