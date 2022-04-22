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
        if (tokens == null) return Result.success();
        for (File tokenFolder : tokens) {
            File tnrDir = new File(tokenFolder, "tournaments");
            if (!tnrDir.exists()) return Result.success();
            File[] tnrFiles = tnrDir.listFiles();
            if (tnrFiles == null) return Result.success();

            try {
                Scheduler scheduler = new Scheduler(context, tokenFolder.getName());
                for (File file : tnrFiles) {
                    FileInputStream tnrFileStream = new FileInputStream(file);
                    byte[] data = new byte[(int) file.length()];
                    tnrFileStream.read(data);
                    tnrFileStream.close();
                    JSONObject tnrData = new JSONObject(new String(data));
                    Date now = new Date();
                    // Check whether tournament should be scheduled today
                    if (!tnrData.getJSONArray("scheduleOn").getBoolean(now.getDay())) continue;
                    // Check whether tournament was scheduled today
                    int lastScheduledDate = tnrData.getJSONObject("status").getInt("lastCreated");
                    if (isSameDay(lastScheduledDate, now)) continue;
                    // Schedule tournament
                    // scheduler.scheduleTournament(tnrData, queue);
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
        return Result.success();
    }

    private boolean isSameDay(int timestamp, Date date) {
        Date tsDate = new Date(timestamp);
        return tsDate.getDate() == date.getDate()
                && tsDate.getMonth() == date.getMonth()
                && tsDate.getYear() == date.getYear();
    }
}
