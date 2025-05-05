package com.example.resuscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;  // Add this import
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.resuscanner.fragments.ResumeAnalysisFragment;
import com.example.resuscanner.fragments.JDAnalysisFragment;
import com.example.resuscanner.fragments.ComparisonScoreFragment;
import com.example.resuscanner.fragments.InterviewQuestionsFragment;
import com.example.resuscanner.fragments.HistoryFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Set up the Toolbar - ADD THIS CODE
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Optionally set a title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("ResuScanner");
        }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle sign out action
        if (id == R.id.action_sign_out) {
            signOut();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        // Sign out from Firebase
        mAuth.signOut();

        // Show a message
        Toast.makeText(MainActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();

        // Navigate back to login screen
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        // Clear the back stack so user can't go back to MainActivity after logout
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}