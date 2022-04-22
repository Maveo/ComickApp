package com.skillor.comick.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.Navigation;

import com.skillor.comick.MainActivity;
import com.skillor.comick.R;
import com.skillor.comick.databinding.FragmentReaderBinding;
import com.skillor.comick.utils.ComickService;

public class ReaderFragment extends Fragment {

    private FragmentReaderBinding binding;

    private WebView readerWebView;

    private boolean ignoreNextTap = false;

    private ComickService.Comic comic;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentReaderBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        readerWebView = binding.readerWebview;

        readerWebView.getSettings().setAllowFileAccess(true);
        readerWebView.getSettings().setJavaScriptEnabled(true);

        readerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.endsWith("next_chapter")) {
                    ignoreNextTap = true;
                    comic.nextChapter();
                    refreshWebview();
                    return true;
                } else if (url.endsWith("prev_chapter")) {
                    ignoreNextTap = true;
                    comic.prevChapter();
                    refreshWebview();
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        readerWebView.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (!ignoreNextTap) {
                        ((MainActivity)getActivity()).triggerUI();
                    }
                    ignoreNextTap = false;
                    return super.onSingleTapConfirmed(e);
                }

            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });

        if (getArguments() != null && getArguments().containsKey("comic_title")) {
            comic = ComickService.getInstance().getComicByTitle(getArguments().getString("comic_title"));
        } else {
            comic = ComickService.getInstance().getLastReadComic();
        }

        if (comic == null || comic.getCurrentChapterI() == null) {
            Navigation.findNavController(container).popBackStack();
            Navigation.findNavController(container).navigate(R.id.nav_overview);
            return root;
        }

        SharedPreferences.Editor edit = ((MainActivity) requireActivity()).getSharedPrefEditor();
        edit.putString(getString(R.string.last_read_key), comic.getComicTitle());
        edit.commit();

        refreshWebview();

        return root;
    }

    private void refreshWebview() {
        readerWebView.loadDataWithBaseURL("file://"+comic.getCurrentChapter().getPath()+"/", createHtml(ComickService.getInstance().getChapterImages(comic.getCurrentChapter())), "text/html", "UTF-8", null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private String createHtml(String[][] chapterImages) {
        ComickService.Comic.Chapter currentChapter = comic.getCurrentChapter();
        StringBuilder builder = new StringBuilder();
        builder.append("<html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\"><title>Comick</title><style type=\"text/css\">html,body{width: 100%;height:100%;margin:0;}.outer-container{width:100%;display:flex;justify-content:center;}.inner-container{width:100%;max-width:500px;display:flex;flex-direction:column;}#images-container{display:flex;flex-direction:column;}a{border-radius:4px;background:#4479BA;color:#FFF;padding:8px 12px;text-decoration:none;}.left{float:left;}.right{float:right;}img{width:100%;}</style></head><body><div class=\"outer-container\"><div class=\"inner-container\"><div>");
        if (comic.hasPrevChapter()) {
            builder.append("<a class=\"left\" href=\"prev_chapter\">").append(getString(R.string.prev_chapter)).append("</a>");
        }
        if (comic.hasNextChapter()) {
            builder.append("<a class=\"right\" href=\"next_chapter\">").append(getString(R.string.next_chapter)).append("</a>");
        }
        builder.append("</div><h1>").append(getString(R.string.chapter_identifier, currentChapter.getFormattedI())).append("</h1><div id=\"images-container\">");
        if (ComickService.getInstance().isOffline() && !currentChapter.isDownloaded()) {
            builder.append("<h2>").append(getString(R.string.reading_offline)).append("</h2>");
        } else {
            for (String[] img: chapterImages) {
                builder.append("<img alt=\"\" src=\"");
                builder.append(img[0]);
                builder.append("\"");
                if (img.length > 1) {
                    builder.append(" onerror=\"this.onerror=null;this.src='");
                    builder.append(img[1]);
                    builder.append("';\"");
                }
                builder.append(">");
            }
        }

        builder.append("</div><h1>").append(getString(R.string.chapter_identifier, currentChapter.getFormattedI())).append("</h1><div>");
        if (comic.hasPrevChapter()) {
            builder.append("<a class=\"left\" href=\"prev_chapter\">").append(getString(R.string.prev_chapter)).append("</a>");
        }
        if (comic.hasNextChapter()) {
            builder.append("<a class=\"right\" href=\"next_chapter\">").append(getString(R.string.next_chapter)).append("</a>");
        }
        builder.append("</div></div></div></body></html>");
        return builder.toString();
    }
}