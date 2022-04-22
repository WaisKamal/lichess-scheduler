package com.waisapps.lichessscheduler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PlayerItem {
    public String id, name;

    public PlayerItem(String pId, String pName) {
        id = pId;
        name = pName;
    }

    public PlayerItem(JSONObject player) throws JSONException {
        id = player.getString("id");
        name = player.getString("name");
    }
}
