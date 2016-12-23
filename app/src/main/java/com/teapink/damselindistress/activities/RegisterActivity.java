package com.teapink.damselindistress.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
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

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.teapink.damselindistress.R;
import com.teapink.damselindistress.application.AppController;
import com.teapink.damselindistress.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.teapink.damselindistress.application.AppController.START_PHONE_VERIFICATION_URL;
import static com.teapink.damselindistress.application.AppController.TWILIO_API_KEY;
import static com.teapink.damselindistress.application.AppController.VERIFY_CODE_URL;

/**
 * A login screen that offers login.
 */
public class RegisterActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    private SharedPreferences sharedPref;

    // UI references.
    private EditText mNameView, mPhoneView, mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    Dialog verifyPhoneDialog;

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
        String name = mNameView.getText().toString();
        String phone = mPhoneView.getText().toString();
        String password = mPasswordView.getText().toString();

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
            showProgress(true);
            verifyPhone(name, phone, password);
        }
    }

    private boolean isNameValid(String name) {
        return !name.matches(".*\\d+.*");
    }

    private boolean isPhoneValid(String phone) {
        return phone.trim().length() >= 10 || !phone.equals("");
    }

    private boolean isPasswordValid(String password) {
        return password.trim().length() > 0 || !password.equals("");
    }

    private boolean isCodeValid(String code) {
        return code.trim().length() > 0 || !code.equals("");
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

    void verifyPhone(final String userName, final String userPhone, final String userPassword) {

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
                                verifyPhoneDialog = new Dialog(RegisterActivity.this);
                                verifyPhoneDialog.setContentView(R.layout.verify_phone_dialog);
                                verifyPhoneDialog.setTitle("Verify your mobile number");
                                verifyPhoneDialog.show();

                                showProgress(false);

                                Button verifyBtn = (Button) verifyPhoneDialog.findViewById(R.id.verifyBtn);
                                final EditText verCodeEditText = (EditText) verifyPhoneDialog.findViewById(R.id.verCodeEditText);

                                verifyBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        String verCode = verCodeEditText.getText().toString().trim();
                                        if (isCodeValid(verCode)) {
                                            showProgress(true);
                                            verifyOTP(userName, userPhone, userPassword, verCode);
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Please enter valid OTP",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

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
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq);
    }

    void verifyOTP(final String userName, final String userPhone, final String userPassword, String verCode) {

        String urlString = VERIFY_CODE_URL + "?api_key=" + TWILIO_API_KEY +
                "&phone_number=" + userPhone +
                "&country_code=" + "91" +
                "&verification_code=" + verCode;

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                urlString, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Response: " + response.toString());

                        try {
                            boolean success = response.getBoolean("success");
                            if (success) {
                                Toast.makeText(getApplicationContext(), "OTP Verified.",
                                        Toast.LENGTH_SHORT).show();
                                verifyPhoneDialog.cancel();
                                registerUser(userName, userPhone, userPassword);
                            } else {
                                showProgress(false);
                                Toast.makeText(getApplicationContext(), "Incorrect OTP. Try again.",
                                        Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq);
    }

    void registerUser(String userName, String userPhone, String userPassword) {
        // register user
        User user = new User(userPhone,
                new User.Info(userName, userPassword),
                new User.Location());

        DatabaseReference databaseRef;
        databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child("users").child(userPhone).setValue(user.getInfo());
        databaseRef.child("location").child(userPhone).setValue(user.getLocation());

        sharedPref = getSharedPreferences("USER_LOGIN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);
        editor.putString("current_user", json);
        editor.commit();

        finish();
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
    }

}

