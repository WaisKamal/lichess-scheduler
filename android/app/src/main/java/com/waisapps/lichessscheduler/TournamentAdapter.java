package com.waisapps.lichessscheduler;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TournamentAdapter extends RecyclerView.Adapter<TournamentAdapter.ViewHolder>
        implements TournamentPromptDialog.OnButtonClick {

    // Intent constants
    private static final String FROM_ACTIVITY = "com.waisapps.lichessscheduler.fromActiviy";
    private static final String ID = "com.waisapps.lichessscheduler.id";
    private static final String TEAM = "com.waisapps.lichessscheduler.team";

    private TournamentPromptDialog dialog;
    private List<TournamentItem> tournaments;
    private Context context;
    private String token;
    private RequestQueue queue;
    private Scheduler scheduler;

    public TournamentAdapter(List<TournamentItem> tournaments, Context context, String token, RequestQueue queue) {
        this.tournaments = tournaments;
        this.context = context;
        this.token = token;
        this.queue = queue;
        try {
            scheduler = new Scheduler(context, token);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshAdapter(List<TournamentItem> tnrs) {
        tournaments = tnrs;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public View overlay;
        public ConstraintLayout container, detailsContainer;
        public TextView name, time, variant, rating, duration, teamName, startTime, statusText,
                statusDescription;
        public ImageView teamIcon, tnrIcon, statusIcon;
        public ImageButton btnExpand;
        public Switch tnrSchedulingSwitch;
        public Button btnAction, btnRemove;

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

            btnAction = parent.findViewById(R.id.btnAction);
            btnRemove = parent.findViewById(R.id.btnRemove);
            detailsContainer = parent.findViewById(R.id.detailsContainer);
            tnrSchedulingSwitch = parent.findViewById(R.id.schedulingSwitch);

            statusIcon = parent.findViewById(R.id.statusIcon);
            statusText = parent.findViewById(R.id.statusText);
            statusDescription = parent.findViewById(R.id.statusDescription);

            container.setOnClickListener(this);
            btnExpand.setOnClickListener(this);
            btnAction.setOnClickListener(this);
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

                case R.id.btnAction:
                    try {
                        if (scheduler == null) {
                            scheduler = new Scheduler(context, token);
                        }
                        JSONObject tnrData = tournaments.get(getAdapterPosition()).data;
                        int tnrSchedulingStatus = scheduler.getTournamentState(tnrData);
                        if (tnrSchedulingStatus == 0) {
                            // Open tournament in browser
                            JSONArray lastTnrIds = tnrData
                                    .getJSONObject("status")
                                    .getJSONArray("lastTnrId");
                            String tournamentUrl = String.format("https://lichess.org/%s/%s",
                                    tnrData.getString("type").equals("swiss") ? "swiss" : "tournament",
                                    lastTnrIds.getString(lastTnrIds.length() - 1));
                            try {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(tournamentUrl));
                                context.startActivity(browserIntent);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(context,
                                        "There is no installed application that can open this URL",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            scheduler = new Scheduler(context, token);
                            scheduler.fetchWinnersAndSchedule(token,
                                    tournaments.get(getAdapterPosition()).data, queue);
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case R.id.btnRemove:
                    dialog = new TournamentPromptDialog(context, getAdapterPosition());
                    dialog.setOnClickListener(TournamentAdapter.this);
                    dialog.show();
                    // Set dialog background to transparent
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    // Set dialog dimensions
                    int width = context.getResources().getDisplayMetrics().widthPixels;
                    dialog.getWindow().setLayout((6 * width) / 7,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT);
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
                    timeControlText += "\u00bc"; // 1/4
                    break;
                case 30:
                    timeControlText += "\u00bd"; // 1/2
                    break;
                case 45:
                    timeControlText += "\u00be"; // 3/4
                    break;
                case 90:
                    timeControlText += "1" + "\u00bd"; // 1 1/2
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

            // Tournament status
            JSONObject tnrStatus = item.data.getJSONObject("status");

            // Set switch state
            if (tnrStatus.getBoolean("schedulingEnabled")) {
                holder.overlay.setVisibility(View.GONE);
                holder.tnrSchedulingSwitch.setChecked(true);
            } else {
                holder.overlay.setVisibility(View.VISIBLE);
                holder.tnrSchedulingSwitch.setChecked(false);
            }

            // Tournament last creation result
            String tnrLastCreationResult = tnrStatus.getString("lastCreationResult");
            // Set scheduling status icon
            int tnrSchedulingStatus = scheduler.getTournamentState(item.data);
            if (tnrSchedulingStatus == 0) {
                holder.statusIcon.setImageResource(R.drawable.ic_success);
                holder.statusText.setText("Scheduled successfully");
                holder.statusDescription.setText(String.format("Last scheduled %s",
                        getRelativeDate(tnrStatus.getLong("lastCreated"))));
                holder.btnAction.setText("View");
            } else if (tnrSchedulingStatus == 1) {
                holder.statusIcon.setImageResource(R.drawable.ic_info);
                holder.statusText.setText("Tournament was not scheduled today");
                holder.statusDescription.setText(String.format("Last scheduled %s",
                        getRelativeDate(tnrStatus.getLong("lastCreated"))));
                holder.btnAction.setText("Schedule");
            } else if (tnrSchedulingStatus == 2) {
                holder.statusIcon.setImageResource(R.drawable.ic_error);
                holder.statusText.setText("Failed to schedule tournament");
                // NOTE: This currently only shows the first error
                holder.statusDescription.setText(tnrStatus.getJSONArray("lastError").getString(0));
                holder.btnAction.setText("Retry");
            } else if (tnrSchedulingStatus == 3) {
                holder.statusIcon.setImageResource(R.drawable.ic_new);
                holder.statusText.setText("Tournament was never scheduled before");
                holder.statusDescription.setText("Tap \"SCHEDULE\" to schedule tournament");
                holder.btnAction.setText("Schedule");
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

    // Utility function to get days difference from a given date
    String getRelativeDate(long timeStamp) {
        Date tsDate = new Date(timeStamp);
        long now = new Date().getTime();
        // Length of a day in milliseconds
        final long day = 86400000;
        // Time left to the end of the day on which the tournament was scheduled
        long dayLeftOver = day - timeStamp % day;
        // Difference between current time and time the tournament was last scheduled
        long diff = now - timeStamp;
        // Hours and minutes formatted to two digits
        String tsHours = String.format(Locale.ENGLISH, "%02d", tsDate.getHours());
        String tsMinutes = String.format(Locale.ENGLISH, "%02d", tsDate.getMinutes());
        if (diff < dayLeftOver) {
            return String.format(Locale.ENGLISH, "today at %s:%s",
                    tsHours,
                    tsMinutes);
        } else {
            long daysDiff = (diff - dayLeftOver + day - 1) / day;
            if (daysDiff == 1) {
                return String.format(Locale.ENGLISH, "yesterday at %s:%s",
                        tsHours,
                        tsMinutes);
            } else if (daysDiff < 7) {
                return String.format(Locale.ENGLISH, "%d days ago", daysDiff);
            } else {
                String[] months = { "January", "February", "March", "April", "May", "June", "July",
                        "August", "September", "October", "November", "December" };
                return String.format(Locale.ENGLISH, "on %s %d, %d",
                        months[tsDate.getMonth()],
                        tsDate.getDate(),
                        tsDate.getYear());
            }
        }
    }

    @Override
    public void onRemoveButtonClick(int position) {
        try {
            String tnrId = tournaments.get(position).data.getString("id");
            File tnrFile = new File(context.getFilesDir() + "/data/"
                    + token + "/tournaments/" + tnrId);
            if (!tnrFile.delete()) {
                return;
            }
            tournaments.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, tournaments.size());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dialog.dismiss();
    }

    @Override
    public void onCancelButtonClick() {
        dialog.dismiss();
    }
}
