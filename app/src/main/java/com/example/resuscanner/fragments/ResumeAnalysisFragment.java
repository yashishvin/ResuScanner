package com.example.resuscanner.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.resuscanner.R;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ResumeAnalysisFragment extends Fragment {

    private Button btnUploadResume;
    private TextView tvResumeAnalysisResult;

    public ResumeAnalysisFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resume_analysis, container, false);

        // Initialize views
        btnUploadResume = view.findViewById(R.id.btn_upload_resume);
        tvResumeAnalysisResult = view.findViewById(R.id.tv_resume_analysis_result);

        // Upload Button Click
        btnUploadResume.setOnClickListener(v -> {
            // TODO: Open file picker to upload resume
            Toast.makeText(getContext(), "Upload Resume clicked!", Toast.LENGTH_SHORT).show();
            // For now, let's just simulate analysis output
            tvResumeAnalysisResult.setText("Standout Features:\n• Leadership\n• Communication\n• Python Developer Skills");
        });

        return view;
    }
}
