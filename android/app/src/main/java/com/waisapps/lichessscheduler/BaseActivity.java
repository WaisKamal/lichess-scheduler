package com.waisapps.lichessscheduler;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    protected String activityTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set theme
        String currentThemeSetting = getAppThemeSetting();
        if (currentThemeSetting.equals("deviceDefault")) {
            if (isDarkThemeEnabled()) {
                activityTheme = "dark";
                setTheme(R.style.ThemeDark_LichessScheduler);
            } else {
                activityTheme = "light";
                setTheme(R.style.Theme_LichessScheduler);
            }
        } else {
            activityTheme = currentThemeSetting;
            setTheme(activityTheme.equals("light") ? R.style.Theme_LichessScheduler : R.style.ThemeDark_LichessScheduler);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentThemeSetting = getAppThemeSetting();
        if (currentThemeSetting.equals("deviceDefault")) {
            if (activityTheme.equals("light") ^ !isDarkThemeEnabled()) {
                recreate();
            }
        } else if (!activityTheme.equals(currentThemeSetting)) {
            recreate();
        }
    }

    protected boolean isDarkThemeEnabled() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    protected String getAppThemeSetting() {
        return getSharedPreferences("com.waisapps.lichessscheduler.settings", MODE_PRIVATE)
                .getString("appThemeSetting", "deviceDefault");
    }
}
