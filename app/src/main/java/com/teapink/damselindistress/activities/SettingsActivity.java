package com.teapink.damselindistress.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

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
import com.teapink.damselindistress.utilities.AppCompatPreferenceActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.teapink.damselindistress.application.AppController.SOCKET_TIMEOUT_MS;
import static com.teapink.damselindistress.application.AppController.START_PHONE_VERIFICATION_URL;
import static com.teapink.damselindistress.application.AppController.TWILIO_API_KEY;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    private final String TAG = this.getClass().getSimpleName();
    private static User user = new User();
    private static ArrayList<Contact> contactArrayList;
    private static final int REQUEST_VERIFICATION = 2000;
    private static  ProgressDialog pDialog;

    /**
     * A preference value change listener that updates the user details and summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreference
            = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            int flag = 0;
            if (user != null) {
                switch (preference.getKey()) {
                    case "prefName":
                        if (isNameInvalid(value.toString().trim())) {
                            flag = 1;
                            Toast.makeText(preference.getContext(), R.string.invalid_name_error, Toast.LENGTH_SHORT).show();
                            break;
                        }
                        preference.setSummary(value.toString());
                        user.getInfo().setName(value.toString());
                        updateFirebaseDB(user);
                        break;
                    case "prefPhone":
                        preference.setSummary(value.toString());
                        String oldPhone = user.getPhone();
                        user.setPhone(value.toString());
                        updateFirebaseDB(oldPhone, user);
                        break;
                    case "prefAlert":
                        user.getLocation().setAlertAllowed(Boolean.valueOf(value.toString()));
                        updateFirebaseDB(user);
                        break;
                    default:
                        break;
                }
                return flag == 0;
            }
            return true;
        }

    };

    private static Preference.OnPreferenceClickListener phonePreferenceClickListener
            = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(final Preference preference) {
            if (preference.getKey().equals("prefPhone")) {
                // ask for the new phone
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(preference.getContext());
                alertDialog.setTitle(R.string.phone_number_dialog_title);
                final EditText phoneInputEditText = new EditText(preference.getContext());
                phoneInputEditText.setInputType(InputType.TYPE_CLASS_PHONE);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                phoneInputEditText.setLayoutParams(lp);
                phoneInputEditText.setText(user.getPhone());
                phoneInputEditText.setSelectAllOnFocus(true);
                alertDialog.setView(phoneInputEditText);

                alertDialog.setPositiveButton(R.string.change_phone_positive_button_text,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String value = phoneInputEditText.getText().toString().trim();
                        value = value.replaceAll("\\s", "");    // remove all spaces
                        if (!value.equals(user.getPhone())) {   // if phone number changed
                            if (isPhoneValid(value)) {
                                checkIfPhoneAvailable(value, preference);
                            } else {
                                Toast.makeText(preference.getContext(), R.string.invalid_phone_error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

                alertDialog.setNegativeButton(R.string.change_phone_negative_button_text,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                alertDialog.show();
                return false;
            }
            return false;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     */
    private static void bindPreference(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreference);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof SwitchPreference) {
            sBindPreference.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), true));
        } else {
            sBindPreference.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        SharedPreferences sp = getSharedPreferences("LOGGED_USER", MODE_PRIVATE);
        String currentUser = sp.getString("current_user", null);
        if (currentUser != null) {
            Gson gson = new Gson();
            user = gson.fromJson(currentUser, User.class);
        }
        String jsonArrayList = sp.getString("contact_list", null);
        contactArrayList = new ArrayList<>();
        if (jsonArrayList != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
            contactArrayList = gson.fromJson(jsonArrayList, type);
        }

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || AlertPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            //initialize progress dialog
            pDialog = new ProgressDialog(getActivity());
            pDialog.setMessage(getString(R.string.verification_progress_dialog_message));
            pDialog.setCancelable(false);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreference(findPreference("prefName"));
            bindPreference(findPreference("prefPhone"));
            findPreference("prefPhone").setOnPreferenceClickListener(phonePreferenceClickListener);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AlertPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_alert);
            setHasOptionsMenu(true);

            // bind preference value change
            bindPreference(findPreference("prefAlert"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        // update shared preference
        SharedPreferences sharedPref = getSharedPreferences("LOGGED_USER", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);
        editor.putString("current_user", json);
        editor.apply();
        super.onPause();
    }

    // not really required now
/*    @Override
    protected void onPause() {
        // update shared preference
        SharedPreferences sharedPref = getSharedPreferences("LOGGED_USER", MODE_PRIVATE);

        SharedPreferences settingsPref = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        String updatedName = settingsPref.getString("prefName", null);
        String updatedPhone = settingsPref.getString("prefPhone", null);
        boolean updatedAlertChoice = settingsPref.getBoolean("prefAlert", true);

        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = new Gson();
        User updatedUser = user;    // get old user details

        // update changes from settings pref
        updatedUser.setPhone(updatedPhone);
        updatedUser.getInfo().setName(updatedName);
        updatedUser.getLocation().setAlertAllowed(updatedAlertChoice);

        String json = gson.toJson(updatedUser);
        editor.putString("current_user", json);
        editor.apply();
        super.onPause();
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_VERIFICATION) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "OnActivityResult RESULT_OK");
                String newPhone = data.getExtras().getString("NEW_PHONE");
                String oldPhone = user.getPhone();
                user.setPhone(newPhone);
                updateFirebaseDB(oldPhone, user);
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(),
                        R.string.phone_verification_cancelled, Toast.LENGTH_LONG).show();
                Log.d(TAG, "OnActivityResult RESULT_CANCELED");
            }
        }
    }

    private static boolean isNameInvalid(String name) {
        return name.matches(".*\\d+.*") || name.equals("");
    }

    private static boolean isPhoneValid(String phone) {
        return phone.trim().length() >= 10 && !phone.equals("");
    }

    private static void verifyPhone(final String userPhone, final Preference preference) {

        showPDialog();

        String urlString = START_PHONE_VERIFICATION_URL;

        Map<String, String> startParams = new HashMap<>();
        startParams.put("api_key", TWILIO_API_KEY);
        startParams.put("via", "sms");
        startParams.put("phone_number", userPhone);
        startParams.put("country_code", "91");
        startParams.put("locale", "en");

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                urlString, new JSONObject(startParams),
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("SettingsActivity", "Response: " + response.toString());

                        try {
                            boolean success = response.getBoolean("success");
                            if (success) {
                                hidePDialog();
                                Intent intent = new Intent(preference.getContext(), OTPVerificationActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putString("USER_PHONE", userPhone);
                                bundle.putString("CALLING_ACTIVITY", "SettingsActivity");
                                intent.putExtras(bundle);
                                Activity activity = (Activity) preference.getContext();
                                activity.startActivityForResult(intent, REQUEST_VERIFICATION);
                            } else {
                                hidePDialog();
                                Toast.makeText(preference.getContext(), R.string.phone_incorrect_error, Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("SettingsActivity", "JSON Error: " + e.getMessage());
                            hidePDialog();
                            Toast.makeText(preference.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("SettingsActivity", "Error in " + "SettingsActivity" + " : " + error.getMessage());
                if (error instanceof TimeoutError)
                    VolleyLog.d("SettingsActivity", "Timeout Error");
                hidePDialog();
                Toast.makeText(preference.getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
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

    private static void showPDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private static void hidePDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    private static void updateFirebaseDB(User user) {
        // update Firebase DB Online
        DatabaseReference databaseRef;
        databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child("users").child(user.getPhone()).setValue(user.getInfo());
        databaseRef.child("location").child(user.getPhone()).setValue(user.getLocation());
    }

    private static void updateFirebaseDB(String oldPhone, User user) {
        // update Firebase DB Online
        DatabaseReference databaseRef;
        databaseRef = FirebaseDatabase.getInstance().getReference();
        // remove children with old phone
        databaseRef.child("users").child(oldPhone).removeValue();
        databaseRef.child("location").child(oldPhone).removeValue();
        databaseRef.child("emergencyList").child(oldPhone).removeValue();
        // add children with new phone
        databaseRef.child("users").child(user.getPhone()).setValue(user.getInfo());
        databaseRef.child("location").child(user.getPhone()).setValue(user.getLocation());
        for (Contact contact : contactArrayList)
            databaseRef.child("emergencyList").child(user.getPhone()).push().setValue(contact);
    }

    private static void checkIfPhoneAvailable(final String phone, final Preference preference) {
        showPDialog();
        DatabaseReference databaseRef;
        databaseRef = FirebaseDatabase.getInstance().getReference().child("users");
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int flag = 0;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    String p = postSnapshot.getKey();
                    if (p.equals(phone)){
                        flag = 1;
                        break;
                    }
                }
                if (flag == 1) {
                    hidePDialog();
                    Log.d("SettingsActivity", "Phone number exists.");
                    Toast.makeText(preference.getContext(), R.string.phone_exists_error, Toast.LENGTH_SHORT).show();
                } else {
                    hidePDialog();
                    Log.d("SettingsActivity", "Phone number available.");
                    verifyPhone(phone, preference);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("SettingsActivity", "Firebase Error: " + databaseError.getMessage());
            }
        });
    }


}
