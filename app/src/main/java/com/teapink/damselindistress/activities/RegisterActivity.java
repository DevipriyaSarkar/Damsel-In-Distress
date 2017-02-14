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
import com.teapink.damselindistress.R;
import com.teapink.damselindistress.application.AppController;
import com.teapink.damselindistress.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.teapink.damselindistress.application.AppController.SOCKET_TIMEOUT_MS;
import static com.teapink.damselindistress.application.AppController.START_PHONE_VERIFICATION_URL;
import static com.teapink.damselindistress.application.AppController.TWILIO_API_KEY;

/**
 * A login screen that offers login.
 */
public class RegisterActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    private final int REQUEST_VERIFICATION = 2000;

    // UI references.
    private EditText mNameView, mPhoneView, mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private String name, phone, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Set up the login form.
        mNameView = (EditText) findViewById(R.id.name);
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

        Button mSignUpButton = (Button) findViewById(R.id.signUpButton);
        mSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
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
        mNameView.setError(null);
        mPhoneView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        name = mNameView.getText().toString().trim();
        phone = mPhoneView.getText().toString().trim();
        phone = phone.replaceAll("\\s", "");    // remove all spaces
        password = mPasswordView.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid name.
        if (TextUtils.isEmpty(name)) {
            mNameView.setError(getString(R.string.error_field_required));
            focusView = mNameView;
            cancel = true;
        } else if (!isNameValid(name)) {
            mNameView.setError(getString(R.string.error_invalid_name));
            focusView = mNameView;
            cancel = true;
        }

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
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPhoneView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
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
            checkIfPhoneAvailable(phone);
        }
    }

    private boolean isNameValid(String name) {
        return !name.matches(".*\\d+.*");
    }

    private boolean isPhoneValid(String phone) {
        return phone.trim().length() >= 10;
    }

    private boolean isPasswordValid(String password) {
        return password.trim().length() > 0;
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

    private void checkIfPhoneAvailable(final String phone) {
        showProgress(true);
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
                    showProgress(false);
                    Log.d(TAG, "Phone number exists.");
                    Toast.makeText(getApplicationContext(), R.string.phone_exists_error, Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Phone number available.");
                    verifyPhone(phone);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "Firebase Error: " + databaseError.getMessage());
            }
        });
    }

    private void verifyPhone(final String userPhone) {

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
                        Log.d(TAG, "Response: " + response.toString());

                        try {
                            boolean success = response.getBoolean("success");
                            if (success) {
                                showProgress(false);
                                Intent intent = new Intent(getApplicationContext(), OTPVerificationActivity.class);
                                intent.putExtra("USER_PHONE", userPhone);
                                startActivityForResult(intent, REQUEST_VERIFICATION);
                            } else {
                                showProgress(false);
                                mPhoneView.setError(getString(R.string.error_incorrect_credentials));
                                mPhoneView.requestFocus();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d(TAG, "JSON Error: " + e.getMessage());
                            showProgress(false);
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error in " + TAG + " : " + error.getMessage());
                showProgress(false);
                if (error instanceof TimeoutError)
                    VolleyLog.d(TAG, "Error in " + TAG + " : " + "Timeout Error");
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void registerUser(String userName, String userPhone, String userPassword) {
        // register user
        User user = new User(userPhone,
                new User.Info(userName, userPassword),
                new User.Location());

        DatabaseReference databaseRef;
        databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child("users").child(userPhone).setValue(user.getInfo());
        databaseRef.child("location").child(userPhone).setValue(user.getLocation());

        SharedPreferences sharedPref = getSharedPreferences("LOGGED_USER", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);
        editor.putString("current_user", json);
        editor.commit();

        // initialise settings pref
        SharedPreferences settingsPref = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor settingsEditor = settingsPref.edit();
        settingsEditor.putString("prefName", user.getInfo().getName());
        settingsEditor.putString("prefPhone", user.getPhone());
        settingsEditor.putBoolean("prefAlert", user.getLocation().hasAlertAllowed());
        settingsEditor.apply();

        finish();
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_VERIFICATION) {
            if (resultCode == RESULT_OK) {
                if (name != null && phone != null && password != null) {
                    registerUser(name, phone, password);
                }
                Log.d(TAG, "OnActivityResult RESULT_OK");
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(),
                        R.string.phone_verification_cancelled, Toast.LENGTH_LONG).show();
                Log.d(TAG, "OnActivityResult RESULT_CANCELED");
            }
        }
    }
}

