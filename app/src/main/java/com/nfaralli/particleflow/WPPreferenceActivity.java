package com.nfaralli.particleflow;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class WPPreferenceActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // use old API for backward compatibility.
        addPreferencesFromResource(R.xml.wallpaper_preference);
    }
}
