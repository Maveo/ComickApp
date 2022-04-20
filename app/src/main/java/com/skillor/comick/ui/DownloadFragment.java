package com.skillor.comick.ui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.Navigation;

import com.skillor.comick.R;
import com.skillor.comick.databinding.FragmentDownloadBinding;
import com.skillor.comick.utils.ComickService;

public class DownloadFragment extends Fragment {

    private FragmentDownloadBinding binding;
    private ComickService.Comic cachedComic;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDownloadBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ComickService.getInstance().getError().observe(getViewLifecycleOwner(), new Observer<Exception>() {
            @Override
            public void onChanged(Exception e) {
                if (e == null) {
                    binding.downloadUrlErrorText.setText("");
                } else {
                    binding.downloadUrlErrorText.setText(e.getMessage());
                    binding.cachedComicLoadingSpinner.setVisibility(View.GONE);
                    binding.cachedComicGrid.setVisibility(View.GONE);
                }
            }
        });

        binding.searchUrlButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                binding.cachedComicLoadingSpinner.setVisibility(View.VISIBLE);
                binding.cachedComicGrid.setVisibility(View.GONE);
                ComickService.getInstance().cacheComic(binding.downloadUrlText.getText().toString())
                        .observe(getViewLifecycleOwner(), new Observer<ComickService.Comic>() {
                    @Override
                    public void onChanged(ComickService.Comic c) {
                        if (c != null) {
                            cachedComic = c;
                            binding.cachedComicLoadingSpinner.setVisibility(View.GONE);
                            binding.cachedComicGrid.setVisibility(View.VISIBLE);
                            binding.cachedComicCoverView.setImageBitmap(c.getCoverBitmap());
                            binding.cachedComicTitleView.setText(c.getComicTitle());
                            binding.cachedOnlineLastChapterView.setText(c.getFormattedLastChapterI());
                        }
                    }
                });
                binding.downloadUrlText.setText("");
            }
        });

        binding.cachedComicAddButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (cachedComic != null) {
                    ComickService.getInstance().addCachedComic(cachedComic);
                    Navigation.findNavController(v).navigate(R.id.nav_overview);
                }
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
