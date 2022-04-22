package com.waisapps.lichessscheduler;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scheduler {
    Context context;
    JSONObject playerNames;
    String token;

    public Scheduler(Context context, String token) throws IOException, JSONException {
        this.context = context;
        this.token = token;
        this.playerNames = getPlayerNames(token);
    }

    public void scheduleTournament(JSONObject tnrData, RequestQueue queue) throws JSONException {
        // The request URL
        String url = "";

        // The request object
        JSONObject requestObject = new JSONObject();

        // NOTE: placeholders in name should be replaced
        // NOTE: name should be trimmed to max 30 characters
        requestObject.put("name", tnrData.getString("name"));
        requestObject.put("variant", tnrData.getString("variant"));
        requestObject.put("rated", tnrData.getBoolean("rated"));

        // Set request URL and duration/number of rounds
        // Set berserk, streak and chatroom options
        // Set forbidden pairings (if tournament type is Swiss)
        // Set start date
        int tnrStartTimeHours = Integer.parseInt(tnrData.getString("startTime").substring(0, 2));
        int tnrStartTimeMinutes = Integer.parseInt(tnrData.getString("startTime").substring(2, 4));
        Date tnrStartDate = new Date();
        tnrStartDate.setHours(tnrStartTimeHours);
        tnrStartDate.setMinutes(tnrStartTimeMinutes);
        tnrStartDate.setSeconds(0);
        tnrStartDate = new Date(tnrStartDate.getTime() / 1000 * 1000);
        String tnrType = tnrData.getString("type");
        if (tnrType.equals("arena") || tnrType.equals("teamBattle")) {
            url = "https://lichess.org/api/tournament";
            requestObject.put("minutes", tnrData.getInt("duration"));
            requestObject.put("clockTime", tnrData.getInt("initialTime") / 60f);
            requestObject.put("clockIncrement", tnrData.getInt("increment"));
            if (tnrData.has("berserk")) {
                requestObject.put("berserkable", tnrData.getBoolean("berserk"));
            }
            if (tnrData.has("streaks")) {
                requestObject.put("streakable", tnrData.getBoolean("streaks"));
            }
            if (tnrData.has("chatroom")) {
                requestObject.put("hasChat", tnrData.getBoolean("chatroom"));
            }
            requestObject.put("startDate", tnrStartDate.getTime());
        } else if (tnrType.equals("swiss")) {
            String tnrTeam = tnrData.getString("team");
            url = "https://lichess.org/api/swiss/new/" + tnrTeam;
            requestObject.put("clock.limit", tnrData.getInt("initialTime"));
            requestObject.put("clock.increment", tnrData.get("increment"));
            requestObject.put("nbRounds", tnrData.get("rounds"));
            if (tnrData.has("breaks")) {
                requestObject.put("roundInterval", tnrData.getString("breaks"));
            }
            if (tnrData.has("chatFor")) {
                requestObject.put("chatFor", tnrData.getInt("chatFor"));
            }
            if (tnrData.has("forbiddenPairings")) {
                requestObject.put("forbiddenPairings", tnrData.getString("forbiddenPairings"));
            }
            requestObject.put("startsAt", tnrStartDate.getTime());
        }

        // Set starting position
        if (tnrData.has("startPos")) {
            requestObject.put("position", tnrData.getString("startPos"));
        }

        // Set password
        if (tnrData.has("password")) {
            requestObject.put("password", tnrData.getString("password"));
        }

        // Set minimum rating, maximum rating and minimum no. of rated games
        if (tnrData.has("minRating")) {
            requestObject.put("conditions.minRating.rating", tnrData.getInt("minRating"));
        }
        if (tnrData.has("maxRating")) {
            requestObject.put("conditions.maxRating.rating", tnrData.getInt("maxRating"));
        }
        if (tnrData.has("ratedGames")) {
            requestObject.put("conditions.nbRatedGame.nb", tnrData.getInt("ratedGames"));
        }

        // Set team
        if (tnrType.equals("arena") && tnrData.has("team")) {
            requestObject.put("conditions.teamMember.teamId", tnrData.getString("team"));
        } else if (tnrType.equals("teamBattle")) {
            requestObject.put("teamBattleByTeam", tnrData.getString("team"));
        }

        // Set description (allow no more than 1000 characters)
        if (tnrData.has("description")) {
            requestObject.put("description", tnrData.getString("description"));
        }

        // Replace any name placeholders (<1>, <2>, <3>)
        Pattern placeholderPattern = Pattern.compile("<\\d>");
        Matcher placeholderMatcher = placeholderPattern.matcher(requestObject.getString("name"));
        if (placeholderMatcher.find() && !tnrData.getJSONObject("status").getString("lastTnrId").isEmpty()) {
            // Assumes previous tournament is of the same type
            // This reduces number of requests to Lichess and hence avoids a 429
            if (tnrType.equals("arena") || tnrType.equals("teamBattle")) {
                String podiumUrl = "https://lichess.org/api/tournament/"
                        + tnrData.getJSONObject("status").getString("lastTnrId");
                String finalUrl = url;
                JsonObjectRequest podiumRequest = new JsonObjectRequest(Request.Method.GET, podiumUrl, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    Log.d("podiumResponse", response.toString());
                                    if (response.has("isFinished")) {
                                        if (response.getBoolean("isFinished")) {
                                            ArrayList<String> winners = new ArrayList<>();
                                            JSONArray podium = response.getJSONArray("podium");
                                            int podiumLength = Math.min(podium.length(), 3);
                                            for (int i = 0; i < podiumLength; i++) {
                                                String playerUsername = podium.getJSONObject(i)
                                                        .getString("name");
                                                winners.add(playerUsername);
                                                // Replace winners' usernames with their names
                                                if (playerNames.has(playerUsername.toLowerCase())) {
                                                    winners.set(i, playerNames.getString(playerUsername.toLowerCase()));
                                                }
                                            }
                                            String newTnrName = requestObject.getString("name")
                                                    .replaceAll("<1>", winners.get(0))
                                                    .replaceAll("<2>", winners.get(1))
                                                    .replaceAll("<3>", winners.get(2));
                                            // Slice name to no more than 30 characters
                                            newTnrName = newTnrName.length() > 30 ? newTnrName.substring(0, 30) : newTnrName;
                                            requestObject.put("name", newTnrName);
                                            // Remove all warnings from tournament file
                                            removeWarningsFromTournamentFile(token, tnrData.getString("id"));
                                            // Send POST request and schedule tournament
                                            createTournament(tnrData, requestObject, finalUrl, queue);
                                        }
                                    }
                                } catch(JSONException | IOException e) {
                                    Toast.makeText(context, "Podium request failed", Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        try {
                            String tnrId = tnrData.getString("id");
                            File tnrFile = new File(context.getFilesDir() + "/data/"
                                    + token + "/tournaments/" + tnrId);
                            FileInputStream fis = new FileInputStream(tnrFile);
                            byte[] data = new byte[(int) tnrFile.length()];
                            fis.read(data);
                            fis.close();
                            JSONObject tnrObject = new JSONObject(new String(data, StandardCharsets.UTF_8));
                            JSONObject tnrStatus = tnrObject.getJSONObject("status");
                            tnrStatus.put("lastCreationResult", "warning");
                            JSONArray errorsToWrite = new JSONArray();
                            errorsToWrite.put("Placeholder replacement failed");
                            tnrStatus.put("lastWarning", errorsToWrite);
                            tnrObject.put("status", tnrStatus);
                            FileOutputStream fos = new FileOutputStream(tnrFile);
                            byte[] dataToWrite = tnrObject.toString().getBytes(StandardCharsets.UTF_8);
                            fos.write(dataToWrite);
                            fos.close();
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                queue.add(podiumRequest);
            } else if (tnrType.equals("swiss")) {
                // PENDING deployment of API endpoint https://lichess.org/api/swiss/{id}
                String podiumUrl = "https://lichess.org/api/swiss/"
                        + tnrData.getJSONObject("status").getString("lastTnrId")
                        + "/results?nb=3";
                String finalUrl = url;
                StringRequest podiumRequest = new StringRequest(Request.Method.GET, podiumUrl,
                        new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String[] responseObjects = response.split("\\n|\\r\\n");
                        ArrayList<String> winners = new ArrayList<>();
                        try {
                            for (int i = 0; i < 3; i++) {
                                String playerUsername = new JSONObject(responseObjects[i])
                                        .getString("username");
                                winners.add(playerUsername);
                                // Replace winners' usernames with their names
                                if (playerNames.has(playerUsername.toLowerCase())) {
                                    winners.set(i, playerNames.getString(playerUsername.toLowerCase()));
                                }
                            }
                            String newTnrName = requestObject.getString("name")
                                    .replaceAll("<1>", winners.get(0))
                                    .replaceAll("<2>", winners.get(1))
                                    .replaceAll("<3>", winners.get(2));
                            // Slice name to no more than 30 characters
                            newTnrName = newTnrName.length() > 30 ? newTnrName.substring(0, 30) : newTnrName;
                            requestObject.put("name", newTnrName);

                            // Send POST request and schedule tournament
                            createTournament(tnrData, requestObject, finalUrl, queue);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, "Podium request failed", Toast.LENGTH_SHORT).show();
                        error.printStackTrace();
                    }
                });
                queue.add(podiumRequest);
            }
        } else {
            // Send POST request and schedule tournament
            createTournament(tnrData, requestObject, url, queue);
        }
    }

    public void setBattleTeams(String tnrId, JSONObject tnrData, RequestQueue queue)
            throws JSONException {
        String teamsRequestUrl = "https://lichess.org/api/tournament/team-battle/" + tnrId;
        JSONObject teamsRequestObject = new JSONObject();
        JSONArray tnrBattleTeams = tnrData.getJSONArray("battleTeams");
        StringBuilder battleTeams = new StringBuilder(tnrBattleTeams.getString(0));
        for (int i = 1; i < tnrBattleTeams.length(); i++) {
            battleTeams.append(",").append(tnrBattleTeams.getString(i));
        }
        teamsRequestObject.put("teams", battleTeams.toString());
        teamsRequestObject.put("nbLeaders", tnrData.getInt("leaders"));
        JsonObjectRequest teamsRequest = new JsonObjectRequest(Request.Method.POST,
                teamsRequestUrl, teamsRequestObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, "Could not add teams to team battle", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        queue.add(teamsRequest);
    }

    private JSONObject getPlayerNames(String token) throws IOException, JSONException {
        JSONObject playerNames = new JSONObject();
        File plrFile = new File(context.getFilesDir() + "/data/"
                + token + "/players");
        FileInputStream fis = new FileInputStream(plrFile);
        byte[] data = new byte[(int) plrFile.length()];
        fis.read(data);
        fis.close();
        String plrFileText = new String(data, StandardCharsets.UTF_8);
        JSONArray plrFileObject = new JSONArray(plrFileText);
        for (int i = 0; i < plrFileObject.length(); i++) {
            playerNames.put(plrFileObject.getJSONObject(i).getString("id"),
                    plrFileObject.getJSONObject(i).getString("name"));
        }
        return playerNames;
    }

    private void createTournament(JSONObject tnrData, JSONObject requestObject, String url, RequestQueue queue) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(context, "Tournament scheduled successfully", Toast.LENGTH_SHORT).show();
                        // Update the scheduling status
                        try {
                            File tnrFile = new File(context.getFilesDir() + "/data/"
                                    + token + "/tournaments/" + tnrData.getString("id"));
                            FileInputStream fis = new FileInputStream(tnrFile);
                            byte[] dataToRead = new byte[(int) tnrFile.length()];
                            fis.read(dataToRead);
                            fis.close();
                            String tnrFileText = new String(dataToRead, StandardCharsets.UTF_8);
                            JSONObject tnrJSONData = new JSONObject(tnrFileText);
                            JSONObject tnrStatus = tnrJSONData.getJSONObject("status");
                            tnrStatus.put("lastCreated", new Date().getTime());
                            tnrStatus.put("lastTnrId", response.getString("id"));
                            tnrStatus.put("lastTnrType", tnrData.getString("type"));
                            tnrStatus.put("pendingPlaceholderReplacement", false);
                            tnrStatus.put("lastCreationResult", "success");
                            tnrStatus.put("lastError", new JSONArray());
                            tnrJSONData.put("status", tnrStatus);
                            FileOutputStream fos = new FileOutputStream(tnrFile);
                            byte[] dataToWrite = tnrJSONData.toString().getBytes(StandardCharsets.UTF_8);
                            fos.write(dataToWrite);
                            fos.close();

                            // Add teams if tournament is a team battle
                            if (tnrData.getString("type").equals("teamBattle")) {
                                setBattleTeams(response.getString("id"), tnrData, queue);
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, "Failed to schedule tournament", Toast.LENGTH_SHORT).show();
                // Write error to tournament file
                try {
                    JSONObject errorObject = new JSONObject(new String(error.networkResponse.data))
                            .getJSONObject("error");
                    JSONArray errorToWrite = new JSONArray();
                    Iterator<String> errorKeys = errorObject.keys();
                    while (errorKeys.hasNext()) {
                        String nextKey = errorKeys.next();
                        switch(nextKey) {
                            case "startDate":
                                errorToWrite.put("The specified start time has passed");
                                break;
                            case "position":
                                errorToWrite.put("The specified starting position is invalid");
                                break;
                            case "conditions.teamMember.teamId":
                            case "teamBattleByTeam":
                                errorToWrite.put("You are not a leader in the specified team");
                                break;
                            default:
                                errorToWrite.put("Unknown error occurred");
                        }
                    }
                    File tnrFile = new File(context.getFilesDir() + "/data/" + token +
                            "/tournaments/" + tnrData.getString("id"));
                    FileInputStream fis = new FileInputStream(tnrFile);
                    byte[] dataToRead = new byte[(int) tnrFile.length()];
                    fis.read(dataToRead);
                    fis.close();
                    JSONObject tnrJSONData = new JSONObject(new String(dataToRead, StandardCharsets.UTF_8));
                    JSONObject tnrStatus = tnrJSONData.getJSONObject("status");
                    tnrStatus.put("pendingPlaceholderReplacement", false);
                    tnrStatus.put("lastCreationResult", "error");
                    tnrStatus.put("lastError", errorToWrite);
                    tnrJSONData.put("status", tnrStatus);
                    FileOutputStream fos = new FileOutputStream(tnrFile);
                    byte[] dataToWrite = tnrJSONData.toString().getBytes(StandardCharsets.UTF_8);
                    fos.write(dataToWrite);
                    fos.close();
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        queue.add(request);
    }

    private boolean isSameDay(Date a, Date b) {
        return (a.getDay() == b.getDay() && a.getMonth() == b.getMonth() && a.getYear() == b.getYear());
    }

    private void removeWarningsFromTournamentFile(String token, String tnrId) throws IOException, JSONException {
        File tnrFile = new File(context.getFilesDir() + "/data/" + token + "/tournaments/" + tnrId);
        FileInputStream fis = new FileInputStream(tnrFile);
        byte[] dataToRead = new byte[(int) tnrFile.length()];
        fis.read(dataToRead);
        fis.close();
        JSONObject tnrJSONData = new JSONObject(new String(dataToRead, StandardCharsets.UTF_8));
        tnrJSONData.getJSONObject("status").put("lastWarning", new JSONArray());
        FileOutputStream fos = new FileOutputStream(tnrFile);
        byte[] dataToWrite = tnrJSONData.toString().getBytes(StandardCharsets.UTF_8);
        fos.write(dataToWrite);
        fos.close();
    }

    private void removeErrorsFromTournamentFile(String token, String tnrId) throws IOException, JSONException {
        File tnrFile = new File(context.getFilesDir() + "/data/" + token + "/tournaments/" + tnrId);
        FileInputStream fis = new FileInputStream(tnrFile);
        byte[] dataToRead = new byte[(int) tnrFile.length()];
        fis.read(dataToRead);
        fis.close();
        JSONObject tnrJSONData = new JSONObject(new String(dataToRead, StandardCharsets.UTF_8));
        tnrJSONData.getJSONObject("status").put("lastError", new JSONArray());
        FileOutputStream fos = new FileOutputStream(tnrFile);
        byte[] dataToWrite = tnrJSONData.toString().getBytes(StandardCharsets.UTF_8);
        fos.write(dataToWrite);
        fos.close();
    }
}
