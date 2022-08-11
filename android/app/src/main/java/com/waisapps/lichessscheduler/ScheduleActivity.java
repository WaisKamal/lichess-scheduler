package com.waisapps.lichessscheduler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ScheduleActivity extends AppCompatActivity {

    public static class TournamentStartTimeComparator implements Comparator<TournamentItem> {
        @Override
        public int compare(TournamentItem tnr1, TournamentItem tnr2) {
            try {
                return tnr1.data.getString("startTime").compareTo(tnr2.data.getString("startTime"));
            } catch (JSONException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }

    // The current token
    String token;

    // The request queue
    RequestQueue queue;

    // Tournament file manager
    private TournamentFileManager tournamentFileManager;

    private CoordinatorLayout parent;
    private TextView labelNoItem;
    private FloatingActionButton btnAdd;

    private RecyclerView tnrContainer;
    private TournamentAdapter tnrAdapter;
    ArrayList<TournamentItem> tournaments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize current token
        token = getSharedPreferences("com.waisapps.lichessscheduler.userinfo", MODE_PRIVATE)
                .getString("token", "");

        // Initialize request queue
        queue = Volley.newRequestQueue(this);

        // Initialize tournament file manager
        tournamentFileManager = new TournamentFileManager(getFilesDir());

        // Initialize coordinator layout
        parent = findViewById(R.id.container);

        // Initialize no item label
        labelNoItem = findViewById(R.id.labelNoItem);

        // Initialize container
        tnrContainer = findViewById(R.id.tnrContainer);

        // Initialize floating action button
        btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScheduleActivity.this, EditActivity.class);
                intent.putExtra(IntentConstants.FROM_ACTIVITY, "ScheduleActivity");
                startActivityForResult(intent, 1);
            }
        });

        try {
            tournaments = getTournaments();
            tnrAdapter = new TournamentAdapter(tournaments, this, token, queue);
            tnrAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    if (tournaments.size() > 0) {
                        labelNoItem.setVisibility(View.GONE);
                    } else {
                        labelNoItem.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onChanged() {
                    if (tournaments.size() > 0) {
                        labelNoItem.setVisibility(View.GONE);
                    } else {
                        labelNoItem.setVisibility(View.VISIBLE);
                    }
                }
            });
            tnrContainer.setAdapter(tnrAdapter);
            tnrContainer.setLayoutManager(new LinearLayoutManager(this));
            if (tnrAdapter.getItemCount() == 0) {
                labelNoItem.setVisibility(View.VISIBLE);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            tournaments = getTournaments();
            tnrAdapter.refreshAdapter(tournaments);
            super.onActivityResult(requestCode, resultCode, data);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public ArrayList<TournamentItem> getTournaments() throws IOException, JSONException {
        ArrayList<TournamentItem> tournaments = new ArrayList<>();
        File[] tnrFiles = tournamentFileManager.getTournamentsByToken(token);
        if (tnrFiles != null) {
            for (File file : tnrFiles) {
                String fileText = tournamentFileManager.getTournamentJSON(token, file.getName());
                tournaments.add(new TournamentItem(new JSONObject(fileText)));
            }
            // Sort tournaments by start time
            Collections.sort(tournaments, new TournamentStartTimeComparator());
        } else {
            labelNoItem.setVisibility(View.VISIBLE);
        }
        return tournaments;
    }
}