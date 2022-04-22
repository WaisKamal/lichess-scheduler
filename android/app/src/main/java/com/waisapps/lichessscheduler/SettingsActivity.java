package com.waisapps.lichessscheduler;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Date;
import java.util.TimeZone;

public class SettingsActivity extends AppCompatActivity {

    private Spinner scheduleHours, scheduleMins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize spinners
        scheduleHours = findViewById(R.id.scheduleHours);
        scheduleMins = findViewById(R.id.scheduleMins);

        // Populate spinners
        ArrayAdapter<CharSequence> scheduleHoursAdapter = ArrayAdapter.createFromResource(this,
                R.array.hours, R.layout.centered_spinner);
        scheduleHoursAdapter.setDropDownViewResource(R.layout.centered_spinner);
        scheduleHours.setAdapter(scheduleHoursAdapter);

        ArrayAdapter<CharSequence> scheduleMinsAdapter = ArrayAdapter.createFromResource(this,
                R.array.minutes, R.layout.centered_spinner);
        scheduleMinsAdapter.setDropDownViewResource(R.layout.centered_spinner);
        scheduleMins.setAdapter(scheduleMinsAdapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}