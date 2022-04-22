package com.waisapps.lichessscheduler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar toolbar;
    private ConstraintLayout btnNew;
    private ConstraintLayout btnSchedule;
    private ConstraintLayout btnPlayers;
    private ConstraintLayout btnSettings;
    private ConstraintLayout btnLogOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");

        CardView buttonNew = findViewById(R.id.cardView3);
        buttonNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                startActivity(intent);
            }
        });

        btnNew = findViewById(R.id.btnNew);
        btnSchedule = findViewById(R.id.btnSchedule);
        btnPlayers = findViewById(R.id.btnPlayers);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogOut = findViewById(R.id.btnLogout);

        btnSchedule.setOnClickListener(this);
        btnNew.setOnClickListener(this);
        btnPlayers.setOnClickListener(this);
        btnSettings.setOnClickListener(this);
        btnLogOut.setOnClickListener(this);

        // Get the current user from SharedPreferences
        String username = getSharedPreferences("com.waisapps.lichessscheduler.userinfo", MODE_PRIVATE)
                .getString("username", "Anonymous");
        // currentUser.setText(username);

        // Create work request
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest schedulingWorkRequest = new PeriodicWorkRequest
                .Builder(SchedulingWorker.class, 15, TimeUnit.MINUTES)
                .addTag("SchedulingWorker")
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("SchedulingWorker",
                 ExistingPeriodicWorkPolicy.KEEP, schedulingWorkRequest);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch(v.getId()) {
            case R.id.btnNew:
                intent = new Intent(this, EditActivity.class);
                startActivity(intent);
                break;
            case R.id.btnSchedule:
                intent = new Intent(this, ScheduleActivity.class);
                startActivity(intent);
                break;
            case R.id.btnPlayers:
                intent = new Intent(this, PlayersActivity.class);
                startActivity(intent);
                break;
            case R.id.btnSettings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.btnLogout:
                SharedPreferences prefs = getSharedPreferences("com.waisapps.lichessscheduler.userinfo", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("username");
                editor.remove("token");
                editor.apply();
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}