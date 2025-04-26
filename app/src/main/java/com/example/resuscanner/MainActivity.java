package com.example.resuscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.example.resuscanner.fragments.ResumeAnalysisFragment;
import com.example.resuscanner.fragments.JDAnalysisFragment;
import com.example.resuscanner.fragments.ComparisonScoreFragment;
import com.example.resuscanner.fragments.InterviewQuestionsFragment;
import com.example.resuscanner.fragments.HistoryFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load default fragment - Resume Analysis
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ResumeAnalysisFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.nav_resume_analysis) {
                selectedFragment = new ResumeAnalysisFragment();
            } else if (itemId == R.id.nav_jd_analysis) {
                selectedFragment = new JDAnalysisFragment();
            } else if (itemId == R.id.nav_comparison) {
                selectedFragment = new ComparisonScoreFragment();
            } else if (itemId == R.id.nav_questions) {
                selectedFragment = new InterviewQuestionsFragment();
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }
}
