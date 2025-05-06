package com.example.resuscanner.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.resuscanner.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JDAnalysisFragment extends Fragment {
    private View        rootView;
    private EditText    etJobDescription;
    private Button      btnAnalyzeJD;
    private TextView    tvRoleTitle, tvCompany, tvLocation, tvCompensation;
    private ChipGroup   chipRequiredSkills, chipPreferredSkills;
    private LinearLayout containerResponsibilities;
    private ProgressBar progressBar; // Add ProgressBar variable

    // Provided by ResumeAnalysisFragment via arguments
    private String resumeSessionId;

    private static final String BASE_URL = "http://10.0.2.2:5001";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        rootView = inflater.inflate(R.layout.fragment_jd_analysis, container, false);

        etJobDescription         = rootView.findViewById(R.id.et_job_description);
        btnAnalyzeJD             = rootView.findViewById(R.id.btn_analyze_jd);
        tvRoleTitle              = rootView.findViewById(R.id.tv_role_title);
        tvCompany                = rootView.findViewById(R.id.tv_company);
        tvLocation               = rootView.findViewById(R.id.tv_location);
        tvCompensation           = rootView.findViewById(R.id.tv_compensation);
        chipRequiredSkills       = rootView.findViewById(R.id.chipgroup_required_skills);
        chipPreferredSkills      = rootView.findViewById(R.id.chipgroup_preferred_skills);
        containerResponsibilities= rootView.findViewById(R.id.container_responsibilities);
        progressBar              = rootView.findViewById(R.id.progress_bar); // Initialize ProgressBar

        // Retrieve the resumeSessionId passed as an argument
        // at the top of onCreateView() in JDAnalysisFragment
        resumeSessionId = requireActivity().getPreferences(Context.MODE_PRIVATE)
                .getString("resumeSessionId", null);
        if (resumeSessionId == null) {
            Toast.makeText(getContext(), "No resume session ID found. Please analyze a resume first.", Toast.LENGTH_LONG).show();
            // optionally disable the Analyze JD button
            btnAnalyzeJD.setEnabled(false);
        }

        btnAnalyzeJD.setOnClickListener(v -> {
            String jdText = etJobDescription.getText().toString().trim();
            if (jdText.isEmpty()) {
                Toast.makeText(getContext(), "Please paste a job description", Toast.LENGTH_SHORT).show();
            } else if (resumeSessionId == null) {
                Toast.makeText(getContext(), "Missing resume session ID", Toast.LENGTH_SHORT).show();
            } else {
                // Show loading indicator and disable button
                progressBar.setVisibility(View.VISIBLE);
                btnAnalyzeJD.setEnabled(false);

                analyzeJD(resumeSessionId, jdText);
            }
        });

        return rootView;
    }

    private void analyzeJD(String sessionId, String jdText) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("session_id", sessionId);
            payload.put("jd_text", jdText);
        } catch (JSONException e) {
            e.printStackTrace();
            // Hide loading indicator if there's an error
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnAnalyzeJD.setEnabled(true);
            });
            return;
        }

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("ResuScannerPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("resumeSessionId", sessionId)
                .apply();

        postJson("/analyze-jd", payload, json -> {
            JSONObject analysis = json.getJSONObject("analysis");
            requireActivity().runOnUiThread(() -> {
                // Hide loading indicator
                progressBar.setVisibility(View.GONE);
                btnAnalyzeJD.setEnabled(true);

                // Show success dialog
                showSuccessDialog();

                // Populate the analysis results
                populateAnalysis(analysis);
            });
        });
    }

    // Add this new method for showing the success dialog
    private void showSuccessDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Success")
                .setMessage("Job description submitted successfully!")
                .setPositiveButton("OK", null)
                .show();
    }

    private void populateAnalysis(JSONObject a) {
        // Role & Company
        tvRoleTitle.setText(a.optString("role_title", "(none)"));
        tvCompany.setText(a.optString("company", "(none)"));

        // Location & Compensation
        tvLocation.setText(a.optString("location", "(none)"));
        tvCompensation.setText(a.optString("compensation", "(none)"));

        // Required Skills
        chipRequiredSkills.removeAllViews();
        JSONArray req = a.optJSONArray("required_skills");
        if (req != null) {
            for (int i = 0; i < req.length(); i++) {
                Chip c = new Chip(getContext());
                c.setText(req.optString(i));
                chipRequiredSkills.addView(c);
            }
        }

        // Preferred Skills
        chipPreferredSkills.removeAllViews();
        JSONArray pref = a.optJSONArray("preferred_skills");
        if (pref != null) {
            for (int i = 0; i < pref.length(); i++) {
                Chip c = new Chip(getContext());
                c.setText(pref.optString(i));
                chipPreferredSkills.addView(c);
            }
        }

        // Responsibilities
        containerResponsibilities.removeAllViews();
        JSONArray resp = a.optJSONArray("responsibilities");
        if (resp != null) {
            for (int i = 0; i < resp.length(); i++) {
                TextView tv = new TextView(getContext());
                tv.setText("• " + resp.optString(i));
                tv.setPadding(0, 8, 0, 8);
                containerResponsibilities.addView(tv);
            }
        }
    }

    private void postJson(String path, JSONObject payload, JsonCallback cb) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                payload.toString(), MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    // Hide loading indicator in case of network error
                    progressBar.setVisibility(View.GONE);
                    btnAnalyzeJD.setEnabled(true);

                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String s = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject j = new JSONObject(s);
                    cb.onJson(j);
                } catch (JSONException e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(() -> {
                        // Hide loading indicator in case of parse error
                        progressBar.setVisibility(View.GONE);
                        btnAnalyzeJD.setEnabled(true);

                        Toast.makeText(getContext(), "Parse error", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private interface JsonCallback {
        void onJson(JSONObject json) throws JSONException;
    }
}