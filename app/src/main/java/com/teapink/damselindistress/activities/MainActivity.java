package com.teapink.damselindistress.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.teapink.damselindistress.R;
import com.teapink.damselindistress.models.User;
import com.teapink.damselindistress.services.ShakeSensorService;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    final String TAG = this.getClass().getSimpleName();
    final String TAG_SENSOR = ShakeSensorService.class.getSimpleName();
    ToggleButton toggleButton;
    MediaPlayer mediaPlayer;
    AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(getApplicationContext(), ShakeSensorService.class));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        toggleButton = (ToggleButton) findViewById(R.id.panicBtn);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    toggleButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_bg_off));
                    Toast.makeText(getApplicationContext(), "Playing sound", Toast.LENGTH_SHORT).show();
                    setMediaVolumeMax();
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.scream);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                    sendSMSMessage();
                } else {
                    // The toggle is disabled
                    toggleButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_bg_on));
                    Toast.makeText(getApplicationContext(), "Stopped sound", Toast.LENGTH_SHORT).show();
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                }
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String calledFrom = bundle.getString("CALLED_FROM");
            if (calledFrom != null && calledFrom.equals("ShakeSensorService")) {
                Log.d(TAG, "Called From " + TAG_SENSOR);
                toggleButton.setChecked(true);
            }
        }

        // test shared pref contents
/*        SharedPreferences sp = getSharedPreferences("LOGGED_USER", Context.MODE_PRIVATE);
        String user = sp.getString("current_user", null);
        Log.d(TAG, "Current User: " + user);

        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String prefName = sp.getString("prefName", null);
        String prefPhone = sp.getString("prefPhone", null);
        boolean prefAlert = sp.getBoolean("prefAlert", true);
        Log.d(TAG, "Current Settings User: " + "Name: " + prefName + " Phone: " + prefPhone + " Alert: " + prefAlert);*/
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

            //intent.setData(Uri.parse("tel:" + phoneNo));
            //          startActivity(intent);
            //        Toast.makeText(getApplicationContext(), "Calling", Toast.LENGTH_LONG).show();
        }

        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "SMS failed, please try again.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setMediaVolumeMax() {
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(3);
        audioManager.setStreamVolume(3, maxVolume, 1);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void logOut() {
        SharedPreferences sp1 = getSharedPreferences("LOGGED_USER", MODE_PRIVATE);
        SharedPreferences sp2 = getSharedPreferences("FIRST_LAUNCH", MODE_PRIVATE);

        String currentUser = sp1.getString("current_user", null);
        if (currentUser != null) {
            Gson gson = new Gson();
            User user = gson.fromJson(currentUser, User.class);
            user.getLocation().setAlertAllowed(false);
            // un-subscribe from SMS alerts when the user logs out
            DatabaseReference databaseRef;
            databaseRef = FirebaseDatabase.getInstance().getReference();
            databaseRef.child("location").child(user.getPhone()).setValue(user.getLocation());  // update the un-subscription online
        }

        SharedPreferences.Editor editor1 = sp1.edit();
        SharedPreferences.Editor editor2 = sp2.edit();
        editor1.clear();
        editor1.commit();
        editor2.clear();
        editor2.commit();

        // un-register from the service
        stopService(new Intent(getApplicationContext(), ShakeSensorService.class));

        finish();
        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_contacts) {
            // manage emergency contacts
            Intent intent = new Intent(getApplicationContext(), EmergencyContactsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            // app settings
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_log_out) {
            logOut();
        } else if (id == R.id.nav_invite) {
            // share the app with others
            Intent share = new Intent(android.content.Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_action_text));
            startActivity(Intent.createChooser(share, getString(R.string.share_action_intent_chooser)));
        } else if (id == R.id.nav_about) {
            // credits :P
            final SpannableString spannableString = new SpannableString(getString(R.string.about_message));
            Linkify.addLinks(spannableString, Linkify.EMAIL_ADDRESSES);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.about_title);
            alertDialogBuilder.setMessage(spannableString);
            alertDialogBuilder.setNeutralButton(R.string.about_neutral_button,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // do nothing
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            // make emails clickable
            ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            toggleButton.setChecked(false);
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
