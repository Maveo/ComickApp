package com.skillor.comick.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.Navigation;

import com.skillor.comick.R;
import com.skillor.comick.databinding.FragmentSummaryBinding;
import com.skillor.comick.utils.ComickService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class SummaryFragment extends Fragment {

    private FragmentSummaryBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSummaryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(container).popBackStack();
                Navigation.findNavController(container).navigate(R.id.nav_overview);
            }
        });

        ComickService.Comic comic;
        if (getArguments() != null) {
            comic = ComickService.getInstance().getComicByTitle(getArguments().getString("comic_title"));
        } else {
            comic = ComickService.getInstance().getLastReadComic();
        }

        if (comic == null || comic.getCurrentChapterI() == null) {
            Navigation.findNavController(container).popBackStack();
            Navigation.findNavController(container).navigate(R.id.nav_overview);
            return root;
        }

        binding.summaryComicCoverView.setImageBitmap(comic.getCoverBitmap());
        binding.summaryComicTitleView.setText(comic.getComicTitle());

        binding.readingChapterView.setText(comic.getCurrentChapter().getFormattedI());

        binding.readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("comic_title", comic.getComicTitle());
                Navigation.findNavController(v).popBackStack();
                Navigation.findNavController(v).navigate(R.id.nav_reader, bundle);
            }
        });


        ArrayList<ComickService.Comic.Chapter> chapters = new ArrayList<>(comic.getChapters());
        Collections.sort(chapters, new Comparator<ComickService.Comic.Chapter>() {
            @Override
            public int compare(ComickService.Comic.Chapter o1, ComickService.Comic.Chapter o2) {
                return o1.getChapterI().compareTo(o2.getChapterI());
            }
        });

        ChapterAdapter chapterAdapter = new ChapterAdapter(this, chapters, comic);
        binding.chapterList.setAdapter(chapterAdapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

class ChapterAdapter extends ArrayAdapter<ComickService.Comic.Chapter> {
    private final SummaryFragment fragment;
    private final List<ComickService.Comic.Chapter> chapters;
    private final ComickService.Comic comic;

    public ChapterAdapter(SummaryFragment fragment, List<ComickService.Comic.Chapter> chapters, ComickService.Comic comic) {
        super(fragment.requireActivity(), R.layout.summary_chapter_item, chapters);
        this.fragment = fragment;
        this.chapters = chapters;
        this.comic = comic;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        LayoutInflater inflater = fragment.requireActivity().getLayoutInflater();
        if(convertView==null) row = inflater.inflate(R.layout.summary_chapter_item, null, true);

        ComickService.Comic.Chapter chapter = chapters.get(position);

        Button downloadDeleteButton = row.findViewById(R.id.downloadDeleteButton);
        ProgressBar downloadProgress = row.findViewById(R.id.downloadProgress);
        TextView chapterTitleView = row.findViewById(R.id.chapterTitleView);
        Button readButton = row.findViewById(R.id.readButton);

        chapterTitleView.setText(fragment.getString(R.string.chapter_identifier, chapter.getFormattedI()));

        chapter.getDownloading().observe(fragment.getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer progress) {
                if (chapter.isDownloading()) {
                    downloadProgress.setProgress(chapter.getDownloading().getValue());
                    downloadProgress.setVisibility(View.VISIBLE);
                    downloadDeleteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.picture_frame, 0, 0, 0);
                } else if (chapter.isDownloaded()) {
                    downloadProgress.setVisibility(View.GONE);
                    downloadDeleteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_delete, 0, 0, 0);
                } else {
                    downloadProgress.setVisibility(View.GONE);
                    downloadDeleteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.stat_sys_download, 0, 0, 0);
                }
                notifyDataSetChanged();
            }
        });

        downloadDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chapter.isDownloading()) {
                    chapter.abortDownload();
                } else if (chapter.isDownloaded()) {
                    ComickService.getInstance().deleteChapter(chapter);
                } else {
                    ComickService.getInstance().downloadChapter(chapter);
                }
            }
        });


        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comic.setCurrentChapterI(chapter.getChapterI());
                Bundle bundle = new Bundle();
                bundle.putString("comic_title", comic.getComicTitle());
                Navigation.findNavController(v).popBackStack();
                Navigation.findNavController(v).navigate(R.id.nav_reader, bundle);
            }
        });

        return row;
    }
}
