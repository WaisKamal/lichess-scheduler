package com.waisapps.lichessscheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

public class SchedulingWorker extends Worker {

    private final RequestQueue queue;

    public SchedulingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        queue = Volley.newRequestQueue(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            logAttempt("Running worker...");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Context context = getApplicationContext();
        // Log the time
        SharedPreferences prefs = context.getSharedPreferences("com.waisapps.lichessscheduler.userinfo",
                Context.MODE_PRIVATE);
        String currentVal = prefs.getString("timelog", "");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("timelog", currentVal + " " + new Date().getTime());
        editor.apply();
        // Read tournaments
        File dataDir = new File(context.getFilesDir(), "data");
        File[] tokens = dataDir.listFiles();
        // Exit if no tokens found
        // NOTE: THIS PART SHOULD BE REFACTORED TO USE TournamentFileManager
        if (tokens == null) {
            try {
                logAttempt("No tokens found");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Result.success();
        }
        for (File tokenFolder : tokens) {
            String token = tokenFolder.getName();
            File tnrDir = new File(tokenFolder, "tournaments");
            if (!tnrDir.exists()) {
                try {
                    logAttempt("Tournament directory not found");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return Result.success();
            }
            File[] tnrFiles = tnrDir.listFiles();
            if (tnrFiles == null) {
                try {
                    logAttempt("No tournaments found");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return Result.success();
            }
            try {
                Scheduler scheduler = new Scheduler(context, token);
                logAttempt(String.format("Scheduling tournaments for user %s\n", token));
                for (File file : tnrFiles) {
                    FileInputStream tnrFileStream = new FileInputStream(file);
                    byte[] data = new byte[(int) file.length()];
                    tnrFileStream.read(data);
                    tnrFileStream.close();
                    JSONObject tnrData = new JSONObject(new String(data));
                    Date now = new Date();
                    // NOTE: the below code can be refactored to use the new method
                    // getTournamentState() in Scheduler
                    // Check whether scheduling is enabled for this tournament
                    if (!tnrData.getJSONObject("status").getBoolean("schedulingEnabled")) {
                        logAttempt("Scheduling is disabled for this tournament");
                        continue;
                    }
                    // Check whether tournament should be scheduled today
                    if (!tnrData.getJSONArray("scheduleOn").getBoolean(now.getDay())) {
                        logAttempt("Tournament not set to be scheduled today");
                        continue;
                    }
                    // Check whether tournament was scheduled today
                    long lastScheduledDate = tnrData.getJSONObject("status").getLong("lastCreated");
                    if (isSameDay(lastScheduledDate, now)) {
                        logAttempt("This tournament was already scheduled today");
                        continue;
                    }
                    scheduler.fetchWinnersAndSchedule(token, tnrData, queue);
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
        return Result.success();
    }

    private boolean isSameDay(long timestamp, Date date) {
        Date tsDate = new Date(timestamp);
        return tsDate.getDate() == date.getDate()
                && tsDate.getMonth() == date.getMonth()
                && tsDate.getYear() == date.getYear();
    }

    private void logAttempt(String data) throws IOException {
        File logFile = new File(getApplicationContext().getFilesDir(), "worker_log.txt");
        FileOutputStream fos = new FileOutputStream(logFile, true);
        String now = new Date().toString();
        fos.write(String.format("%s - %s\n", now, data).getBytes(StandardCharsets.UTF_8));
        fos.close();
    }
}
