package com.waisapps.lichessscheduler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditActivity extends AppCompatActivity implements MenuItem.OnMenuItemClickListener,
        SeekBar.OnSeekBarChangeListener {

    private static final int PERMISSION_REQUEST_CODE = 5;

    // Intent constants
    private static final String FROM_ACTIVITY = "com.waisapps.lichessscheduler.fromActiviy";
    private static final String ID = "com.waisapps.lichessscheduler.id";
    private static final String TEAM = "com.waisapps.lichessscheduler.team";

    // Current access token and username
    String token;
    String username;

    // Current tournament id
    String currentTnrId = "";

    // The current team id
    String currentTeamId = "";

    // Request queue
    RequestQueue requestQueue;

    // Menu buttons
    private MenuItem menuPlayers;

    // Frame layout
    private FrameLayout frameLayout;

    // Fields
    private static CardView startLoader;
    private TextView tnrInitialTimeText, tnrIncrementText, tnrRoundsText, tnrBreaksText,
            tnrDurationText, tnrTeamsText, tnrTeamBattleTeamsText, tnrLeadersText,
            tnrMinRatingText, tnrMaxRatingText, tnrRatedGamesText, tnrForbiddenPairingsText,
            tnrChatAccessText;
    private Switch tnrRated, tnrBerserk, tnrStreaks, tnrChatroom;
    private EditText tnrName, tnrDisplayName, tnrStartPos, tnrDescr, tnrPassword,
            tnrTeamBattleTeams, tnrForbiddenPairings;
    private Spinner tnrType, tnrVariant, tnrTeams, tnrChatAccess, tnrStartTimeHrs, tnrStartTimeMins;
    private SeekBar tnrInitialTime, tnrIncrement, tnrRounds, tnrBreaks, tnrDuration, tnrLeaders,
            tnrMinRating,tnrMaxRating, tnrRatedGames;
    private CheckBox[] checkBoxes = new CheckBox[7];
    private Button btnSave;
    private ProgressBar loader;

    // Seekbar options
    private List<String> initialTimeStops, incrementStops, breaksStops, durationStops,
            ratedGamesStops;

    // Seekbar values;
    private String[] tnrTypeValues, tnrVariantValues;
    private int[] tnrInitialTimeValues, tnrIncrementValues, tnrDurationValues, tnrBreaksValues,
            tnrRatedGamesValues;

    // Teams valid in the tournament by team field
    private ArrayList<String> teamIds = new ArrayList<>();
    private ArrayList<String> teamNames = new ArrayList<>();

    // The scheduling status of the current tournament
    JSONObject tnrStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize request queue
        requestQueue = Volley.newRequestQueue(this);

        // Initialize current token and username
        token = getSharedPreferences("com.waisapps.lichessscheduler.userinfo", MODE_PRIVATE).getString("token", "");
        username = getSharedPreferences("com.waisapps.lichessscheduler.userinfo", MODE_PRIVATE).getString("username", "");

        // Initialize frame layout
        frameLayout = findViewById(R.id.frameLayout);

        // Initialize labels
        tnrInitialTimeText = findViewById(R.id.tnrInitialTimeText);
        tnrIncrementText = findViewById(R.id.tnrIncrementText);
        tnrRoundsText = findViewById(R.id.tnrRoundsText);
        tnrBreaksText = findViewById(R.id.tnrBreaksText);
        tnrDurationText = findViewById(R.id.tnrDurationText);
        tnrTeamsText = findViewById(R.id.tnrTeamsText);
        tnrTeamBattleTeamsText = findViewById(R.id.tnrTeamBattleTeamsText);
        tnrLeadersText = findViewById(R.id.tnrLeadersText);
        tnrMinRatingText = findViewById(R.id.tnrMinRatingText);
        tnrMaxRatingText = findViewById(R.id.tnrMaxRatingText);
        tnrRatedGamesText = findViewById(R.id.tnrRatedGamesText);
        tnrChatAccessText = findViewById(R.id.tnrChatAccessText);
        tnrForbiddenPairingsText = findViewById(R.id.tnrForbiddenPairingsText);

        // Initialize fields
        tnrName = findViewById(R.id.tnrName);
        tnrDisplayName = findViewById(R.id.tnrDisplayName);
        tnrType = findViewById(R.id.tnrType);
        tnrVariant = findViewById(R.id.tnrVariant);
        tnrRated = findViewById(R.id.tnrRated);
        tnrInitialTime = findViewById(R.id.tnrInitialTime);
        tnrIncrement = findViewById(R.id.tnrIncrement);
        tnrRounds = findViewById(R.id.tnrRounds);
        tnrBreaks = findViewById(R.id.tnrBreaks);
        tnrDuration = findViewById(R.id.tnrDuration);
        tnrStartPos = findViewById(R.id.tnrStartPos);
        tnrDescr = findViewById(R.id.tnrDescr);
        tnrTeams = findViewById(R.id.tnrTeams);
        tnrPassword = findViewById(R.id.tnrPassword);
        tnrTeamBattleTeams = findViewById(R.id.tnrTeamBattleTeams);
        tnrLeaders = findViewById(R.id.tnrLeaders);
        tnrMinRating = findViewById(R.id.tnrMinRating);
        tnrMaxRating = findViewById(R.id.tnrMaxRating);
        tnrRatedGames = findViewById(R.id.tnrRatedGames);
        tnrBerserk = findViewById(R.id.tnrBerserk);
        tnrStreaks = findViewById(R.id.tnrStreaks);
        tnrChatroom = findViewById(R.id.tnrChatroom);
        tnrChatAccess = findViewById(R.id.tnrChatAccess);
        // Set default position for tnrChatAccess
        tnrChatAccess.setSelection(2);
        tnrForbiddenPairings = findViewById(R.id.tnrForbiddenPairings);
        tnrStartTimeHrs = findViewById(R.id.tnrStartTimeHrs);
        tnrStartTimeMins = findViewById(R.id.tnrStartTimeMins);

        btnSave = findViewById(R.id.btnSave);
        loader = findViewById(R.id.saveLoader);
        startLoader = findViewById(R.id.loader);

        // Initialize checkboxes array
        for (int i = 0; i < 7; i++) {
            int id = getResources().getIdentifier("cb" + (i + 1), "id", getPackageName());
            checkBoxes[i] = findViewById(id);
        }

        // Initialize seekbar options
        initialTimeStops = Arrays.asList(getResources().getStringArray(R.array.initial_time));
        incrementStops = Arrays.asList(getResources().getStringArray(R.array.increment));
        breaksStops = Arrays.asList(getResources().getStringArray(R.array.time_between_rounds));
        durationStops = Arrays.asList(getResources().getStringArray(R.array.duration));
        ratedGamesStops = Arrays.asList(getResources().getStringArray(R.array.rated_games));

        // Initialize seekbar and spinner values
        tnrTypeValues = getResources().getStringArray(R.array.tournament_type_values);
        tnrVariantValues = getResources().getStringArray(R.array.variant_values);
        tnrInitialTimeValues = getResources().getIntArray(R.array.initial_time_values);
        tnrIncrementValues = getResources().getIntArray(R.array.increment_values);
        tnrDurationValues = getResources().getIntArray(R.array.duration_values);
        tnrBreaksValues = getResources().getIntArray(R.array.time_between_rounds_values);
        tnrRatedGamesValues = getResources().getIntArray(R.array.rated_games_values);

        // Add "Not specified" to teams spinner
        // The corresponding team ID is the empty string
        teamIds.add("");
        teamNames.add("Not specified");

        ArrayAdapter<String> tnrTeamsAdapter = new ArrayAdapter<>(EditActivity.this,
                R.layout.support_simple_spinner_dropdown_item, teamNames);
        tnrTeams.setAdapter(tnrTeamsAdapter);

        ArrayAdapter<CharSequence> tnrStartTimeHrsAdapter = ArrayAdapter.createFromResource(this,
                R.array.hours, R.layout.centered_spinner);
        tnrStartTimeHrsAdapter.setDropDownViewResource(R.layout.centered_spinner);
        tnrStartTimeHrs.setAdapter(tnrStartTimeHrsAdapter);

        ArrayAdapter<CharSequence> tnrStartTimeMinsAdapter = ArrayAdapter.createFromResource(this,
                R.array.minutes, R.layout.centered_spinner);
        tnrStartTimeMinsAdapter.setDropDownViewResource(R.layout.centered_spinner);
        tnrStartTimeMins.setAdapter(tnrStartTimeMinsAdapter);

        // Populate the teams spinner
        populateTeamsSpinner();

        // Set seekbar listeners
        tnrInitialTime.setOnSeekBarChangeListener(this);
        tnrIncrement.setOnSeekBarChangeListener(this);
        tnrRounds.setOnSeekBarChangeListener(this);
        tnrBreaks.setOnSeekBarChangeListener(this);
        tnrDuration.setOnSeekBarChangeListener(this);
        tnrLeaders.setOnSeekBarChangeListener(this);
        tnrMinRating.setOnSeekBarChangeListener(this);
        tnrMaxRating.setOnSeekBarChangeListener(this);
        tnrRatedGames.setOnSeekBarChangeListener(this);

        tnrType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    tnrRoundsText.setVisibility(View.GONE);
                    tnrRounds.setVisibility(View.GONE);
                    tnrBreaksText.setVisibility(View.GONE);
                    tnrBreaks.setVisibility(View.GONE);
                    tnrDurationText.setVisibility(View.VISIBLE);
                    tnrDuration.setVisibility(View.VISIBLE);
                    tnrTeamsText.setText("Only members of team");
                    tnrTeamBattleTeamsText.setVisibility(View.GONE);
                    tnrTeamBattleTeams.setVisibility(View.GONE);
                    tnrLeadersText.setVisibility(View.GONE);
                    tnrLeaders.setVisibility(View.GONE);
                    tnrBerserk.setVisibility(View.VISIBLE);
                    tnrStreaks.setVisibility(View.VISIBLE);
                    tnrChatroom.setVisibility(View.VISIBLE);
                    tnrChatAccessText.setVisibility(View.GONE);
                    tnrChatAccess.setVisibility(View.GONE);
                    tnrForbiddenPairingsText.setVisibility(View.GONE);
                    tnrForbiddenPairings.setVisibility(View.GONE);
                } else if (position == 1) {
                    tnrRoundsText.setVisibility(View.VISIBLE);
                    tnrRounds.setVisibility(View.VISIBLE);
                    tnrBreaksText.setVisibility(View.VISIBLE);
                    tnrBreaks.setVisibility(View.VISIBLE);
                    tnrDurationText.setVisibility(View.GONE);
                    tnrDuration.setVisibility(View.GONE);
                    tnrTeamsText.setText("Only members of team");
                    tnrTeamBattleTeamsText.setVisibility(View.GONE);
                    tnrTeamBattleTeams.setVisibility(View.GONE);
                    tnrLeadersText.setVisibility(View.GONE);
                    tnrLeaders.setVisibility(View.GONE);
                    tnrBerserk.setVisibility(View.GONE);
                    tnrStreaks.setVisibility(View.GONE);
                    tnrChatroom.setVisibility(View.GONE);
                    tnrChatAccessText.setVisibility(View.VISIBLE);
                    tnrChatAccess.setVisibility(View.VISIBLE);
                    tnrForbiddenPairingsText.setVisibility(View.VISIBLE);
                    tnrForbiddenPairings.setVisibility(View.VISIBLE);
                } else if (position == 2) {
                    tnrRoundsText.setVisibility(View.GONE);
                    tnrRounds.setVisibility(View.GONE);
                    tnrBreaksText.setVisibility(View.GONE);
                    tnrBreaks.setVisibility(View.GONE);
                    tnrDurationText.setVisibility(View.VISIBLE);
                    tnrDuration.setVisibility(View.VISIBLE);
                    tnrTeamsText.setText("Team battle by team");
                    tnrTeamBattleTeamsText.setVisibility(View.VISIBLE);
                    tnrTeamBattleTeams.setVisibility(View.VISIBLE);
                    tnrLeadersText.setVisibility(View.VISIBLE);
                    tnrLeaders.setVisibility(View.VISIBLE);
                    tnrBerserk.setVisibility(View.VISIBLE);
                    tnrStreaks.setVisibility(View.VISIBLE);
                    tnrChatroom.setVisibility(View.VISIBLE);
                    tnrChatAccessText.setVisibility(View.GONE);
                    tnrChatAccess.setVisibility(View.GONE);
                    tnrForbiddenPairingsText.setVisibility(View.GONE);
                    tnrForbiddenPairings.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    save();
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Check whether this activity was called by ScheduleActivity
        Intent intent = getIntent();
        if (intent.hasExtra(ID)) {
            currentTnrId = intent.getStringExtra(ID);
            getSupportActionBar().setTitle("Edit Tournament");

            // Read the tournament from file as a JSON object
            File tnrFile = new File(getFilesDir() + "/data/" + token + "/tournaments/"
                    + currentTnrId);
            try {
                FileInputStream fis = new FileInputStream(tnrFile);
                byte[] data = new byte[(int) tnrFile.length()];
                fis.read(data);
                fis.close();
                String tnrFileText = new String(data, StandardCharsets.UTF_8);

                // The current tournament data
                JSONObject tnrData = new JSONObject(tnrFileText);

                // Set the tournament scheduling status
                if (tnrData.has("status")) {
                    tnrStatus = tnrData.getJSONObject("status");
                }

                tnrName.setText(tnrData.getString("name"), TextView.BufferType.EDITABLE);
                tnrDisplayName.setText(tnrData.getString("displayName"), TextView.BufferType.EDITABLE);

                String type = tnrData.getString("type");
                switch (type) {
                    case "arena":
                        tnrType.setSelection(0);
                        break;
                    case "swiss":
                        tnrType.setSelection(1);
                        break;
                    case "teamBattle":
                        tnrType.setSelection(2);
                        break;
                }

                String variant = tnrData.getString("variant");
                for (int i = 0; i < tnrVariantValues.length; i++) {
                    if (tnrVariantValues[i].equals(variant)) {
                        tnrVariant.setSelection(i);
                        break;
                    }
                }

                tnrRated.setChecked(tnrData.getBoolean("rated"));

                int initialTime = tnrData.getInt("initialTime");
                for (int i = 0; i < tnrInitialTimeValues.length; i++) {
                    if (tnrInitialTimeValues[i] == initialTime) {
                        tnrInitialTime.setProgress(i);
                        break;
                    }
                }

                int increment = tnrData.getInt("increment");
                for (int i = 0; i < tnrIncrementValues.length; i++) {
                    if (tnrIncrementValues[i] == increment) {
                        tnrIncrement.setProgress(i);
                        break;
                    }
                }

                if (type.equals("arena") || type.equals("teamBattle")) {
                    int intentDuration = tnrData.getInt("duration");
                    for (int i = 0; i < tnrDurationValues.length; i++) {
                        if (tnrDurationValues[i] == (intentDuration)) {
                            tnrDuration.setProgress(i);
                            break;
                        }
                    }
                } else if (type.equals("swiss")) {
                    int intentRounds = tnrData.getInt("rounds");
                    tnrRounds.setProgress(intentRounds - 3);

                    if (tnrData.has("breaks")) {
                        int breaks = tnrData.getInt("breaks");
                        for (int i = 0; i < tnrBreaksValues.length; i++) {
                            if (tnrBreaksValues[i] == breaks) {
                                tnrBreaks.setProgress(i);
                                break;
                            }
                        }
                    }
                }

                if (tnrData.has("startPos")) {
                    String startPos = tnrData.getString("startPos");
                    tnrStartPos.setText(startPos, TextView.BufferType.EDITABLE);
                }

                if (tnrData.has("description")) {
                    tnrDescr.setText(tnrData.getString("description"), TextView.BufferType.EDITABLE);
                }

                if (tnrData.has("team")) {
                    String team = tnrData.getString("team");
                    String teamName = tnrData.getString("teamName");
                    teamIds.add(team);
                    teamNames.add(teamName);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(EditActivity.this,
                            R.layout.support_simple_spinner_dropdown_item, teamNames);
                    tnrTeams.setAdapter(adapter);
                    tnrTeams.setSelection(1);
                }

                if (tnrData.has("password")) {
                    tnrPassword.setText(tnrData.getString("password"), TextView.BufferType.EDITABLE);
                }

                if (tnrData.has("battleTeams")) {
                    JSONArray battleTeams = tnrData.getJSONArray("battleTeams");
                    StringBuilder value = new StringBuilder(battleTeams.getString(0));
                    for (int i = 1; i < battleTeams.length(); i++) {
                        value.append(", ").append(battleTeams.getString(i));
                    }
                    tnrTeamBattleTeams.setText(value.toString(), TextView.BufferType.EDITABLE);
                }

                if (tnrData.has("leaders")) {
                    int leaders = tnrData.getInt("leaders");
                    tnrLeaders.setProgress(leaders - 1);
                }

                if (tnrData.has("minRating")) {
                    tnrMinRating.setProgress((tnrData.getInt("minRating") - 900) / 100);
                }

                if (tnrData.has("maxRating")) {
                    tnrMaxRating.setProgress((tnrData.getInt("maxRating") - 700) / 100);
                }

                if (tnrData.has("ratedGames")) {
                    int ratedGames = tnrData.getInt("ratedGames");
                    for (int i = 0; i < tnrRatedGamesValues.length; i++) {
                        if (tnrRatedGamesValues[i] == (ratedGames)) {
                            tnrRatedGames.setProgress(i);
                            break;
                        }
                    }
                }

                if (tnrData.getString("type").equals("swiss") && tnrData.has("chatFor")) {
                    tnrChatAccess.setSelection(tnrData.getInt("chatFor") / 10);
                } else {
                    tnrBerserk.setChecked(tnrData.optBoolean("berserk", true));
                    tnrStreaks.setChecked(tnrData.optBoolean("streaks", true));
                    tnrChatroom.setChecked(tnrData.optBoolean("chatroom", true));
                }

                if (tnrData.has("forbiddenPairings")) {
                    String forbiddenPairings = tnrData.getString("forbiddenPairings");
                    tnrForbiddenPairings.setText(forbiddenPairings, TextView.BufferType.EDITABLE);
                }

                String startTime = tnrData.getString("startTime");
                int startTimeHours = Integer.parseInt(startTime.substring(0, 2));
                int startTimeMinutes = Integer.parseInt(startTime.substring(2));
                tnrStartTimeHrs.setSelection(startTimeHours);
                tnrStartTimeMins.setSelection(startTimeMinutes);

                JSONArray scheduleOn = tnrData.getJSONArray("scheduleOn");
                for (int i = 0; i < checkBoxes.length; i++) {
                    checkBoxes[i].setChecked(scheduleOn.getBoolean(i));
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        menuPlayers = menu.findItem(R.id.menu_players);
        menuPlayers.setOnMenuItemClickListener(this);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_players:
                Intent intent = new Intent(this, PlayersActivity.class);
                startActivity(intent);
        }
        return false;
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.tnrInitialTime:
                tnrInitialTimeText.setText("Initial time: " + initialTimeStops.get(progress));
                break;
            case R.id.tnrIncrement:
                tnrIncrementText.setText("Increment: " + incrementStops.get(progress));
                break;
            case R.id.tnrRounds:
                tnrRoundsText.setText("Number of rounds: " + (progress + 3));
                break;
            case R.id.tnrBreaks:
                tnrBreaksText.setText("Round breaks: " + breaksStops.get(progress));
                break;
            case R.id.tnrDuration:
                tnrDurationText.setText("Duration: " + durationStops.get(progress));
                break;
            case R.id.tnrLeaders:
                tnrLeadersText.setText("Team leaders: " + (tnrLeaders.getProgress() + 1));
                break;
            case R.id.tnrMinRating:
                tnrMinRatingText.setText("Minimum rating: " + getMinRatingText(progress));
                break;
            case R.id.tnrMaxRating:
                tnrMaxRatingText.setText("Maximum rating: " + getMaxRatingText(progress));
                break;
            case R.id.tnrRatedGames:
                tnrRatedGamesText.setText("Minimum rated games: " + ratedGamesStops.get(progress));
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void save() throws JSONException, IOException {
        // Create the tournament JSON object
        JSONObject tnr = new JSONObject();
        if (!currentTnrId.equals("")) {
            tnr.put("id", currentTnrId);
        } else {
            // WARNING: must check that the generated id does not exist
            tnr.put("id", generateTournamentID());
        }

        tnr.put("name", tnrName.getText().toString());
        tnr.put("displayName", tnrDisplayName.getText().toString());
        tnr.put("type", tnrTypeValues[tnrType.getSelectedItemPosition()]);
        tnr.put("variant", tnrVariantValues[tnrVariant.getSelectedItemPosition()]);
        tnr.put("rated", tnrRated.isChecked());
        tnr.put("initialTime", tnrInitialTimeValues[tnrInitialTime.getProgress()]);
        tnr.put("increment", tnrIncrementValues[tnrIncrement.getProgress()]);

        if (tnrType.getSelectedItemPosition() == 0 || tnrType.getSelectedItemPosition() == 2) {
            tnr.put("duration", tnrDurationValues[tnrDuration.getProgress()]);
        } else if (tnrType.getSelectedItemPosition() == 1) {
            tnr.put("rounds", tnrRounds.getProgress() + 3);
            if (tnrBreaks.getProgress() > 0) {
                // Only put if not set to auto (default)
                tnr.put("roundBreaks", tnrBreaksValues[tnrBreaks.getProgress()]);
            }
        }

        if (!tnrStartPos.getText().toString().isEmpty()) {
            tnr.put("startPos", tnrStartPos.getText().toString());
        }

        if (!tnrDescr.getText().toString().equals("")) {
            tnr.put("description", tnrDescr.getText().toString());
        }

        if (tnrTeams.getSelectedItemPosition() > 0) {
            tnr.put("team", teamIds.get(tnrTeams.getSelectedItemPosition()));
            tnr.put("teamName", teamNames.get(tnrTeams.getSelectedItemPosition()));
        }

        if (!tnrPassword.getText().toString().equals("")) {
            tnr.put("password", tnrPassword.getText().toString());
        }

        if (tnrType.getSelectedItemPosition() == 2) {
            String[] teams = tnrTeamBattleTeams.getText().toString().split(",");
            JSONArray teamBattleTeams = new JSONArray();
            for (String team : teams) {
                teamBattleTeams.put(team.trim());
            }
            tnr.put("battleTeams", teamBattleTeams);

            // Add leaders if not set to default (5 leaders)
            if (tnrLeaders.getProgress() != 4) {
                tnr.put("leaders", tnrLeaders.getProgress() + 1);
            }
        }

        // Add rating/games restrictions if not set to default
        if (tnrMinRating.getProgress() > 0) {
            tnr.put("minRating", getMinRatingValue(tnrMinRating.getProgress()));
        }
        if (tnrMaxRating.getProgress() > 0) {
            tnr.put("maxRating", getMaxRatingValue(tnrMaxRating.getProgress()));
        }
        if (tnrRatedGames.getProgress() > 0) {
            tnr.put("ratedGames", tnrRatedGamesValues[tnrRatedGames.getProgress()]);
        }

        // Add berserk, streaks and chatroom options if not set to default (true)
        if (tnrType.getSelectedItemPosition() != 1) {
            if (!tnrBerserk.isChecked()) {
                tnr.put("berserk", false);
            }
            if (!tnrStreaks.isChecked()) {
                tnr.put("streaks", false);
            }
            if (!tnrChatroom.isChecked()) {
                tnr.put("chatroom", false);
            }
        } else {
            if (tnrChatAccess.getSelectedItemPosition() != 2) {
                tnr.put("chatFor", tnrChatAccess.getSelectedItemPosition() * 10);
            }
        }


        // Add forbidden pairings if specified
        if (tnrType.getSelectedItemPosition() == 1 && !tnrForbiddenPairings.getText().toString().isEmpty()) {
            tnr.put("forbiddenPairings", tnrForbiddenPairings.getText().toString().trim());
        }

        // Add start time
        tnr.put("startTime", tnrStartTimeHrs.getSelectedItem().toString() + tnrStartTimeMins.getSelectedItem().toString());

        // Add schedule on days
        JSONArray scheduleOn = new JSONArray();
        for (int i = 0; i < 7; i++) {
            scheduleOn.put(checkBoxes[i].isChecked());
        }
        tnr.put("scheduleOn", scheduleOn);

        // Add the tournament status
        if (tnrStatus == null) {
            tnrStatus = new JSONObject();
            tnrStatus.put("schedulingEnabled", true);
            tnrStatus.put("lastCreated", 0);
            tnrStatus.put("lastTnrId", "");
            tnrStatus.put("lastTnrType", "");
            tnrStatus.put("pendingPlaceholderReplacement", false);
            tnrStatus.put("lastCreationResult", "");
            tnrStatus.put("lastError", new JSONArray());
            tnrStatus.put("lastWarning", new JSONArray());
        }
        tnr.put("status", tnrStatus);

        // Validation checks
        if (!validateName(tnrName.getText().toString())) return;
        if (!validateDisplayName(tnrDisplayName.getText().toString())) return;
        if (!validateInitialTime(tnrInitialTime.getProgress(), tnrIncrement.getProgress())) return;
        if (tnrType.getSelectedItemPosition() == 0 && !validateDuration(tnrInitialTime.getProgress(), tnrDuration.getProgress()))
            return;
        if (!validateFEN(tnrVariantValues[tnrVariant.getSelectedItemPosition()], tnrStartPos.getText().toString())) return;
        if (!validateTeam(tnrType.getSelectedItemPosition(), tnrTeams.getSelectedItemPosition()))
            return;
        if (tnrType.getSelectedItemPosition() == 2 && !validateTeamBattleTeams(tnrTeamBattleTeams.getText().toString()))
            return;
        if (!validateMinAndMaxRatings(tnrMinRating.getProgress(), tnrMaxRating.getProgress()))
            return;
        if (tnrType.getSelectedItemPosition() == 1 && !validateForbiddenPairings(tnrForbiddenPairings.getText().toString()))
            return;
        if (!validateScheduleOn(checkBoxes)) return; // To be looked at

        // Show the loader
        loader.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        // Save the tournament to file
        String tnrFileName = tnr.getString("id");
        try {
            File dataDir = new File(getFilesDir(), "data");
            if (!dataDir.exists()) {
                dataDir.mkdir();
            }
            File tokenDir = new File(dataDir, token);
            if (!tokenDir.exists()) {
                tokenDir.mkdir();
            }
            File tnrDir = new File(tokenDir, "tournaments");
            if (!tnrDir.exists()) {
                tnrDir.mkdir();
            }
            File tnrFile = new File(tnrDir, tnrFileName);
            FileOutputStream tnrFileStream = new FileOutputStream(tnrFile);
            tnrFileStream.write(tnr.toString().getBytes(StandardCharsets.UTF_8));
            tnrFileStream.close();
            Toast.makeText(this, "Successfully saved", Toast.LENGTH_SHORT).show();

            // Hide the loader
            loader.setVisibility(View.GONE);
            btnSave.setEnabled(true);

            // Go to ScheduleActivity and kill this activity
            if (getIntent().hasExtra(ID) || getIntent().hasExtra(FROM_ACTIVITY)) {
                setResult(1);
                finish();
            } else {
                Intent intent = new Intent(this, ScheduleActivity.class);
                startActivity(intent);
                finish();
            }
        } catch(Exception e) {
            Snackbar.make(frameLayout, "Could not save", Snackbar.LENGTH_INDEFINITE)
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                save();
                            } catch (JSONException | IOException jsonException) {
                                jsonException.printStackTrace();
                            }
                        }
                    })
                    .setActionTextColor(0xFFE64A19)
                    .show();
            // Hide the loader
            loader.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            e.printStackTrace();
        }
    }

    public void populateTeamsSpinner() {
        startLoader.setVisibility(View.VISIBLE);

        String url = "https://lichess.org/api/team/of/" + username;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                startLoader.setVisibility(View.GONE);
                if (response.length() == 0) {
                    Snackbar.make(frameLayout, "Could not get teams", Snackbar.LENGTH_INDEFINITE)
                            .setAction("RETRY", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    populateTeamsSpinner();
                                }
                            })
                            .setActionTextColor(0xFFE64A19)
                            .show();
                } else {
                    ArrayList<String> newTeamIds = new ArrayList<>();
                    newTeamIds.add("");
                    ArrayList<String> newTeamNames = new ArrayList<>();
                    newTeamNames.add("Not specified");
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONArray teamLeaders = response.getJSONObject(i).getJSONArray("leaders");
                            for (int j = 0; j < teamLeaders.length(); j++) {
                                if (teamLeaders.getJSONObject(j).getString("id").equals(username.toLowerCase())) {
                                    newTeamIds.add(response.getJSONObject(i).getString("id"));
                                    newTeamNames.add(response.getJSONObject(i).getString("name"));
                                }
                            }
                        }
                        teamIds = newTeamIds;
                        teamNames = newTeamNames;
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(EditActivity.this,
                                R.layout.support_simple_spinner_dropdown_item, teamNames);
                        tnrTeams.setAdapter(adapter);
                        Intent intent = getIntent();
                        if (intent.hasExtra(TEAM)) {
                            String intentTeam = intent.getStringExtra(TEAM);
                            for (int j = 0; j < teamIds.size(); j++) {
                                if (teamIds.get(j).equals(intentTeam)) {
                                    tnrTeams.setSelection(j);
                                    break;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                startLoader.setVisibility(View.GONE);
                Snackbar.make(frameLayout, "Could not get teams", Snackbar.LENGTH_INDEFINITE)
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                populateTeamsSpinner();
                            }
                        })
                        .setActionTextColor(0xFFE64A19)
                        .show();
            }
        });
        requestQueue.add(jsonArrayRequest);
    }

    public String generateTournamentID() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int index = (int) Math.floor(Math.random() * 52);
            result.append(index < 26 ? Character.toUpperCase(chars.charAt(index)) : chars.charAt(index % 26));
        }
        return result.toString();
    }

    public boolean validateName(String name) {
        if (name.length() < 2 || name.length() > 30) {
            Toast.makeText(this, "Tournament name must be between 2 and 30 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!name.matches("(\\w|\\s|-|,|\\(|\\)|<\\d>){2,30}") || name.toLowerCase().contains("lichess")) {
            Toast.makeText(this, "Invalid tournament name", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateDisplayName(String displayName) {
        if (displayName.length() < 2 || displayName.length() > 30) {
            Toast.makeText(this, "Display name must be between 2 and 30 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!displayName.matches("(\\w|\\s|-|,|\\(|\\)){2,30}") || displayName.toLowerCase().contains("lichess")) {
            Toast.makeText(this, "Invalid tournament name", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateInitialTime(int initialTimeIndex, int incrementIndex) {
        if (initialTimeIndex == 0 && incrementIndex == 0) {
            Toast.makeText(this, "Initial time cannot be 0", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateForbiddenPairings(String forbiddenPairings) {
        if (forbiddenPairings.trim().isEmpty()) return true;
        String[] lines = forbiddenPairings.split("(\\n|\\r\\n)+");
        // Allow no more than 1000 forbidden pairings
        if (lines.length > 1000) {
            Toast.makeText(this, "Maximum number of forbidden pairings is 1000", Toast.LENGTH_SHORT).show();
            return false;
        }
        for (String line : lines) {
            String[] usernamePair = line.trim().split(" +");
            // Only accept exactly two usernames per line
            if (usernamePair.length != 2) {
                Toast.makeText(this, "Invalid forbidden pairings format", Toast.LENGTH_SHORT).show();
                return false;
            }
            for (String username : usernamePair) {
                // Accept valid usernames only
                if (!username.trim().toLowerCase().matches("[a-z0-9][a-z0-9_-]{0,28}[a-z0-9]")) {
                    Toast.makeText(this, "Invalid usernames in forbidden pairings", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }

    public boolean validateDuration(int initialTime, int duration) {
        // Check if duration meets minimum duration
        if (tnrDurationValues[duration] < tnrInitialTimeValues[initialTime] / 12) {
            Toast.makeText(this, "You must increase tournament duration", Toast.LENGTH_SHORT).show();
            return false;
        } else if (
                (initialTime == 1 && duration > 11) || (initialTime == 2 && duration > 15) ||
                        (initialTime == 3 && duration > 17) || (initialTime == 4 && duration > 19) ||
                        (initialTime == 5 && duration > 22) || (initialTime == 6 && duration > 24)) {
            Toast.makeText(this, "You must decrease tournament duration", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateFEN(String variant, String fen) {
        if (fen.isEmpty()) return true;
        if (!variant.equals("standard") && !variant.equals("chess960")) return false;
        String pieces = "kqrbnpKQRBNP";
        String nums = "12345678";
        int rows = 1;
        int squaresInCurrentRow = 0;
        for (int i = 0; i < fen.length(); i++) {
            if (pieces.indexOf(fen.charAt(i)) > -1) {
                squaresInCurrentRow++;
            } else if (nums.indexOf(fen.charAt(i)) > -1) {
                squaresInCurrentRow += Integer.parseInt(String.valueOf(fen.charAt(i)));
            } else if (squaresInCurrentRow == 8 && fen.charAt(i) == '/') {
                rows++;
                squaresInCurrentRow = 0;
            } else {
                Toast.makeText(this, "Invalid FEN", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (squaresInCurrentRow > 8 || rows > 8) {
                Toast.makeText(this, "Invalid FEN", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return rows == 8 && squaresInCurrentRow == 8;
    }

    public boolean validateTeam(int type, int team) {
        if (type == 1 && team == 0) {
            Toast.makeText(this, "You must select a team", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Pending
    public boolean validateScheduleOn(CheckBox[] days) {
        for (CheckBox day : days) {
            if (day.isChecked()) return true;
        }
        Toast.makeText(this, "You must select at least one day", Toast.LENGTH_SHORT).show();
        return false;
    }

    // Get minimum rating text/value
    public String getMinRatingText(int progress) {
        if (progress == 0) {
            return "none";
        } else {
            return String.valueOf(progress * 100 + 900);
        }
    }
    public int getMinRatingValue(int progress) {
        return progress * 100 + 900;
    }

    // Get maximum rating text/value
    public String getMaxRatingText(int progress) {
        if (progress == 0) {
            return "none";
        } else {
            return String.valueOf(progress * 100 + 700);
        }
    }
    public int getMaxRatingValue(int progress) {
        return progress * 100 + 700;
    }

    public boolean validateMinAndMaxRatings(int minProgress, int maxProgress) {
        if (minProgress == 0 && maxProgress == 0) {
            return true;
        }
        if (getMinRatingValue(minProgress) >= getMaxRatingValue(maxProgress)) {
            Toast.makeText(this, "Minimum rating should be less than maximum rating", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateTeamBattleTeams(String teams) {
        String[] teamsArr = teams.trim().split(",");
        // Allow between 2 and 200 teams
        if (teamsArr.length < 2) {
            Toast.makeText(this, "Tournament should have at least 2 teams", Toast.LENGTH_SHORT)
                    .show();
            return false;
        } else if (teamsArr.length > 200) {
            Toast.makeText(this, "Tournament should have at most 200 teams", Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
        for (String team : teamsArr) {
            if (!team.trim().matches("[A-Za-z0-9-]+[^-]")) {
                Toast.makeText(this, "Invalid team IDs", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }
}