package com.example.resuscanner.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.resuscanner.R;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ComparisonScoreFragment extends Fragment {

    private ProgressBar progressMatchScore;
    private TextView tvMatchScore;

    public ComparisonScoreFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comparison_score, container, false);

        progressMatchScore = view.findViewById(R.id.progress_match_score);
        tvMatchScore = view.findViewById(R.id.tv_match_score);

        simulateScoreAnimation();

        return view;
    }

    private void simulateScoreAnimation() {
        final int score = 78; // Dummy score out of 100
        final Handler handler = new Handler();
        final int[] progressStatus = {0};

        new Thread(() -> {
            while (progressStatus[0] < score) {
                progressStatus[0]++;
                handler.post(() -> {
                    progressMatchScore.setProgress(progressStatus[0]);
                    tvMatchScore.setText(progressStatus[0] + "%");
                });
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
