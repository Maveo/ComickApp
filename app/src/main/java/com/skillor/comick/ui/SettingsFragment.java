package com.skillor.comick.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.skillor.comick.MainActivity;
import com.skillor.comick.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                switch (key) {
                    case "external_file_path":
                        break;
                }
            }
        };

        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(listener);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}