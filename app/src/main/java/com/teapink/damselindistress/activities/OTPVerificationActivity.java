package com.teapink.damselindistress.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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
import com.teapink.damselindistress.R;
import com.teapink.damselindistress.application.AppController;

import org.json.JSONException;
import org.json.JSONObject;

import static com.teapink.damselindistress.application.AppController.SOCKET_TIMEOUT_MS;
import static com.teapink.damselindistress.application.AppController.TWILIO_API_KEY;
import static com.teapink.damselindistress.application.AppController.VERIFY_CODE_URL;

public class OTPVerificationActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    private String userPhone;
    private String callingActivity;
    private View verificationFormView;
    private View progressView;
    private TextView invalidPhoneView;
    private EditText verCodeEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        verificationFormView = findViewById(R.id.verificationFormView);
        progressView = findViewById(R.id.loginProgress);
        invalidPhoneView = (TextView) findViewById(R.id.invalidPhoneView);
        Button verifyBtn = (Button) findViewById(R.id.verifyBtn);
        verCodeEditText = (EditText) findViewById(R.id.verCodeEditText);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            userPhone = bundle.getString("USER_PHONE", null);
            callingActivity = bundle.getString("CALLING_ACTIVITY", null);

            verifyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String verCode = verCodeEditText.getText().toString();
                    if (isCodeValid(verCode) && userPhone != null) {
                        verifyOTP(verCode);
                    } else {
                        verificationFormView.setVisibility(View.GONE);
                        invalidPhoneView.setVisibility(View.VISIBLE);
                    }
                }
            });

        } else {
            verificationFormView.setVisibility(View.GONE);
            invalidPhoneView.setVisibility(View.VISIBLE);
        }


    }

    private void verifyOTP(String verCode) {

        showProgress(true);

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
                                Toast.makeText(getApplicationContext(), R.string.otp_verification_success_message,
                                        Toast.LENGTH_SHORT).show();
                                if (callingActivity != null && callingActivity.equals("SettingsActivity")) {
                                    // update the settings preference
                                    SharedPreferences settingsPref = PreferenceManager
                                            .getDefaultSharedPreferences(getApplicationContext());
                                    SharedPreferences.Editor settingsEditor = settingsPref.edit();
                                    settingsEditor.putString("prefPhone", userPhone);
                                    settingsEditor.commit();
                                }
                                Intent intent = new Intent();
                                intent.putExtra("NEW_PHONE", userPhone);
                                setResult(RESULT_OK, intent);
                                finish();
                            } else {
                                showProgress(false);
                                Toast.makeText(getApplicationContext(), R.string.otp_verification_error_message,
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
                if (error instanceof TimeoutError)
                    VolleyLog.d(TAG, "Error in " + TAG + " : " + "Timeout Error");
                showProgress(false);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            verificationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            verificationFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    verificationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private boolean isCodeValid(String code) {
        return code.trim().length() > 0 || !code.equals("");
    }

}
