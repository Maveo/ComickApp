package com.skillor.comick.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.skillor.comick.MainActivity;
import com.skillor.comick.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    private void refreshScreen() {
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.root_preferences);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals(getString(R.string.use_external_files_key))) {
                    ((MainActivity) requireActivity()).loadDirectory();
                    refreshScreen();
                } else if (key.equals(getString(R.string.external_file_path_key))) {
                    ((MainActivity) requireActivity()).loadDirectory();
                    refreshScreen();
                }
            }
        };

        PreferenceManager.getDefaultSharedPreferences(requireActivity()).registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onDetach() {
        PreferenceManager.getDefaultSharedPreferences(requireActivity()).unregisterOnSharedPreferenceChangeListener(listener);
        super.onDetach();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}