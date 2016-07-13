package com.teapink.damselindistress;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ScreamActivity extends AppCompatActivity {

    Button buttonOff;
    MediaPlayer mp;
    AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scream);

        buttonOff = (Button) findViewById(R.id.panicBtn);
        buttonOff.setEnabled(false);

        mp = MediaPlayer.create(this, R.raw.scream);
        mp.setLooping(true);
        setMediaVolumeMax();

/*        HardwareButtonReceiver receiver = new HardwareButtonReceiver();
        registerMediaButtonEventReceiver(receiver);*/

        Toast.makeText(getApplicationContext(), "Playing sound", Toast.LENGTH_SHORT).show();
        mp.start();
        buttonOff.setEnabled(true);
        sendSMSMessage();

        buttonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Stopped sound", Toast.LENGTH_SHORT).show();
                mp.stop();
                mp.reset();
                buttonOff.setEnabled(false);
                buttonOff.setText(getResources().getString(R.string.disabled));
            }
        });
    }



    private void sendSMSMessage() {
        Log.i("Send SMS", "");
        String phoneNo = "9686970950";
        String message = "Distress !! Panic on the senders side. Lat: 12.908341472, Long: 77.632927725";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
            //Intent intent = new Intent(Intent.ACTION_CALL);

//            intent.setData(Uri.parse("tel:" + phoneNo));
            //          startActivity(intent);
            //        Toast.makeText(getApplicationContext(), "Calling", Toast.LENGTH_LONG).show();
        }

        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "SMS failed, please try again.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setMediaVolumeMax()
    {
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int i = audioManager.getStreamMaxVolume(3);
        audioManager.setStreamVolume(3, i, 1);
    }

    public void onBackPressed()
    {   if (mp != null) {
            if (mp.isPlaying())
                mp.stop();
            mp.release();
            mp = null;
        }
        super.onBackPressed();
    }

    protected void onPause()
    {   if(mp != null)
            if(mp.isPlaying())
                mp.pause();
        super.onPause();
    }

    protected void onStop() {
        super.onStop();
        if (mp != null) {
            if (mp.isPlaying()) {
                mp.stop();
            }
            mp.release();
            mp = null;
        }
    }
/*
    private class HardwareButtonReceiver implements BroadcastReceiver {
        void onReceive(Intent intent) {
            KeyEvent e = (KeyEvent) intent.getExtra(Intent.EXTRA_KEY_EVENT);
            if(e.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                //code
            }
        }
    }*/


}