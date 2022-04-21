package com.skillor.comick.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.Navigation;

import com.skillor.comick.R;
import com.skillor.comick.databinding.FragmentOverviewBinding;
import com.skillor.comick.utils.ComickService;

import java.util.ArrayList;
import java.util.List;

public class OverviewFragment extends Fragment {

    private FragmentOverviewBinding binding;

    private ComicListAdapter comicListAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentOverviewBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.updateAllButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ComickService.getInstance().updateAllComicData();
            }
        });

        List<ComickService.Comic> comics = ComickService.getInstance().getComics().getValue();
        comicListAdapter = new ComicListAdapter(this, new ArrayList<>());
        binding.comicList.setAdapter(comicListAdapter);
        ComickService.getInstance().getComics().observe(getViewLifecycleOwner(), new Observer<List<ComickService.Comic>>() {
            @Override
            public void onChanged(List<ComickService.Comic> comics) {
                comicListAdapter.getComics().clear();
                comicListAdapter.getComics().addAll(comics);
                comicListAdapter.notifyDataSetChanged();
            }
        });

        binding.sortAZButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ComickService.getInstance().getSortedValue() == ComickService.SORTED_AZ_ASC) {
                    ComickService.getInstance().setSorted(ComickService.SORTED_AZ_DESC);
                } else {
                    ComickService.getInstance().setSorted(ComickService.SORTED_AZ_ASC);
                }
            }
        });

        binding.sortAddedButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ComickService.getInstance().getSortedValue() == ComickService.SORTED_ADDED_DESC) {
                    ComickService.getInstance().setSorted(ComickService.SORTED_ADDED_ASC);
                } else {
                    ComickService.getInstance().setSorted(ComickService.SORTED_ADDED_DESC);
                }
            }
        });

        ComickService.getInstance().getSorted().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer sorted) {
                if (sorted != null) {
                    switch (sorted) {
                        case ComickService.SORTED_AZ_DESC:
                            binding.sortAZButton.setText(R.string.sort_za);
                            binding.sortAZButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_up_float, 0, 0, 0);
                            break;
                        case ComickService.SORTED_AZ_ASC:
                            binding.sortAZButton.setText(R.string.sort_az);
                            binding.sortAZButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_down_float, 0, 0, 0);
                            break;
                        case ComickService.SORTED_ADDED_ASC:
                            binding.sortAddedButton.setText(R.string.sort_added);
                            binding.sortAddedButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_up_float, 0, 0, 0);
                            break;
                        default:
                            binding.sortAddedButton.setText(R.string.sort_added);
                            binding.sortAddedButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_down_float, 0, 0, 0);
                            break;
                    }
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

class ComicListAdapter extends ArrayAdapter<ComickService.Comic> {
    private final List<ComickService.Comic> comics;
    private final OverviewFragment fragment;

    public ComicListAdapter(OverviewFragment fragment, List<ComickService.Comic> comics) {
        super(fragment.requireActivity(), R.layout.overview_comic_item, comics);

        this.fragment = fragment;

        this.comics = comics;
    }

    public List<ComickService.Comic> getComics() {
        return comics;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        LayoutInflater inflater = fragment.requireActivity().getLayoutInflater();
        if(convertView==null) row = inflater.inflate(R.layout.overview_comic_item, null, true);
        TextView comicTitleView = row.findViewById(R.id.comicTitleView);
        TextView onlineLastChapterView = row.findViewById(R.id.onlineLastChapterView);
        TextView readingChapterView = row.findViewById(R.id.readingChapterView);
        ImageView comicCoverView = row.findViewById(R.id.comicCoverView);
        Button summaryButton = row.findViewById(R.id.summaryButton);
        Button readButton = row.findViewById(R.id.readButton);

        ComickService.Comic comic = getComics().get(position);

        summaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("comic_title", comic.getComicTitle());
                Navigation.findNavController(v).popBackStack();
                Navigation.findNavController(v).navigate(R.id.nav_summary, bundle);
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
        readingChapterView.setText(comic.getCurrentChapter().getFormattedI());

        comicCoverView.setImageBitmap(comic.getCoverBitmap());
        return row;
    }
}
