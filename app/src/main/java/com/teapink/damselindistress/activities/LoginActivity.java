package com.teapink.damselindistress.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.teapink.damselindistress.R;
import com.teapink.damselindistress.models.Contact;
import com.teapink.damselindistress.models.User;

import java.util.ArrayList;

/**
 * A login screen that offers login.
 */
public class LoginActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    private boolean ALREADY_LOGGED_IN = false;
    private SharedPreferences sharedPref;

    // UI references.
    private EditText mPhoneView, mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private User.Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mPhoneView = (EditText) findViewById(R.id.phone);
        mPasswordView = (EditText) findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.signInButton);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        // check if user is already logged in
        sharedPref = getSharedPreferences("LOGGED_USER", Context.MODE_PRIVATE);
        String currentUser = sharedPref.getString("current_user", null);
        if (currentUser != null) {
            ALREADY_LOGGED_IN = true;
            Gson gson = new Gson();
            User user = gson.fromJson(currentUser, User.class);
            showProgress(true);
            validateUser(user.getPhone(), user.getInfo().getPassword());
        }

        Button registerButton = (Button) findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
            }
        });

    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mPhoneView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String phone = mPhoneView.getText().toString();
        phone = phone.replaceAll("\\s", "");    // remove all spaces
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid phone.
        if (TextUtils.isEmpty(phone)) {
            mPhoneView.setError(getString(R.string.error_field_required));
            focusView = mPhoneView;
            cancel = true;
        } else if (!isPhoneValid(phone)) {
            mPhoneView.setError(getString(R.string.error_invalid_phone));
            focusView = mPhoneView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            validateUser(phone, password);
        }
    }

    private boolean isPhoneValid(String phone) {
        return phone.length() >= 10;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 0;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void validateUser(final String userPhone, final String userPassword) {
        // validate user
        DatabaseReference databaseRef;
        databaseRef = FirebaseDatabase.getInstance().getReference().child("users").child(userPhone);
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final User.Info info = dataSnapshot.getValue(User.Info.class);
                if (info.getPassword().equals(userPassword)) {
                    Log.d(TAG, "User Verified!");

                    if(!ALREADY_LOGGED_IN) {
                        // new user - save user details in shared pref
                        // get user location from Firebase
                        DatabaseReference dbRef;
                        dbRef = FirebaseDatabase.getInstance().getReference().child("location")
                                .child(userPhone);
                        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                location = dataSnapshot.getValue(User.Location.class);
                                location.setAlertAllowed(true); // user is logging in to the app first time
                                updateAllDB(userPhone, info, location);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                location = new User.Location();
                                location.setAlertAllowed(true); // user is logging in to the app first time
                                updateAllDB(userPhone, info, location);
                            }
                        });

                    }

                    showProgress(false);
                    finish();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "Firebase Error: " + databaseError.getMessage());
            }
        });
    }

    private void updateAllDB(String userPhone, User.Info info, User.Location location) {
        // initialise shared pref
        User user = new User(userPhone, info, location);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);
        editor.putString("current_user", json);
        editor.apply();

        // initialise settings pref
        SharedPreferences settingsPref = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor settingsEditor = settingsPref.edit();
        settingsEditor.putString("prefName", user.getInfo().getName());
        settingsEditor.putString("prefPhone", user.getPhone());
        settingsEditor.putBoolean("prefAlert", user.getLocation().hasAlertAllowed());
        settingsEditor.apply();

        // update Firebase database
        // new user - subscribe to SMS alerts
        // do not do if ALREADY_LOGGED_IN - they might have opted out of the alerts in settings
        DatabaseReference dbRef;
        dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("location").child(user.getPhone()).setValue(user.getLocation());

        getEmergencyContacts(userPhone);    // retrieve all emergency contacts
    }

    private void getEmergencyContacts(String userPhone) {
        final ArrayList<Contact> emergencyContactList = new ArrayList<>();
        DatabaseReference databaseRef;
        databaseRef = FirebaseDatabase.getInstance().getReference().child("emergencyList").child(userPhone);
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Contact contact = postSnapshot.getValue(Contact.class);
                    emergencyContactList.add(contact);
                }
                // add to shared preference
                Gson gson = new Gson();
                String jsonArrayList = gson.toJson(emergencyContactList);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("contact_list", jsonArrayList);
                editor.apply();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "Firebase Error: " + databaseError.getMessage());
            }
        });
    }

}