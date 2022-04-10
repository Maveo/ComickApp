package com.skillor.comick.ui.reader;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.skillor.comick.MainActivity;
import com.skillor.comick.databinding.FragmentReaderBinding;

public class ReaderFragment extends Fragment {

    private ReaderViewModel readerViewModel;
    private FragmentReaderBinding binding;

    private WebView readerWebView;

    private boolean ignoreNextTap = false;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        readerViewModel = new ViewModelProvider(this).get(ReaderViewModel.class);

        binding = FragmentReaderBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        readerWebView = binding.readerWebview;

        ((MainActivity)getActivity()).hideNavbar();

        readerWebView.getSettings().setAllowFileAccess(true);
        readerWebView.getSettings().setJavaScriptEnabled(true);

        readerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                ignoreNextTap = true;
                return super.shouldOverrideUrlLoading(view, url);
            }

//            @Override
//            public void onPageFinished(WebView view, String url) {
//                ((TextView) findViewById(R.id.textView3)).setText(url);
//            }
        });

        readerWebView.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (!ignoreNextTap) {
                        ((MainActivity)getActivity()).triggerSystemUI();
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

        readerWebView.loadUrl("file://storage");

        return root;
    }

    @Override
    public void onDestroyView() {
        ((MainActivity)getActivity()).showSystemUI();
        super.onDestroyView();
        binding = null;
    }
}