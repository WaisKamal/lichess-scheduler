package com.waisapps.lichessscheduler;

public class SchedulingStatus {
    public static final int SUCCESS = 0;
    public static final int NO_INTERNET = 0;
    public static final int PREVIOUS_TOURNAMENT_NOT_FOUND = 1;
    public static final int FAILED_SETTING_BATTLE_TEAMS = 2;
    public static final int TOURNAMENT_SCHEDULING_FAILED = 3;
    public static final int INVALID_NAME_AFTER_PLACEHOLDERS_REPLACED = 4;
    public static final int REPLACING_PLACEHOLDERS_FAILED = 5;
    public static final int START_TIME_PASSED = 6;
}
