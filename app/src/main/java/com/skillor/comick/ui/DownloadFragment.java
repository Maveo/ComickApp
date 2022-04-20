package com.skillor.comick.ui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.skillor.comick.databinding.FragmentDownloadBinding;
import com.skillor.comick.utils.ComickService;

public class DownloadFragment extends Fragment {

    private FragmentDownloadBinding binding;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDownloadBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView downloadUrlErrorText = binding.downloadUrlErrorText;
        ComickService.getInstance().getErrorText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                downloadUrlErrorText.setText(s);
            }
        });

        Button downloadUrlButton = binding.downloadUrlButton;
        downloadUrlButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ComickService.getInstance().downloadComic(binding.downloadUrlText.getText().toString());
                binding.downloadUrlText.setText("");
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
