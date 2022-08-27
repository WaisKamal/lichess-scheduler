package com.waisapps.lichessscheduler;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Date;
import java.util.TimeZone;

public class SettingsActivity extends BaseActivity {

    private Spinner themeSelect, tnrNotFinished, schedulingMode, scheduleHours, scheduleMins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize spinners
        themeSelect = findViewById(R.id.themeSelect);
        tnrNotFinished = findViewById(R.id.tnrNotFinished);
        schedulingMode = findViewById(R.id.schedulingMode);
        scheduleHours = findViewById(R.id.scheduleHours);
        scheduleMins = findViewById(R.id.scheduleMins);

        // Populate normal spinners
        populateSpinners();

        // Populate centered spinners
        ArrayAdapter<CharSequence> scheduleHoursAdapter = ArrayAdapter.createFromResource(this,
                R.array.hours, R.layout.centered_spinner);
        scheduleHoursAdapter.setDropDownViewResource(R.layout.centered_spinner);
        scheduleHours.setAdapter(scheduleHoursAdapter);

        ArrayAdapter<CharSequence> scheduleMinsAdapter = ArrayAdapter.createFromResource(this,
                R.array.minutes, R.layout.centered_spinner);
        scheduleMinsAdapter.setDropDownViewResource(R.layout.centered_spinner);
        scheduleMins.setAdapter(scheduleMinsAdapter);

        // Set spinner position before attaching listener
        // Default position is zero
        switch(getAppThemeSetting()) {
            case "light":
                themeSelect.setSelection(1);
                break;
            case "dark":
                themeSelect.setSelection(2);
                break;
        }
        themeSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences prefs = getSharedPreferences("com.waisapps.lichessscheduler.settings", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                switch(position) {
                    case 0:
                        editor.putString("appThemeSetting", "deviceDefault").commit();
                        if (activityTheme.equals("light") ^ !isDarkThemeEnabled()) {
                            recreate();
                        }
                        break;

                    case 1:
                        editor.putString("appThemeSetting", "light").commit();
                        if (!activityTheme.equals("light")) {
                            recreate();
                        }
                        editor.commit();
                        break;

                    case 2:
                        editor.putString("appThemeSetting", "dark").commit();
                        if (!activityTheme.equals("dark")) {
                            recreate();
                        }
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void populateSpinners() {
        int[] spinnerIds = {R.id.themeSelect, R.id.tnrNotFinished, R.id.schedulingMode};
        int[] entriesIds = {R.array.settings_theme, R.array.previous_tournament_not_finished, R.array.schedulingMode};
        for (int i = 0; i < spinnerIds.length; i++) {
            Spinner spinner = findViewById(spinnerIds[i]);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    entriesIds[i], R.layout.normal_spinner);
            adapter.setDropDownViewResource(R.layout.normal_spinner);
            spinner.setAdapter(adapter);
        }
    }
}