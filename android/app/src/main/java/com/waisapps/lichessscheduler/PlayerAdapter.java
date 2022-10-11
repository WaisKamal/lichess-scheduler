package com.waisapps.lichessscheduler;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.ViewHolder>
        implements PlayerPromptDialog.OnButtonClick {
    private List<PlayerItem> players;
    private Context context;
    PlayerPromptDialog dialog;

    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    public void setUnsavedChanges(boolean unsavedChanges) {
        this.unsavedChanges = unsavedChanges;
    }

    // Tells whether there are unsaved changes
    private boolean unsavedChanges = false;

    public PlayerAdapter(Context context, ArrayList<PlayerItem> players) {
        this.players = players;
        this.context = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView id, name;
        public ImageButton btnEdit, btnRemove;

        public ViewHolder(@NonNull View parent) {
            super(parent);

            id = parent.findViewById(R.id.id);
            name = parent.findViewById(R.id.name);
            btnEdit = parent.findViewById(R.id.btnEdit);
            btnRemove = parent.findViewById(R.id.btnRemove);

            btnEdit.setOnClickListener(this);
            btnRemove.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.btnEdit:
                    String initialId = id.getText().toString();
                    String initialName = name.getText().toString();
                    dialog = new PlayerPromptDialog(context, getAdapterPosition(), initialId, initialName);
                    dialog.setOnClickListener(PlayerAdapter.this);
                    dialog.setTitle("Edit player");
                    dialog.setId(initialId);
                    dialog.setName(initialName);
                    dialog.show();
                    // Set dialog background to transparent
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    // Set dialog dimensions
                    int width = context.getResources().getDisplayMetrics().widthPixels;
                    dialog.getWindow().setLayout((6 * width) / 7,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    break;

                case R.id.btnRemove:
                    int position = getAdapterPosition();
                    players.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, getItemCount());
                    unsavedChanges = true;
                    break;
            }
        }
    }

    public boolean validateUsername(String username) {
        if (!username.trim().toLowerCase().matches("[a-z0-9][a-z0-9_-]{0,28}[a-z0-9]")) {
            Toast.makeText(context, "Invalid username", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateName(String name) {
        if (name.replaceAll("\\s", "").length() == 0) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        } else if (name.length() < 2) {
            Toast.makeText(context, "Name too short", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void removeAllPlayers() {
        players.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View playerItem = inflater.inflate(R.layout.player_item, parent, false);
        return new ViewHolder(playerItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayerItem item = players.get(position);
        holder.id.setText(item.id);
        holder.name.setText(item.name);
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    @Override
    public void onDoneButtonClick(String id, String name, int position) {
        // Change id to lowercase
        id = id.toLowerCase();
        // Remove leading/trailing spaces
        id = id.trim();
        name = name.trim();

        // Validation checks
        if (!validateUsername(id)) return;
        if (!validateName(name)) return;

        // Check if player id already exists
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).id.equals(id)) {
                if (i == position) {
                    break;
                }
                Toast.makeText(context, "Username already exists", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (dialog.getItemPosition() == -1 || !dialog.getInitialId().equals(id) ||
                !dialog.getInitialName().equals(name)) {
            this.unsavedChanges = true;
        }
        PlayerItem item = players.get(position);
        item.id = id.trim();
        item.name = name.trim();
        notifyItemChanged(position);
        dialog.dismiss();
    }

    @Override
    public void onCancelButtonClick() {
        dialog.dismiss();
    }
}
