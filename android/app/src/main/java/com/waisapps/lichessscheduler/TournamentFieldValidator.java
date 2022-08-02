package com.waisapps.lichessscheduler;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.Toast;

public class TournamentFieldValidator {
    private final Context context;
    private final TournamentFieldValues fieldValues;
    
    public TournamentFieldValidator(Context context) {
        this.context = context;
        fieldValues = new TournamentFieldValues(context);
    }
    
    public boolean validateTournamentName(String name) {
        if (name.length() < 2 || name.length() > 30) {
            Toast.makeText(context, "Tournament name must be between 2 and 30 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!name.matches("(\\w|\\s|-|,|\\(|\\)|<\\d>){2,30}") || name.toLowerCase().contains("lichess")) {
            Toast.makeText(context, "Invalid tournament name", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateDisplayName(String displayName) {
        if (displayName.length() < 2 || displayName.length() > 30) {
            Toast.makeText(context, "Display name must be between 2 and 30 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!displayName.matches("(\\w|\\s|-|,|\\(|\\)){2,30}") || displayName.toLowerCase().contains("lichess")) {
            Toast.makeText(context, "Invalid tournament name", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateInitialTime(int initialTimeIndex, int incrementIndex) {
        if (initialTimeIndex == 0 && incrementIndex == 0) {
            Toast.makeText(context, "Initial time cannot be 0", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateForbiddenPairings(String forbiddenPairings) {
        if (forbiddenPairings.trim().isEmpty()) return true;
        String[] lines = forbiddenPairings.split("(\\n|\\r\\n)+");
        // Allow no more than 1000 forbidden pairings
        if (lines.length > 1000) {
            Toast.makeText(context, "Maximum number of forbidden pairings is 1000", Toast.LENGTH_SHORT).show();
            return false;
        }
        for (String line : lines) {
            String[] usernamePair = line.trim().split(" +");
            // Only accept exactly two usernames per line
            if (usernamePair.length != 2) {
                Toast.makeText(context, "Invalid forbidden pairings format", Toast.LENGTH_SHORT).show();
                return false;
            }
            for (String username : usernamePair) {
                // Accept valid usernames only
                if (!username.trim().toLowerCase().matches("[a-z0-9][a-z0-9_-]{0,28}[a-z0-9]")) {
                    Toast.makeText(context, "Invalid usernames in forbidden pairings", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }

    public boolean validateDuration(int initialTime, int duration) {
        // Check if duration meets minimum duration
        if (fieldValues.tnrDurationValues[duration] < fieldValues.tnrInitialTimeValues[initialTime] / 12) {
            Toast.makeText(context, "You must increase tournament duration", Toast.LENGTH_SHORT).show();
            return false;
        } else if (
                (initialTime == 1 && duration > 11) || (initialTime == 2 && duration > 15) ||
                        (initialTime == 3 && duration > 17) || (initialTime == 4 && duration > 19) ||
                        (initialTime == 5 && duration > 22) || (initialTime == 6 && duration > 24)) {
            Toast.makeText(context, "You must decrease tournament duration", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateFEN(String variant, String fen) {
        if (fen.isEmpty()) return true;
        if (!variant.equals("standard") && !variant.equals("chess960")) return false;
        String pieces = "kqrbnpKQRBNP";
        String nums = "12345678";
        int rows = 1;
        int squaresInCurrentRow = 0;
        for (int i = 0; i < fen.length(); i++) {
            if (pieces.indexOf(fen.charAt(i)) > -1) {
                squaresInCurrentRow++;
            } else if (nums.indexOf(fen.charAt(i)) > -1) {
                squaresInCurrentRow += Integer.parseInt(String.valueOf(fen.charAt(i)));
            } else if (squaresInCurrentRow == 8 && fen.charAt(i) == '/') {
                rows++;
                squaresInCurrentRow = 0;
            } else {
                Toast.makeText(context, "Invalid FEN", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (squaresInCurrentRow > 8 || rows > 8) {
                Toast.makeText(context, "Invalid FEN", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return rows == 8 && squaresInCurrentRow == 8;
    }

    public boolean validateTeam(int type, int team) {
        if (type == 1 && team == 0) {
            Toast.makeText(context, "You must select a team", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Pending
    public boolean validateScheduleOn(CheckBox[] days) {
        for (CheckBox day : days) {
            if (day.isChecked()) return true;
        }
        Toast.makeText(context, "You must select at least one day", Toast.LENGTH_SHORT).show();
        return false;
    }

    public boolean validateMinAndMaxRatings(int minProgress, int maxProgress) {
        if (minProgress == 0 && maxProgress == 0) {
            return true;
        }
        if (fieldValues.getMinRatingValue(minProgress) >= fieldValues.getMaxRatingValue(maxProgress)) {
            Toast.makeText(context, "Minimum rating should be less than maximum rating", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateTeamBattleTeams(String teams) {
        String[] teamsArr = teams.trim().split(",");
        // Allow between 2 and 200 teams
        if (teamsArr.length < 2) {
            Toast.makeText(context, "Tournament should have at least 2 teams", Toast.LENGTH_SHORT)
                    .show();
            return false;
        } else if (teamsArr.length > 200) {
            Toast.makeText(context, "Tournament should have at most 200 teams", Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
        for (String team : teamsArr) {
            if (!team.trim().matches("[A-Za-z0-9-]+[^-]")) {
                Toast.makeText(context, "Invalid team IDs", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    public boolean validateUsername(String username) {
        if (!username.trim().toLowerCase().matches("[a-z0-9][a-z0-9_-]{0,28}[a-z0-9]")) {
            Toast.makeText(context, "Invalid username", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean validateName(String name) {
        if (name.replaceAll("\\s", "").length() < 2) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        } else if (name.length() < 2) {
            Toast.makeText(context, "Name too short", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
