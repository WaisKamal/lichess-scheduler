package com.waisapps.lichessscheduler;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class TournamentPromptDialog extends Dialog implements View.OnClickListener {

    private Button btnCancel, btnRemove;
    private int itemPosition;
    private OnButtonClick onButtonClick;

    public TournamentPromptDialog(Context context, int position) {
        super(context);
        itemPosition = position;
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.tournament_prompt_dialog);
        btnRemove = findViewById(R.id.btnRemove);
        btnCancel = findViewById(R.id.btnCancel);
        btnRemove.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
    }

    public void setOnClickListener(OnButtonClick onButtonClick) {
        this.onButtonClick = onButtonClick;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnCancel:
                onButtonClick.onCancelButtonClick();
                break;
            case R.id.btnRemove:
                onButtonClick.onRemoveButtonClick(itemPosition);
                break;
        }
    }

    public interface OnButtonClick {
        void onRemoveButtonClick(int position);
        void onCancelButtonClick();
    }
}
