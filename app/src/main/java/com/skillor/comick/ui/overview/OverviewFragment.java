package com.skillor.comick.ui.overview;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.skillor.comick.MainActivity;
import com.skillor.comick.R;
import com.skillor.comick.databinding.FragmentOverviewBinding;
import com.skillor.comick.utils.ComickService;

import java.util.ArrayList;
import java.util.List;

public class OverviewFragment extends Fragment {

    private OverviewViewModel overviewViewModel;
    private FragmentOverviewBinding binding;

    private ComicListAdapter comicListAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        overviewViewModel = new ViewModelProvider(this).get(OverviewViewModel.class);

        binding = FragmentOverviewBinding.inflate(inflater, container, false);
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
                if (binding.downloadUrlText.getText().toString().equals("")) {
                    ComickService.getInstance().updateAllComicData();
                } else {
                    ComickService.getInstance().downloadComic(binding.downloadUrlText.getText().toString());
                    binding.downloadUrlText.setText("");
                }
            }
        });

        binding.downloadUrlText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    downloadUrlButton.setText(R.string.update_all_comics);
                } else {
                    downloadUrlButton.setText(R.string.download_comic);
                }
            }
        });

        List<ComickService.Comic> comics = ComickService.getInstance().getComics().getValue();
        comicListAdapter = new ComicListAdapter(this, new ArrayList<>(comics), getViewLifecycleOwner());
        ListView comicListView = binding.comicList;
        comicListView.setAdapter(comicListAdapter);
        ComickService.getInstance().getComics().observe(getViewLifecycleOwner(), new Observer<List<ComickService.Comic>>() {
            @Override
            public void onChanged(List<ComickService.Comic> comics) {
                comicListAdapter.getComics().clear();
                comicListAdapter.getComics().addAll(comics);
                comicListAdapter.notifyDataSetChanged();
            }
        });

        comicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Log.d("Click", String.valueOf(position));
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

class ComicListAdapter extends ArrayAdapter {
    private List<ComickService.Comic> comics;
    private OverviewFragment fragment;
    private LifecycleOwner lifecycleOwner;

    public ComicListAdapter(OverviewFragment fragment, List<ComickService.Comic> comics, LifecycleOwner lifecycleOwner) {
        super((MainActivity)fragment.getActivity(), R.layout.overview_item, comics);

        this.fragment = fragment;

        this.lifecycleOwner = lifecycleOwner;
        this.comics = comics;
    }

    public List<ComickService.Comic> getComics() {
        return comics;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        LayoutInflater inflater = fragment.getActivity().getLayoutInflater();
        if(convertView==null) row = inflater.inflate(R.layout.overview_item, null, true);
        TextView comicTitleView = (TextView) row.findViewById(R.id.comicTitleView);
        TextView onlineLastChapterView = (TextView) row.findViewById(R.id.onlineLastChapterView);
        TextView downloadedLastChapterView = (TextView) row.findViewById(R.id.downloadedLastChapterView);
        TextView readingChapterView = (TextView) row.findViewById(R.id.readingChapterView);
        ImageView comicCoverView = (ImageView) row.findViewById(R.id.comicCoverView);
        Button updateButton = (Button) row.findViewById(R.id.updateButton);
        Button readButton = (Button) row.findViewById(R.id.readButton);
        ProgressBar loadingSpinner = (ProgressBar) row.findViewById(R.id.loadingSpinner);

        ComickService.Comic comic = this.comics.get(position);

        updateButton.setVisibility(View.VISIBLE);
        loadingSpinner.setVisibility(View.GONE);
        if (comic.isUpdating()) {
            loadingSpinner.setVisibility(View.VISIBLE);
            updateButton.setVisibility(View.GONE);
        }

        if (comic.getDownloadedLastChapterI() == null) {
            readButton.setVisibility(View.GONE);
        } else {
            readButton.setVisibility(View.VISIBLE);
        }


        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ComickService.getInstance().updateComic(position);
                ComicListAdapter.this.notifyDataSetChanged();
            }
        });

        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("comic_title", comic.getComicTitle());
                Navigation.findNavController(v).popBackStack();
                Navigation.findNavController(v).navigate(R.id.nav_reader, bundle);
            }
        });
        comicTitleView.setText(comic.getComicTitle());
        onlineLastChapterView.setText(comic.getFormattedLastChapterI());
        readingChapterView.setText(comic.getCurrentChapterTitle());

        comic.getDownloadedLastChapterText().observe(lifecycleOwner, new Observer<String>() {
            @Override
            public void onChanged(String downloadedLastChapterText) {
                ComicListAdapter.this.notifyDataSetChanged();

            }
        });
        downloadedLastChapterView.setText(comic.getDownloadedLastChapterText().getValue());


        comicCoverView.setImageBitmap(comic.getCoverBitmap());
        return row;
    }
}
