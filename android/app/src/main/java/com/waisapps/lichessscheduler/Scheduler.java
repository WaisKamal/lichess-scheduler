package com.waisapps.lichessscheduler;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scheduler {
    private final Context context;
    private final JSONObject playerNames;
    private final String token;
    private final TournamentFileManager tournamentFileManager;
    private final PlayerFileManager playerFileManager;

    public Scheduler(Context context, String token) throws JSONException, IOException {
        this.context = context;
        this.token = token;
        this.tournamentFileManager = new TournamentFileManager(context.getFilesDir());
        this.playerFileManager = new PlayerFileManager(context.getFilesDir());
        this.playerNames = getPlayerNames(token);
    }

    // Returns tournament state. State is one of the following:
    //  0: tournament was last successfully scheduled and is not pending scheduling
    //  1: tournament was last successfully scheduled but is pending scheduling
    //  2: tournament scheduling was unsuccessful
    public int getTournamentState(JSONObject tnrData) throws JSONException {
        JSONObject tnrStatus = tnrData.getJSONObject("status");
        long lastScheduledDate = tnrStatus.getLong("lastCreated");
        Date now = new Date();
        if (tnrStatus.getString("lastCreationResult").equals("success")) {
            if (isSameDay(lastScheduledDate, now) ||
                    !tnrData.getJSONArray("scheduleOn").getBoolean(now.getDay()) ||
                    !tnrStatus.getBoolean("schedulingEnabled")) {
                return 0;
            } else {
                return 1;
            }
        } else {
            return 2;
        }
    }

    // Fetches tournament winners, then calls scheduleTournament
    public void fetchWinnersAndSchedule(String token, JSONObject tnrData, RequestQueue queue) {
        String tnrId = tnrData.optString("id", "");
        String tnrName = tnrData.optString("name", "");
        String tnrType = tnrData.optString("type");
        String tnrDescription = tnrData.optString("description", "");
        ArrayList<String> winners = new ArrayList<>();
        Pattern placeholderPattern = Pattern.compile("<[1-3]>");
        try {
            // Check if name and/or description contains winners placeholders
            JSONArray lastTnrId = tnrData.getJSONObject("status").getJSONArray("lastTnrId");
            Matcher nameMatcher = placeholderPattern.matcher(tnrName);
            Matcher descriptionMatcher = placeholderPattern.matcher(tnrDescription);
            if (lastTnrId.length() > 0 && (nameMatcher.find() || descriptionMatcher.find())) {
                String podiumRequestURL = "";
                if (tnrType.equals("arena") || tnrType.equals("teamBattle")) {
                    podiumRequestURL = String.format("https://lichess.org/api/tournament/%s", lastTnrId.getString(lastTnrId.length() - 1));
                } else if (tnrType.equals("swiss")) {
                    podiumRequestURL = String.format("https://lichess.org/api/swiss/%s/results?nb=3", lastTnrId.getString(lastTnrId.length() - 1));
                }
                StringRequest podiumRequest = new StringRequest(Request.Method.GET, podiumRequestURL, response -> {
                    try {
                        if (tnrType.equals("arena") || tnrType.equals("teamBattle")) {
                            JSONArray podium = new JSONObject(response).getJSONArray("podium");
                            for (int i = 0; i < podium.length(); i++) {
                                String userId = podium.getJSONObject(i).getString("name").toLowerCase();
                                winners.add(playerNames.has(userId) ? playerNames.getString(userId) : userId);
                            }
                        } else if (tnrType.equals("swiss")) {
                            String[] podiumString = response.split("\\n|\\r\\n");
                            for (String player : podiumString) {
                                String userId = new JSONObject(player).getString("username").toLowerCase();
                                winners.add(playerNames.has(userId) ? playerNames.getString(userId) : userId);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // Fill the rest of the winners array with empty strings
                    for (int i = winners.size(); i < 3; i++) {
                        winners.add("");
                    }
                    try {
                        scheduleTournament(tnrData, winners, queue);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        // Pending: deal with exception
                    }
                }, podiumRequestError -> {
                    // If tournament not found
                    if (podiumRequestError.networkResponse.statusCode == 404) {
                        try {
                            // Remove the missing tournament ID from lastTnrId
                            lastTnrId.remove(lastTnrId.length() - 1);
                            // Clear the winners array
                            winners.clear();
                            // Update tournament file
                            updateLastTnrId(token, tnrId, lastTnrId);
                            // The request URL for the previous tournament
                            String previousPodiumRequestURL = "";
                            if (tnrType.equals("arena") || tnrType.equals("teamBattle")) {
                                previousPodiumRequestURL = String.format("https://lichess.org/api/tournament/%s", lastTnrId.getString(lastTnrId.length() - 1));
                            } else if (tnrType.equals("swiss")) {
                                previousPodiumRequestURL = String.format("https://lichess.org/api/swiss/%s/results?nb=3", lastTnrId.getString(lastTnrId.length() - 1));
                            }
                            if (lastTnrId.length() > 0) {
                                StringRequest previousPodiumRequest = new StringRequest(Request.Method.GET, previousPodiumRequestURL, response -> {
                                    try {
                                        if (tnrType.equals("arena") || tnrType.equals("teamBattle")) {
                                            JSONArray podium = new JSONObject(response).getJSONArray("podium");
                                            for (int i = 0; i < podium.length(); i++) {
                                                String userId = podium.getJSONObject(i).getString("name").toLowerCase();
                                                winners.add(playerNames.has(userId) ? playerNames.getString(userId) : userId);
                                            }
                                        } else if (tnrType.equals("swiss")) {
                                            String[] podiumString = response.split("\\n|\\r\\n");
                                            for (String player : podiumString) {
                                                String userId = new JSONObject(player).getString("username").toLowerCase();
                                                winners.add(playerNames.has(userId) ? playerNames.getString(userId) : userId);
                                            }
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    // Fill the rest of the winners array with empty strings
                                    for (int i = winners.size(); i < 3; i++) {
                                        winners.add("");
                                    }
                                    try {
                                        scheduleTournament(tnrData, winners, queue);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        // Pending: deal with exception
                                    }
                                }, previousPodiumRequestError -> {
                                    // Empty the winners array if the previous tournament is also not found
                                    winners.clear();
                                    for (int i = 0; i < 3; i++) {
                                        winners.add("");
                                    }
                                    // Empty the lastTnrId array as well
                                    if (previousPodiumRequestError.networkResponse.statusCode == 404) {
                                        try {
                                            updateLastTnrId(token, tnrId, new JSONArray());
                                        } catch (JSONException | IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    try {
                                        scheduleTournament(tnrData, winners, queue);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        // Pending: deal with exception
                                    }
                                });
                                queue.add(previousPodiumRequest);
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                queue.add(podiumRequest);
            } else {
                for (int i = 0; i < 3; i++) {
                    winners.add("");
                }
                scheduleTournament(tnrData, winners, queue);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Schedules tournament given the tournament's JSON file data and the list of winners
    // from the previous tournament's podium
    private void scheduleTournament(JSONObject tnrData, ArrayList<String> winners, RequestQueue queue) throws JSONException {
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

        // Replace any name placeholders (<1>, <2>, <3>) in name and description
        String newTnrName = requestObject.getString("name")
                .replaceAll("<1>", winners.get(0))
                .replaceAll("<2>", winners.get(1))
                .replaceAll("<3>", winners.get(2));
        // Slice name to no more than 30 characters
        newTnrName = newTnrName.substring(0, Math.min(newTnrName.length(), 30));
        requestObject.put("name", newTnrName);
        if (requestObject.has("description")) {
            String newTnrDescription = requestObject.getString("description")
                    .replaceAll("<1>", winners.get(0))
                    .replaceAll("<2>", winners.get(1))
                    .replaceAll("<3>", winners.get(2));
            // Slice description to no more than 1000 characters
            newTnrDescription = newTnrDescription.substring(0, Math.min(newTnrDescription.length(), 1000));
            requestObject.put("description", newTnrDescription);
        }

        // Send POST request and schedule tournament
        createTournament(tnrData, requestObject, url, queue);
    }

    private void setBattleTeams(String tnrId, JSONObject tnrData, RequestQueue queue)
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
                teamsRequestUrl, teamsRequestObject, response -> {
            // Pending...
        }, error -> Toast.makeText(context, "Could not add teams to team battle", Toast.LENGTH_SHORT).show()) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        queue.add(teamsRequest);
    }

    private JSONObject getPlayerNames(String token) throws IOException, JSONException {
        JSONObject playerNames = new JSONObject();
        String plrFileText = playerFileManager.getPlayers(token);
        JSONArray plrFileObject = new JSONArray(plrFileText);
        for (int i = 0; i < plrFileObject.length(); i++) {
            playerNames.put(plrFileObject.getJSONObject(i).getString("id"),
                    plrFileObject.getJSONObject(i).getString("name"));
        }
        return playerNames;
    }

    private void createTournament(JSONObject tnrData, JSONObject requestObject, String url, RequestQueue queue) throws JSONException {
        String tnrId = tnrData.getString("id");
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestObject,
                response -> {
                    Toast.makeText(context, "Tournament scheduled successfully", Toast.LENGTH_SHORT).show();
                    // Update the scheduling status
                    try {
                        String tnrFileText = tournamentFileManager.getTournamentJSON(token, tnrId);
                        JSONObject tnrJSONData = new JSONObject(tnrFileText);
                        JSONObject tnrStatus = tnrJSONData.getJSONObject("status");
                        tnrStatus.getJSONArray("lastTnrId").put(response.getString("id"));
                        // Leave max 2 lastTnrIds
                        if (tnrStatus.getJSONArray("lastTnrId").length() > 2) {
                            tnrStatus.getJSONArray("lastTnrId").remove(0);
                        }
                        tnrStatus
                                .put("lastCreated", new Date().getTime())
                                .put("lastTnrType", tnrData.getString("type"))
                                .put("pendingPlaceholderReplacement", false)
                                .put("lastCreationResult", "success")
                                .put("lastError", new JSONArray());
                        tnrJSONData.put("status", tnrStatus);
                        tournamentFileManager.writeTournamentToFile(token, tnrId, tnrJSONData.toString());

                        // Add teams if tournament is a team battle
                        if (tnrData.getString("type").equals("teamBattle")) {
                            setBattleTeams(response.getString("id"), tnrData, queue);
                        }
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }, error -> {
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
                        String tnrFileText = tournamentFileManager.getTournamentJSON(token, tnrId);
                        JSONObject tnrJSONData = new JSONObject(tnrFileText);
                        JSONObject tnrStatus = tnrJSONData.getJSONObject("status");
                        tnrStatus.put("pendingPlaceholderReplacement", false);
                        tnrStatus.put("lastCreationResult", "error");
                        tnrStatus.put("lastError", errorToWrite);
                        tnrJSONData.put("status", tnrStatus);
                        tournamentFileManager.writeTournamentToFile(token, tnrId, tnrJSONData.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    error.printStackTrace();
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

    private void updateLastTnrId(String token, String tnrId, JSONArray lastTnrId) throws IOException, JSONException {
        String tnrFileText = tournamentFileManager.getTournamentJSON(token, tnrId);
        JSONObject tnrJSONData = new JSONObject(tnrFileText);
        tnrJSONData.getJSONObject("status").put("lastTnrId", lastTnrId);
        tournamentFileManager.writeTournamentToFile(token, tnrId, tnrJSONData.toString());
    }

    private void removeWarningsFromTournamentFile(String token, String tnrId) throws IOException, JSONException {
        String tnrFileText = tournamentFileManager.getTournamentJSON(token, tnrId);
        JSONObject tnrJSONData = new JSONObject(tnrFileText);
        tnrJSONData.getJSONObject("status").put("lastWarning", new JSONArray());
        tournamentFileManager.writeTournamentToFile(token, tnrId, tnrJSONData.toString());
    }

    private void removeErrorsFromTournamentFile(String token, String tnrId) throws IOException, JSONException {
        String tnrFileText = tournamentFileManager.getTournamentJSON(token, tnrId);
        JSONObject tnrJSONData = new JSONObject(tnrFileText);
        tnrJSONData.getJSONObject("status").put("lastError", new JSONArray());
        tournamentFileManager.writeTournamentToFile(token, tnrId, tnrJSONData.toString());
    }

    private boolean isSameDay(long timestamp, Date date) {
        Date tsDate = new Date(timestamp);
        return tsDate.getDate() == date.getDate()
                && tsDate.getMonth() == date.getMonth()
                && tsDate.getYear() == date.getYear();
    }
}
