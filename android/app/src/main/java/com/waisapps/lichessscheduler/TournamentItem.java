package com.waisapps.lichessscheduler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TournamentItem {
    public JSONObject data;

    public TournamentItem(JSONObject tnrData) {
        data = tnrData;
    }
}
