package com.waisapps.lichessscheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TournamentFieldValidatorTest {

    private TournamentFieldValidator validator;

    @Test
    public void setup() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        validator = new TournamentFieldValidator(context);
    }

    @Test
    public void testTournamentNameValidation() {
        String[] validTournamentNames = {};
        String[] invalidTournamentNames = {};
        for (String name : validTournamentNames) {
            assertTrue(validator.validateTournamentName(name));
        }
        for (String name : invalidTournamentNames) {
            assertFalse(validator.validateTournamentName(name));
        }
    }

    @Test
    public void testDisplayNameValidation() {
        String[] validDisplayNames = {};
        String[] invalidDisplayNames = {};
        for (String name : validDisplayNames) {
            assertTrue(validator.validateDisplayName(name));
        }
        for (String name : invalidDisplayNames) {
            assertFalse(validator.validateDisplayName(name));
        }
    }

    @Test
    public void testUsernameValidation() {
        String[] validUsernames = {};
        String[] invalidUsernames = {};
        for (String username : validUsernames) {
            assertTrue(validator.validateUsername(username));
        }
        for (String username : invalidUsernames) {
            assertFalse(validator.validateUsername(username));
        }
    }

    @Test
    public void testTeamBattleTeamsValidation() {
        String[] validTeamBattleTeams = {};
        String[] invalidTeamBattleTeams = {};
        for (String teamId : validTeamBattleTeams) {
            assertTrue(validator.validateTeamBattleTeams(teamId));
        }
        for (String teamId : invalidTeamBattleTeams) {
            assertTrue(validator.validateTeamBattleTeams(teamId));
        }
    }

    public void testFENValidation() {
        String[] validFENs = {};
        String[] invalidFENs = {};
        for (String fen : validFENs) {
            assertTrue(validator.validateFEN(fen));
        }
        for (String fen : invalidFENs) {
            assertFalse(validator.validateFEN(fen));
        }
    }

    public void testPlayerNameValidation() {
        String[] validPlayerNames = {};
        String[] invalidPlayerNames = {};
        for (String name : validPlayerNames) {
            assertTrue(validator.validatePlayerName(name));
        }
        for (String name : invalidPlayerNames) {
            assertFalse(validator.validatePlayerName(name));
        }
    }
}
