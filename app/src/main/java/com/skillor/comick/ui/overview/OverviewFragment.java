package com.skillor.comick.ui.overview;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.skillor.comick.MainActivity;
import com.skillor.comick.R;
import com.skillor.comick.databinding.FragmentOverviewBinding;
import com.skillor.comick.utils.ComickService;

import java.util.List;

public class OverviewFragment extends Fragment {

    private OverviewViewModel overviewViewModel;
    private FragmentOverviewBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
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

        Button button = binding.downloadUrlButton;
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ComickService.getInstance().downloadComic(binding.downloadUrlText.getText().toString());
            }
        });

        List<ComickService.Comic> comics = ComickService.getInstance().getComics().getValue();
        ComicListAdapter comicListAdapter = new ComicListAdapter(getActivity(), ComickService.getInstance().getComics().getValue());
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
    private Activity context;

    public ComicListAdapter(Activity context, List<ComickService.Comic> comics) {
        super(context, R.layout.overview_item, comics);
        this.context = context;
        this.comics = comics;
    }

    public List<ComickService.Comic> getComics() {
        return comics;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        LayoutInflater inflater = context.getLayoutInflater();
        if(convertView==null) row = inflater.inflate(R.layout.overview_item, null, true);
        TextView comicTitleView = (TextView) row.findViewById(R.id.comicTitleView);
        ImageView comicCoverView = (ImageView) row.findViewById(R.id.comicCoverView);
        ComickService.Comic comic = this.comics.get(position);
        comicTitleView.setText(comic.getComicTitle());
        comicCoverView.setImageBitmap(comic.getCoverBitmap());
        return row;
    }
}
