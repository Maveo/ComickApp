package com.skillor.comick.ui.reader;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.skillor.comick.MainActivity;
import com.skillor.comick.databinding.FragmentReaderBinding;
import com.skillor.comick.utils.ComickService;

public class ReaderFragment extends Fragment {

    private ReaderViewModel readerViewModel;
    private FragmentReaderBinding binding;

    private WebView readerWebView;

    private boolean ignoreNextTap = false;

    private ComickService.Comic comic;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        readerViewModel = new ViewModelProvider(this).get(ReaderViewModel.class);

        binding = FragmentReaderBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        readerWebView = binding.readerWebview;

        readerWebView.getSettings().setAllowFileAccess(true);

        readerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.endsWith("next_chapter")) {
                    ignoreNextTap = true;
                    comic.addChapter(1);
                    refreshWebview();
                    return true;
                } else if (url.endsWith("prev_chapter")) {
                    ignoreNextTap = true;
                    comic.addChapter(-1);
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

        if (getArguments() != null) {
            comic = ComickService.getInstance().getComicByTitle(getArguments().getString("comic_title"));
        } else {
            comic = ComickService.getInstance().getLastReadComic();
        }

        refreshWebview();

        return root;
    }

    private void refreshWebview() {
        readerWebView.loadDataWithBaseURL("file://"+comic.getCurrentChapterPath()+"/", createHtml(), "text/html", "UTF-8", null);
    }

    @Override
    public void onDestroyView() {
        ((MainActivity)getActivity()).showSystemUI();
        super.onDestroyView();
        binding = null;
    }

    private String createHtml() {
        StringBuilder builder = new StringBuilder();
        for (String img: comic.getCurrentChapterImages()) {
            builder.append("<img src=\"");
            builder.append(img);
            builder.append("\">");
        }
        return "<html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\"><title>Comick</title><style type=\"text/css\">html, body {width: 100%; height: 100%; margin: 0;}.outer-container {display: flex;justify-content: center;}.inner-container {max-width: 500px;display: flex;flex-direction: column;}#images-container {display: flex;flex-direction: column;}a {border-radius: 4px;background: #4479BA;color: #FFF;padding: 8px 12px;text-decoration: none;}.left {float: left;}.right {float: right;}img {width: 100%;}</style></head><body><div class=\"outer-container\"><div class=\"inner-container\"><div>" +
                (comic.hasPrevChapter() ? "<a class=\"left\" href=\"prev_chapter\">Prev Chapter</a>" : "") +
                (comic.hasNextChapter() ? "<a class=\"right\" href=\"next_chapter\">Next Chapter</a>" : "") +
                "</div><h1>Chapter "+comic.getCurrentChapterTitle()+"</h1><div id=\"images-container\">" +
                builder.toString() +
                "</div><h1>Chapter "+comic.getCurrentChapterTitle()+"</h1><div>" +
                (comic.hasPrevChapter() ? "<a class=\"left\" href=\"prev_chapter\">Prev Chapter</a>" : "") +
                (comic.hasNextChapter() ? "<a class=\"right\" href=\"next_chapter\">Next Chapter</a>" : "") +
                "</div></div></div></body></html>";
    }
}