package com.waisapps.lichessscheduler;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class TournamentAdapter extends RecyclerView.Adapter<TournamentAdapter.ViewHolder> {

    // Intent constants
    private static final String FROM_ACTIVITY = "com.waisapps.lichessscheduler.fromActiviy";
    private static final String ID = "com.waisapps.lichessscheduler.id";
    private static final String TEAM = "com.waisapps.lichessscheduler.team";

    private List<TournamentItem> tournaments;
    private Context context;
    private String token;
    private RequestQueue queue;

    public TournamentAdapter(List<TournamentItem> tnrs, Context ctx, String tkn, RequestQueue reqQueue) {
        tournaments = tnrs;
        context = ctx;
        token = tkn;
        queue = reqQueue;
    }

    public void refreshAdapter(List<TournamentItem> tnrs) {
        tournaments = tnrs;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public View overlay;
        public ConstraintLayout container, detailsContainer;
        public TextView name, time, variant, rating, duration, teamName, startTime;
        public ImageView teamIcon, tnrIcon;
        public ImageButton btnExpand;
        public Switch tnrSchedulingSwitch;
        public Button btnRetry, btnRemove;

        public ViewHolder(View parent) {
            super(parent);

            container = parent.findViewById(R.id.container);
            name = parent.findViewById(R.id.name);
            time = parent.findViewById(R.id.time);
            variant = parent.findViewById(R.id.variant);
            rating = parent.findViewById(R.id.rating);
            duration = parent.findViewById(R.id.duration);
            teamIcon = parent.findViewById(R.id.teamIcon);
            teamName = parent.findViewById(R.id.teamName);
            startTime = parent.findViewById(R.id.startTime);
            overlay = parent.findViewById(R.id.overlay);
            tnrIcon = parent.findViewById(R.id.tnrIcon);
            btnExpand = parent.findViewById(R.id.btnExpand);

            btnRetry = parent.findViewById(R.id.btnRetry);
            btnRemove = parent.findViewById(R.id.btnRemove);
            detailsContainer = parent.findViewById(R.id.detailsContainer);
            tnrSchedulingSwitch = parent.findViewById(R.id.schedulingSwitch);

            container.setOnClickListener(this);
            btnExpand.setOnClickListener(this);
            btnRetry.setOnClickListener(this);
            btnRemove.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.container:
                    try {
                        JSONObject tnrData = tournaments.get(getAdapterPosition()).data;
                        String tnrId = tnrData.getString("id");
                        Intent intent = new Intent(context, EditActivity.class);
                        intent.putExtra(ID, tnrId);
                        intent.putExtra(FROM_ACTIVITY, "ScheduleActivity");
                        if (tnrData.has("team")) {
                            String tnrTeam = tnrData.getString("team");
                            intent.putExtra(TEAM, tnrTeam);
                        }
                        ((AppCompatActivity) context).startActivityForResult(intent, 1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case R.id.btnExpand:
                    if (detailsContainer.getVisibility() == View.GONE) {
                        detailsContainer.setVisibility(View.VISIBLE);
                    } else {
                        detailsContainer.setVisibility(View.GONE);
                    }
                    break;

                case R.id.btnRetry:
                    Scheduler scheduler = null;
                    try {
                        scheduler = new Scheduler(context, token);
                        scheduler.scheduleTournament(tournaments.get(getAdapterPosition()).data, queue);
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case R.id.btnRemove:
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                try {
                                    int index = getAdapterPosition();
                                    String tnrId = tournaments.get(index).data.getString("id");
                                    File tnrFile = new File(context.getFilesDir() + "/data/"
                                            + token + "/tournaments/" + tnrId);
                                    if (!tnrFile.delete()) {
                                        return;
                                    }
                                    tournaments.remove(getAdapterPosition());
                                    notifyItemRemoved(index);
                                    notifyItemRangeChanged(index, tournaments.size());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Remove tournament?")
                            .setMessage("Are you sure you would like to remove this tournament from the schedule?")
                            .setPositiveButton("Remove", dialogClickListener)
                            .setNegativeButton("Cancel", dialogClickListener)
                            .show();

            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View tnrItem = inflater.inflate(R.layout.tnr_item, parent, false);
        return new ViewHolder(tnrItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int fixedPosition = holder.getAdapterPosition();
        TournamentItem item = tournaments.get(fixedPosition);
        try {
            // Set tournament name
            if (!item.data.getString("displayName").equals("")) {
                holder.name.setText(item.data.getString("displayName"));
            } else {
                holder.name.setText(item.data.getString("name"));
            }

            // Set tournament time control
            String timeControlText = "";
            int initialTime = item.data.getInt("initialTime");
            switch (initialTime) {
                case 15:
                    timeControlText += "\u00bc";
                    break;
                case 30:
                    timeControlText += "\u00bd";
                    break;
                case 45:
                    timeControlText += "\u00be";
                    break;
                case 90:
                    timeControlText += "1" + "\u00bd";
                    break;
                default:
                    timeControlText = String.valueOf(initialTime / 60);
            }
            timeControlText += "+" + item.data.getInt("increment");
            holder.time.setText(timeControlText);

            // Set tournament variant
            List<String> tnrVariantOptions = Arrays.asList(holder.itemView.getResources()
                    .getStringArray(R.array.variant));
            List<String> tnrVariantValues = Arrays.asList(holder.itemView.getResources()
                    .getStringArray(R.array.variant_values));
            holder.variant.setText(tnrVariantOptions.get(tnrVariantValues.indexOf(item.data.getString("variant"))));

            // Set rating status (rated/unrated)
            holder.rating.setText(item.data.getBoolean("rated") ? "Rated" : "Casual");

            // Set duration/number of rounds
            // Set tournament icon
            if (item.data.getString("type").equals("swiss")) {
                holder.duration.setText(item.data.getInt("rounds") + " rounds");
                holder.tnrIcon.setImageResource(R.drawable.ic_cup);
            } else {
                holder.duration.setText(item.data.getInt("duration") + " minutes");
                holder.tnrIcon.setImageResource(R.drawable.ic_timer);
            }

            // Set start time
            String startTimeText = item.data.getString("startTime").substring(0, 2) + ":"
                    + item.data.getString("startTime").substring(2, 4);
            holder.startTime.setText(startTimeText);

            // Hide/show team icon as appropriate
            if (item.data.getString("type").equals("arena")) {
                if (item.data.has("team")) {
                    holder.teamName.setText(item.data.getString("teamName"));
                    holder.teamIcon.setVisibility(View.VISIBLE);
                    holder.teamName.setVisibility(View.VISIBLE);
                } else {
                    holder.teamIcon.setVisibility(View.GONE);
                    holder.teamName.setVisibility(View.GONE);
                }
            } else {
                holder.teamName.setText(item.data.getString("teamName"));
                holder.teamIcon.setVisibility(View.VISIBLE);
                holder.teamName.setVisibility(View.VISIBLE);
            }

            // Set switch state
            if (item.data.getJSONObject("status").getBoolean("schedulingEnabled")) {
                holder.overlay.setVisibility(View.GONE);
                holder.tnrSchedulingSwitch.setChecked(true);
            } else {
                holder.overlay.setVisibility(View.VISIBLE);
                holder.tnrSchedulingSwitch.setChecked(false);
            }

            // Add change listener to the switch
            holder.tnrSchedulingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    holder.overlay.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                    try {
                        String tnrId = tournaments.get(fixedPosition).data.getString("id");
                        tournaments.get(fixedPosition).data.getJSONObject("status")
                                .put("schedulingEnabled", isChecked);
                        // Write tournament to file
                        File tnrFile = new File(context.getFilesDir() + "/data/" + token +
                                "/tournaments/" + tnrId);
                        FileOutputStream fos = new FileOutputStream(tnrFile);
                        fos.write(tournaments.get(fixedPosition).data.toString().getBytes(StandardCharsets.UTF_8));
                    } catch(JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return tournaments.size();
    }
}
