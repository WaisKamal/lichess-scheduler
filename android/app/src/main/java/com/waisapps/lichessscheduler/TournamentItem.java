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

    public static ArrayList<TournamentItem> createTournamentItemList(JSONArray tournaments) throws JSONException {
        ArrayList<TournamentItem> tnrItems = new ArrayList<>();
        for (int i = 0; i < tournaments.length(); i++) {
            tnrItems.add(new TournamentItem(tournaments.getJSONObject(i)));
        }
        return tnrItems;
    }

}
