package com.jimnastic.modernscramblednet;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity
{
    public static Integer EasyHeight;
    public static Integer EasyWidth;
    public static boolean AnimationState;
    public static String SoundString;
    public static Integer LongPressDelay;
    public static MainActivity.SoundMode SoundState()
    {
        if (SoundString == null)
        {
            return MainActivity.SoundMode.FULL;
        }

        try
        {
            return MainActivity.SoundMode.valueOf(SoundString);
        }
        catch (IllegalArgumentException e)
        {
            // Handle legacy numeric values
            if ("0".equals(SoundString)) return MainActivity.SoundMode.NONE;
            if ("1".equals(SoundString)) return MainActivity.SoundMode.QUIET;
            if ("2".equals(SoundString)) return MainActivity.SoundMode.FULL;

            return MainActivity.SoundMode.FULL;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null)
        {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

    }

    public static class SettingsFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference animation = findPreference("AnimationPreference");
            Preference sound = findPreference("SoundPreference");
            Preference longPressDelay = findPreference("LongPressPreference");
            Preference easyHeight = findPreference("EasyHeightPreference");
            Preference easyWidth = findPreference("EasyWidthPreference");

            assert animation != null;
            animation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    AnimationState = (boolean) newValue;
                    return true;
                }
            });

            assert sound != null;
            sound.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SoundString = newValue.toString();
                    return true;
                }
            });

            assert longPressDelay != null;
            longPressDelay.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    LongPressDelay = Integer.parseInt(newValue.toString());
                    return true;
                }
            });

            assert easyHeight != null;
            easyHeight.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    EasyHeight = Integer.parseInt(newValue.toString());
                    return true;
                }
            });

            assert easyWidth != null;
            easyWidth.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    EasyWidth = Integer.parseInt(newValue.toString());
                    return true;
                }
            });
        }
    }
}