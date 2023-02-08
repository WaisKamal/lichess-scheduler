package com.waisapps.lichessscheduler;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PlayerPromptDialog extends Dialog implements View.OnClickListener {

    private TextView plrDialogTitle;
    private EditText plrId, plrName;
    private Button btnDone, btnCancel;

    private String initialId, initialName;
    private int itemPosition;

    public String getInitialId() {
        return initialId;
    }

    public int getItemPosition() {
        return itemPosition;
    }

    public String getInitialName() {
        return initialName;
    }

    private OnButtonClick onButtonClick;

    public PlayerPromptDialog(Context context, int position, String initialId, String initialName) {
        super(context);
        itemPosition = position;
        this.initialId = initialId;
        this.initialName = initialName;
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.player_dialog);
        initializeViews();
    }

    // Initialize views
    private void initializeViews() {
        plrDialogTitle = findViewById(R.id.plrDialogTitle);
        plrId = findViewById(R.id.plrIdEdit);
        plrName = findViewById(R.id.plrNameEdit);
        btnDone = findViewById(R.id.btnDone);
        btnCancel = findViewById(R.id.btnCancel);
        btnDone.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
    }

    public void setTitle(String title) {
        plrDialogTitle.setText(title);
    }

    public void setId(String id) {
        plrId.setText(id, EditText.BufferType.EDITABLE);
    }

    public void setName(String name) {
        plrName.setText(name, EditText.BufferType.EDITABLE);
    }

    public void setOnClickListener(OnButtonClick onButtonClick) {
        this.onButtonClick = onButtonClick;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnDone:
                onButtonClick.onDoneButtonClick(plrId.getText().toString(),
                        plrName.getText().toString(), itemPosition);
                break;
            case R.id.btnCancel:
                onButtonClick.onCancelButtonClick();
                break;
        }
    }

    public interface OnButtonClick {
        void onDoneButtonClick(String id, String name, int position);
        void onCancelButtonClick();
    }
}
