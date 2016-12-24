package com.teapink.damselindistress.activities;

import android.annotation.TargetApi;
import android.content.Context;
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
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teapink.damselindistress.R;
import com.teapink.damselindistress.models.Contact;
import com.teapink.damselindistress.models.User;
import com.teapink.damselindistress.utilities.AppCompatPreferenceActivity;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    private static User user = new User();
    private static ArrayList<Contact> contactArrayList;

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
                            Toast.makeText(preference.getContext(), "Entered user name not valid.", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        preference.setSummary(value.toString());
                        user.getInfo().setName(value.toString());
                        updateAllDB(user);
                        break;
                    case "prefPhone":
                        if (isPhoneInvalid(value.toString().trim())) {
                            flag = 1;
                            Toast.makeText(preference.getContext(), "Entered user phone not valid.", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        preference.setSummary(value.toString());
                        String oldPhone = user.getPhone();
                        user.setPhone(value.toString());
                        updateAllDB(oldPhone, user);
                        break;
                    case "prefAlert":
                        user.getLocation().setAlertAllowed(Boolean.valueOf(value.toString()));
                        updateAllDB(user);
                        break;
                    default:
                        break;
                }
                return flag == 0;
            }
            return true;
        }

        void updateAllDB(User user) {
            // update Firebase DB Online
            DatabaseReference databaseRef;
            databaseRef = FirebaseDatabase.getInstance().getReference();
            databaseRef.child("users").child(user.getPhone()).setValue(user.getInfo());
            databaseRef.child("location").child(user.getPhone()).setValue(user.getLocation());
        }

        void updateAllDB(String oldPhone, User user) {
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

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreference(findPreference("prefName"));
            bindPreference(findPreference("prefPhone"));
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

    private static boolean isNameInvalid(String name) {
        return name.matches(".*\\d+.*") || name.equals("");
    }

    private static boolean isPhoneInvalid(String phone) {
        return phone.trim().length() < 10 || phone.equals("");
    }
}
