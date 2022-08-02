package com.waisapps.lichessscheduler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

public class PlayersActivity extends AppCompatActivity implements View.OnClickListener,
        MenuItem.OnMenuItemClickListener, PlayerPromptDialog.OnButtonClick {

    private MenuItem menuSave, menuClear;
    private TextView labelNoItem;
    private CoordinatorLayout parent;
    private FloatingActionButton btnAdd;

    private RecyclerView plrContainer;
    private PlayerAdapter plrAdapter;

    private PlayerPromptDialog dialog;

    // The list of players
    ArrayList<PlayerItem> players;

    // The field validator
    private TournamentFieldValidator validator;

    // Current token
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_players);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize current token
        token = getSharedPreferences("com.waisapps.lichessscheduler.userinfo", MODE_PRIVATE)
                .getString("token", "");

        // Initialize views
        labelNoItem = findViewById(R.id.labelNoItem);
        parent = findViewById(R.id.container);
        btnAdd = findViewById(R.id.btnAdd);

        // Initialize field validator
        validator = new TournamentFieldValidator(this);

        // Initialize recycler view
        plrContainer = findViewById(R.id.tnrContainer);

        try {
            players = getPlayers();
            plrAdapter = new PlayerAdapter(this, players);
            plrAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    if (players.size() > 0) {
                        labelNoItem.setVisibility(View.GONE);
                    } else {
                        labelNoItem.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onChanged() {
                    if (players.size() > 0) {
                        labelNoItem.setVisibility(View.GONE);
                    } else {
                        labelNoItem.setVisibility(View.VISIBLE);
                    }
                }
            });
            plrContainer.setAdapter(plrAdapter);
            plrContainer.setLayoutManager(new LinearLayoutManager(this));
            if (plrAdapter.getItemCount() == 0) {
                labelNoItem.setVisibility(View.VISIBLE);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        btnAdd.setOnClickListener(this);
    }

    public ArrayList<PlayerItem> getPlayers() throws IOException, JSONException {
        ArrayList<PlayerItem> players = new ArrayList<>();
        String plrFileText = PlayerFileManager.getPlayers(getFilesDir(), token);
        JSONArray plrArray = new JSONArray(plrFileText);
        for (int i = 0; i < plrArray.length(); i++) {
            players.add(new PlayerItem(plrArray.getJSONObject(i)));
        }
        return players;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_players, menu);
        menuSave = menu.findItem(R.id.menu_save);
        menuClear = menu.findItem(R.id.menu_clear);
        menuSave.setOnMenuItemClickListener(this);
        menuClear.setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                try {
                    save();
                    plrAdapter.setUnsavedChanges(false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.menu_clear:
                if (plrAdapter.getItemCount() > 0) {
                    DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                plrAdapter.removeAllPlayers();
                                plrAdapter.setUnsavedChanges(true);
                            }
                        }
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Clear player list?")
                            .setMessage("Are you sure you would like to clear the player list?")
                            .setPositiveButton("Clear", dialogListener)
                            .setNegativeButton("Cancel", dialogListener)
                            .show();
                }
        }
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (plrAdapter.hasUnsavedChanges()) {
            DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == AlertDialog.BUTTON_POSITIVE) {
                        try {
                            save();
                            PlayersActivity.super.onBackPressed();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (which == AlertDialog.BUTTON_NEGATIVE) {
                        PlayersActivity.super.onBackPressed();
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Save changes")
                    .setMessage("You have unsaved changes. Would you like to save them before leaving?")
                    .setPositiveButton("Save", dialogListener)
                    .setNegativeButton("Abandon changes", dialogListener)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    public void save() throws JSONException {
        JSONArray playersJSONArray = new JSONArray();
        for (int i = 0; i < players.size(); i++) {
            JSONObject player = new JSONObject();
            player.put("id", players.get(i).id);
            player.put("name", players.get(i).name);
            playersJSONArray.put(player);
        }
        try {
            PlayerFileManager.writePlayersToFile(getFilesDir(), token, playersJSONArray.toString());
            Toast.makeText(this, "Successfully saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Snackbar.make(parent, "Could not save", Snackbar.LENGTH_INDEFINITE)
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                save();
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                            }
                        }
                    })
                    .setActionTextColor(0xFFE64A19)
                    .show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAdd:
                dialog = new PlayerPromptDialog(PlayersActivity.this, -1, "", "");
                dialog.setOnClickListener(this);
                dialog.setTitle("New player");
                dialog.show();
                // Set dialog background to transparent
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                // Set dialog dimensions
                int width = this.getResources().getDisplayMetrics().widthPixels;
                dialog.getWindow().setLayout((6 * width) / 7,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT);
                break;
        }
    }

    @Override
    public void onDoneButtonClick(String id, String name, int position) {
        // Change id to lowercase
        id = id.toLowerCase();
        // Remove leading/trailing spaces
        id = id.trim();
        name = name.trim();

        // Validation checks
        if (!validator.validateUsername(id)) return;
        if (!validator.validateName(name)) return;

        // Check if player id already exists
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).id.equals(id)) {
                if (i == position) {
                    continue;
                }
                Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (dialog.getItemPosition() == -1 || !dialog.getInitialId().equals(id) ||
                !dialog.getInitialName().equals(name)) {
            plrAdapter.setUnsavedChanges(true);
        }
        PlayerItem item = new PlayerItem(id, name);
        players.add(item);
        plrAdapter.notifyItemChanged(players.size() - 1);
        dialog.dismiss();
    }

    @Override
    public void onCancelButtonClick() {
        dialog.dismiss();
    }
}