package com.waisapps.lichessscheduler;

import android.content.Context;

public class TournamentFieldValues {
    private final Context context;
    public final String[] tnrTypeValues, tnrVariantValues;
    public final int[] tnrInitialTimeValues, tnrIncrementValues, tnrDurationValues,
            tnrBreaksValues, tnrRatedGamesValues;

    public TournamentFieldValues(Context context) {
        this.context = context;
        this.tnrTypeValues = context.getResources().getStringArray(R.array.tournament_type_values);
        this.tnrVariantValues = context.getResources().getStringArray(R.array.variant_values);
        this.tnrInitialTimeValues = context.getResources().getIntArray(R.array.initial_time_values);
        this.tnrIncrementValues = context.getResources().getIntArray(R.array.increment_values);
        this.tnrDurationValues = context.getResources().getIntArray(R.array.duration_values);
        this.tnrBreaksValues = context.getResources().getIntArray(R.array.time_between_rounds_values);
        this.tnrRatedGamesValues = context.getResources().getIntArray(R.array.rated_games_values);
    }

    public int getMinRatingValue(int index) {
        return index * 100 + 900;
    }
    public int getMaxRatingValue(int index) {
        return index * 100 + 700;
    }
}
