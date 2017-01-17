package com.teapink.damselindistress.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teapink.damselindistress.R;
import com.teapink.damselindistress.application.AppController;
import com.teapink.damselindistress.models.Contact;
import com.teapink.damselindistress.models.User;
import com.teapink.damselindistress.services.LocationTrackerService;
import com.teapink.damselindistress.services.ShakeSensorService;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.teapink.damselindistress.application.AppController.GOOGLE_MAPS_DISTANCE_MATRIX_API_KEY;
import static com.teapink.damselindistress.application.AppController.SOCKET_TIMEOUT_MS;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String TAG = this.getClass().getSimpleName();
    private final String TAG_SENSOR = ShakeSensorService.class.getSimpleName();
    private static final int PERMISSION_ALL = 1;
    private static final double RADIUS_OF_EARTH = 6371e3; // metres
    private static final double NEARBY_DISTANCE = 2000; // metres
    private ToggleButton toggleButton;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(getApplicationContext(), ShakeSensorService.class));
        startService(new Intent(getApplicationContext(), LocationTrackerService.class));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // The request code used in ActivityCompat.requestPermissions()
        // and returned in the Activity's onRequestPermissionsResult()
        String[] PERMISSIONS = new String[0];
        try {
            PERMISSIONS = getPermissions(getApplicationContext());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        toggleButton = (ToggleButton) findViewById(R.id.panicBtn);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    toggleButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_bg_off));
                    Toast.makeText(getApplicationContext(), R.string.sound_playing_message, Toast.LENGTH_SHORT).show();
                    setMediaVolumeMax();
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.scream);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                    prepareDistressAlert();
                } else {
                    // The toggle is disabled
                    toggleButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_bg_on));
                    Toast.makeText(getApplicationContext(), R.string.sound_stopped_message, Toast.LENGTH_SHORT).show();
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
        SharedPreferences sp = getSharedPreferences("LOGGED_USER", Context.MODE_PRIVATE);
        String user = sp.getString("current_user", null);
        Log.d(TAG, "Current User: " + user);

        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String prefName = sp.getString("prefName", null);
        String prefPhone = sp.getString("prefPhone", null);
        boolean prefAlert = sp.getBoolean("prefAlert", true);
        Log.d(TAG, "Current Settings User: " + "Name: " + prefName + " Phone: " + prefPhone + " Alert: " + prefAlert);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void setMediaVolumeMax() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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

    private void logOut() {
        SharedPreferences sp1 = getSharedPreferences("LOGGED_USER", MODE_PRIVATE);
        SharedPreferences sp2 = getSharedPreferences("FIRST_LAUNCH", MODE_PRIVATE);

        // release media player
        if (mediaPlayer != null) {
            toggleButton.setChecked(false);
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        String currentUser = sp1.getString("current_user", null);
        if (currentUser != null) {
            Gson gson = new Gson();
            User user = gson.fromJson(currentUser, User.class);
            user.getLocation().setAlertAllowed(false);
            // un-subscribe from SMS alerts when the user logs out
            DatabaseReference databaseRef;
            databaseRef = FirebaseDatabase.getInstance().getReference();
            // update the un-subscription online
            databaseRef.child("location").child(user.getPhone()).setValue(user.getLocation());
        }

        SharedPreferences.Editor editor1 = sp1.edit();
        SharedPreferences.Editor editor2 = sp2.edit();
        editor1.clear();
        editor1.commit();
        editor2.clear();
        editor2.commit();

        // un-register from the services
        stopService(new Intent(getApplicationContext(), ShakeSensorService.class));
        stopService(new Intent(getApplicationContext(), LocationTrackerService.class));

        finish();
        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
            TextView dialogMessage = (TextView) alertDialog.findViewById(android.R.id.message);
            if (dialogMessage != null) {
                dialogMessage.setMovementMethod(LinkMovementMethod.getInstance());
            }
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

    //check for internet connectivity
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }


    private void prepareDistressAlert() {
        Log.d(TAG, "prepareDistressAlert");
        final User user;
        SharedPreferences sp = getSharedPreferences("LOGGED_USER", MODE_PRIVATE);
        String currentUser = sp.getString("current_user", null);
        if (currentUser != null) {
            Gson gson = new Gson();
            user = gson.fromJson(currentUser, User.class);  // get user's location
            final boolean internetAvailable = isInternetAvailable();
            Log.d(TAG, "Internet Available: " + internetAvailable);

            // send distress alerts to all emergency contacts
            ArrayList<Contact> emergencyContactList;
            gson = new Gson();
            String jsonArrayList = sp.getString("contact_list", null);
            Type type = new TypeToken<ArrayList<Contact>>() {
            }.getType();
            if (jsonArrayList != null) {
                emergencyContactList = gson.fromJson(jsonArrayList, type);
                for (Contact contact : emergencyContactList) {
                    sendSMS(user, contact);
                }
            }

            // send distress alerts to nearby users of the app
            // only if current user's location is known
            if (!user.getLocation().getLatitude().equals("null") && !user.getLocation().getLongitude().equals("null")) {
                final DatabaseReference dbLocationRef;
                dbLocationRef = FirebaseDatabase.getInstance().getReference().child("location");
                dbLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            User.Location location = postSnapshot.getValue(User.Location.class);
                            // do not consider locations with null values
                            // do not consider user's own location
                            if (location.hasAlertAllowed()
                                    && !location.getLatitude().equals("null") && !location.getLongitude().equals("null")
                                    && !postSnapshot.getKey().equals(user.getPhone())) {
                                // find nearby users and send them texts
                                Contact helperContact = new Contact();
                                helperContact.setPhone(postSnapshot.getKey());
                                if (internetAvailable) {
                                    findDistanceOnline(user, location, helperContact);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, "Firebase Error: " + databaseError.getMessage());
                    }
                });
            }

        }

    }

    private void findDistanceOnline(final User curUser, final User.Location location, final Contact helperContact) {
        // use google maps distance matrix API to calculate distance between two point
        Log.d(TAG, "findDistanceOnline");
        Log.d(TAG, "Current User: ");
        curUser.display();
        Log.d(TAG, "Helper User: ");
        helperContact.display();
        Log.d(TAG, "Helper User Location: ");
        location.display();

        String lat1 = curUser.getLocation().getLatitude().trim();
        String lon1 = curUser.getLocation().getLongitude().trim();
        String lat2 = location.getLatitude().trim();
        String lon2 = location.getLongitude().trim();

        String urlString = "https://maps.googleapis.com/maps/api/distancematrix/json"
                + "?units=metric&origins=" + lat1 + "," + lon1
                + "&destinations=" + lat2 + "," + lon2
                + "&language=en"
                + "&key=" + GOOGLE_MAPS_DISTANCE_MATRIX_API_KEY;

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                urlString, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Response: " + response.toString());
                        int flag = 0;
                        try {
                            String status = response.getString("status");
                            if (status.equals("OK")) {
                                final double distance;
                                JSONObject element = response.getJSONArray("rows").getJSONObject(0)
                                        .getJSONArray("elements").getJSONObject(0);
                                if (element.getString("status").equals("OK")) {
                                    // retrieve the distance
                                    JSONObject distanceObj = element.getJSONObject("distance");
                                    distance = distanceObj.getDouble("value");

                                    // find helper's name and send message if he/she is nearby
                                    if (distance <= NEARBY_DISTANCE) {
                                        final DatabaseReference dbUserRef;
                                        dbUserRef = FirebaseDatabase.getInstance().getReference().child("users")
                                                .child(helperContact.getPhone());
                                        dbUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                User.Info info = dataSnapshot.getValue(User.Info.class);
                                                helperContact.setName(info.getName());
                                                sendSMS(curUser, helperContact, distance);
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Log.d(TAG, "Firebase Error: " + databaseError.getMessage());
                                                helperContact.setName("");
                                                sendSMS(curUser, helperContact, distance);
                                            }
                                        });
                                    }
                                } else {
                                    flag = 1;
                                }

                            } else {
                                flag = 1;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d(TAG, "JSON Error: " + e.getMessage());
                            flag =1;
                        }

                        if (flag == 1) {
                            findDistanceOffline(curUser, location, helperContact);
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error in " + TAG + " : " + error.getMessage());
                if (error instanceof TimeoutError)
                    VolleyLog.d(TAG, "Error in " + TAG + " : " + "Timeout Error");
                findDistanceOffline(curUser, location, helperContact);
            }
        });

        //Set a retry policy in case of SocketTimeout & ConnectionTimeout Exceptions.
        //Volley does retry for you if you have specified the policy.
        jsonObjReq.setRetryPolicy(new DefaultRetryPolicy(SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq);

    }

    private void findDistanceOffline(final User curUser, User.Location location, final Contact helperContact) {
        // use Haversine Formula to calculate distance between two point
        Log.d(TAG, "findDistanceOffline");
        Log.d(TAG, "Current User: ");
        curUser.display();
        Log.d(TAG, "Helper User: ");
        helperContact.display();
        Log.d(TAG, "Helper User Location: ");
        location.display();

        User.Location curUserLocation = curUser.getLocation();
        double lat1 = Math.toRadians(Double.valueOf(curUserLocation.getLatitude().trim()));
        double lat2 = Math.toRadians(Double.valueOf(location.getLatitude().trim()));
        double lon1 = Math.toRadians(Double.valueOf(curUserLocation.getLongitude().trim()));
        double lon2 = Math.toRadians(Double.valueOf(location.getLongitude().trim()));
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLong = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLong / 2) * Math.sin(deltaLong / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        final double distance = RADIUS_OF_EARTH * c;

        if (distance <= NEARBY_DISTANCE) {
            final DatabaseReference dbUserRef;
            dbUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(helperContact.getPhone());
            dbUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        User.Info info = postSnapshot.getValue(User.Info.class);
                        helperContact.setName(info.getName());
                        sendSMS(curUser, helperContact, distance);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "Firebase Error: " + databaseError.getMessage());
                    helperContact.setName("");
                    sendSMS(curUser, helperContact, distance);
                }
            });
        }
    }

    private void sendSMS(User distressedUser, Contact helpingUser, double distance) {
        Log.d(TAG, "App User Send SMS");
        // String distressedPhone = distressedUser.getPhone();
        String distressedName = distressedUser.getInfo().getName();
        String distressedLat = distressedUser.getLocation().getLatitude();
        String distressedLon = distressedUser.getLocation().getLongitude();

        String helperPhone = helpingUser.getPhone();
        String helperName = helpingUser.getName();
        String distressMessage = String.format(getString(R.string.nearby_helper_distress_message_text),
                helperName, distressedName, String.valueOf(distance), distressedLat, distressedLon);
        Log.d(TAG, "Distress Message: " + distressMessage);
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(helperPhone, null, distressMessage, null, null);
            Log.d(TAG, "SMS sent to " + helperName + " successfully.");
            Toast.makeText(getApplicationContext(), String.format(getString(R.string.sms_sent_message), helperName),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.d(TAG, "SMS to " + helperName + " failed.");
            Toast.makeText(getApplicationContext(), String.format(getString(R.string.sms_failed_message), helperName),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void sendSMS(User distressedUser, Contact helpingContact) {
        Log.d(TAG, "Emergency Contact Send SMS");
        // String distressedPhone = distressedUser.getPhone();
        String distressedName = distressedUser.getInfo().getName();
        String distressedLat = distressedUser.getLocation().getLatitude();
        String distressedLon = distressedUser.getLocation().getLongitude();

        String helperPhone = helpingContact.getPhone();
        String helperName = helpingContact.getName();
        String distressMessage = String.format(getString(R.string.emergency_contact_distress_message_text),
                helperName, distressedName, distressedLat, distressedLon);
        Log.d(TAG, "Distress Message: " + distressMessage);
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(helperPhone, null, distressMessage, null, null);
            Log.d(TAG, "SMS sent to " + helperName + " successfully.");
            Toast.makeText(getApplicationContext(), String.format(getString(R.string.sms_sent_message), helperName),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.d(TAG, "SMS to " + helperName + " failed.");
            Toast.makeText(getApplicationContext(), String.format(getString(R.string.sms_failed_message), helperName),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_ALL: {

                if (grantResults.length > 0) {

                    List<Integer> indexesOfPermissionsNeededToShow = new ArrayList<>();
                    ArrayList<String> permissionsRequired = new ArrayList<>();
                    for(int i = 0; i < permissions.length; ++i) {
                        if(ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                            indexesOfPermissionsNeededToShow.add(i);
                        }
                    }

                    int size = indexesOfPermissionsNeededToShow.size();
                    if(size != 0) {
                        int i = 0;
                        boolean isPermissionGranted = true;

                        while(i < size && isPermissionGranted) {
                            isPermissionGranted = grantResults[indexesOfPermissionsNeededToShow.get(i)]
                                    == PackageManager.PERMISSION_GRANTED;
                            i++;
                        }

                        if(!isPermissionGranted) {
                            String title = "Permissions mandatory";
                            String message = "All the permissions are required for this app. Please grant the permissions to proceed.";

                            new AlertDialog.Builder(this)
                                    .setTitle(title)
                                    .setMessage(message)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            try {
                                                if(! hasPermissions(getApplicationContext(),
                                                        getPermissions(getApplicationContext()))) {
                                                    ActivityCompat.requestPermissions(MainActivity.this,
                                                            getPermissions(getApplicationContext()), PERMISSION_ALL);
                                                }
                                            } catch (PackageManager.NameNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                                    .setCancelable(false)
                                    .create()
                                    .show();
                        }
                    }
                }
            }
        }
    }

    public static String[] getPermissions(Context context)
            throws PackageManager.NameNotFoundException {
        PackageInfo info = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_PERMISSIONS);

        return info.requestedPermissions;
    }
}

