package com.waisapps.lichessscheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ConstraintLayout container;
    private EditText tokenInput;
    private TextView errText;
    private CheckBox saveToken;
    private Button btnLogin;
    private ProgressBar loader;

    // Define request queue
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        container = findViewById(R.id.container);
        tokenInput = findViewById(R.id.tokenInput);
        errText = findViewById(R.id.errText);
        saveToken = findViewById(R.id.saveToken);
        btnLogin = findViewById(R.id.btnLogin);
        loader = findViewById(R.id.loginLoader);

        // Initialize request queue
        queue = Volley.newRequestQueue(this);

        String token = getSharedPreferences("com.waisapps.lichessscheduler.userinfo", MODE_PRIVATE)
                .getString("savedToken", "");
        tokenInput.setText(token, TextView.BufferType.EDITABLE);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    login();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void login() throws JSONException {
        if (!validateToken(tokenInput.getText().toString())) return;

        // Show the loader
        loader.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        errText.setVisibility(View.GONE);

        JSONObject data = new JSONObject();
        String token = tokenInput.getText().toString();
        data.put("token", token);
        String url = "https://lichess.org/api/account";
        JsonObjectRequest jsonRequest = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // Save the username to SharedPreferences
                SharedPreferences prefs = LoginActivity.this
                        .getSharedPreferences("com.waisapps.lichessscheduler.userinfo", Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = prefs.edit();
                try {
                    editor.putString("username", response.getString("username"));
                } catch (JSONException e) {
                    e.printStackTrace();
            }
                editor.putString("token", token);
                if (saveToken.isChecked()) {
                    editor.putString("savedToken", token);
                }
                editor.apply();

                JsonObjectRequest loginRequest = new JsonObjectRequest("http://lichess-scheduler.000webhostapp.com/login.php?token=" + token,
                        null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        loader.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Snackbar.make(container, "An error occurred", Snackbar.LENGTH_LONG)
                                .setAction("RETRY", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        try {
                                            login();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                })
                                .setActionTextColor(0xFFE64A19)
                                .show();
                    }
                });
                queue.add(loginRequest);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                loader.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                try {
                    if (error.networkResponse.statusCode == 401) {
                        errText.setVisibility(View.VISIBLE);
                        loader.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        return;
                    }
                } catch (Exception e) {}

                Snackbar.make(container, "An error occurred", Snackbar.LENGTH_LONG)
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    login();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setActionTextColor(0xFFE64A19)
                        .show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        queue.add(jsonRequest);

        /*
        if (token.equals("Veiw1IRs4pFBeLnZ")) {
            // Save the username to SharedPreferences
            SharedPreferences prefs = LoginActivity.this
                    .getSharedPreferences("com.waisapps.lichessscheduler.userinfo", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", "SudaneseChessPlayers");
            editor.putString("token", token);
            if (saveToken.isChecked()) {
                editor.putString("savedToken", token);
            }
            editor.apply();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }*/
    }

    public boolean validateToken(String token) {
        if (token.length() == 0) {
            Toast.makeText(this, "Please input your access token", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}