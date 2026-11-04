package com.naturia.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.naturia.app.ui.SettingsActivity;

/** Applies the saved day/night preference before any activity inflates. */
public class NaturiaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        int mode = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE)
                .getInt(SettingsActivity.KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
