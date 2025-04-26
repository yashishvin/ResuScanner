package com.example.resuscanner.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.resuscanner.R;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class JDAnalysisFragment extends Fragment {

    private EditText etJobDescription;
    private Button btnAnalyzeJD;
    private TextView tvJDAnalysisResult;

    public JDAnalysisFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_jd_analysis, container, false);

        etJobDescription = view.findViewById(R.id.et_job_description);
        btnAnalyzeJD = view.findViewById(R.id.btn_analyze_jd);
        tvJDAnalysisResult = view.findViewById(R.id.tv_jd_analysis_result);

        btnAnalyzeJD.setOnClickListener(v -> {
            String jdText = etJobDescription.getText().toString();
            if (jdText.isEmpty()) {
                Toast.makeText(getContext(), "Please enter Job Description", Toast.LENGTH_SHORT).show();
            } else {
                tvJDAnalysisResult.setText("Skills you must highlight:\n• Java\n• Android\n• REST APIs\n• Communication Skills");
            }
        });

        return view;
    }
}