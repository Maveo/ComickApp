package com.skillor.comick.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.skillor.comick.MainActivity;
import com.skillor.comick.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    private void refreshScreen() {
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.root_preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals(getString(R.string.use_external_files_key))) {
                    ((MainActivity) getActivity()).loadDirectory();
                    refreshScreen();
                } else if (key.equals(getString(R.string.external_file_path_key))) {
                    ((MainActivity) getActivity()).loadDirectory();
                    refreshScreen();
                }
            }
        };

        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(listener);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}