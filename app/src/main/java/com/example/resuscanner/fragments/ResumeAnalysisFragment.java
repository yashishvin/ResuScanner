package com.example.resuscanner.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.resuscanner.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

public class ResumeAnalysisFragment extends Fragment {

    private static final int PICK_PDF_REQUEST = 1;
    private TextView resultView;
    private Uri selectedFileUri;
    private final String BASE_URL = "http://10.0.2.2:5001";


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_resume_analysis, container, false);
        Button uploadBtn = root.findViewById(R.id.btn_upload_resume);
        Button analyzeBtn = root.findViewById(R.id.analyzeButton);
        resultView = root.findViewById(R.id.tv_summary);

        uploadBtn.setOnClickListener(v -> selectPdfFile());
        analyzeBtn.setOnClickListener(v -> {
            if (selectedFileUri != null) uploadResume();
            else Toast.makeText(getActivity(), "Please select a file first.", Toast.LENGTH_SHORT).show();
        });

        // To prevent strict policy issues
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        return root;
    }

    private void selectPdfFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_PDF_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
        }
    }

    private void uploadResume() {
        try {
            File file = createFileFromUri(selectedFileUri);

            OkHttpClient client = new OkHttpClient();
            RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/pdf"));
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(BASE_URL + "/upload-resume")
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new Exception("Upload failed");

            JSONObject json = new JSONObject(response.body().string());
//            String sessionId = json.getString("filename").replace(".pdf", ""); // Or get session ID if returned
            String sessionId = json.getString("session_id");
            analyzeResume(sessionId);

            // in ResumeAnalysisFragment, after upload or analyze:
            requireActivity().getPreferences(Context.MODE_PRIVATE)
                    .edit()
                    .putString("resumeSessionId", sessionId)
                    .apply();

        } catch (Exception e) {
            e.printStackTrace();
            resultView.setText("Upload error: " + e.getMessage());
        }
    }

    private void analyzeResume(String sessionId) throws JSONException {
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(30, TimeUnit.SECONDS)
                .build();

        JSONObject payload = new JSONObject();
        payload.put("session_id", sessionId);
        payload.put("user_id", "spartan@sjsu.edu");

        RequestBody body = RequestBody
                .create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(BASE_URL + "/analyze-resume")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() ->
                        resultView.setText("Analysis error: " + e.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String msg = response.body() != null
                            ? response.body().string() : "no body";
                    requireActivity().runOnUiThread(() ->
                            resultView.setText("Analysis failed: " + msg));
                    return;
                }

                JSONObject root = null;
                try {
                    root = new JSONObject(response.body().string());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                try {
                    final JSONObject analysis = root.getJSONObject("analysis");
                    // Switch to UI thread
                    requireActivity().runOnUiThread(() -> {
                        // 1. Summary
                        String summary = analysis.optString("summary", null);
                        TextView tvSum = getView().findViewById(R.id.tv_summary);
                        tvSum.setText(summary != null && !summary.equals("null")
                                ? summary
                                : "(no summary provided)");

                        // 2. Technical Skills
                        ChipGroup techGroup = getView().findViewById(R.id.chipgroup_tech_skills);
                        JSONArray tech = null;
                        try {
                            tech = analysis.getJSONArray("technical_skills");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        for (int i = 0; i < tech.length(); i++) {
                            Chip c = new Chip(getContext());
                            try {
                                c.setText(tech.getString(i));
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            techGroup.addView(c);
                        }

                        // 3. Soft Skills
                        ChipGroup softGroup = getView().findViewById(R.id.chipgroup_soft_skills);
                        JSONArray soft = null;
                        try {
                            soft = analysis.getJSONArray("soft_skills");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        for (int i = 0; i < soft.length(); i++) {
                            Chip c = new Chip(getContext());
                            try {
                                c.setText(soft.getString(i));
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            softGroup.addView(c);
                        }

                        // 4. Tools
                        ChipGroup toolsGroup = getView().findViewById(R.id.chipgroup_tools);
                        JSONArray tools = null;
                        try {
                            tools = analysis.getJSONArray("tools");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        for (int i = 0; i < tools.length(); i++) {
                            Chip c = new Chip(getContext());
                            try {
                                c.setText(tools.getString(i));
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            toolsGroup.addView(c);
                        }

                        // 5. Work Experience
                        LinearLayout expContainer = getView().findViewById(R.id.container_experience);
                        JSONArray expArr = null;
                        try {
                            expArr = analysis.getJSONArray("work_experience");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        for (int i = 0; i < expArr.length(); i++) {
                            JSONObject job = null;
                            try {
                                job = expArr.getJSONObject(i);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            View item = inflater.inflate(R.layout.item_experience, expContainer, false);
                            try {
                                ((TextView)item.findViewById(R.id.tv_company))
                                        .setText(job.getString("company"));
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            try {
                                ((TextView)item.findViewById(R.id.tv_role))
                                        .setText(job.getString("role"));
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            try {
                                ((TextView)item.findViewById(R.id.tv_duration))
                                        .setText(job.getString("duration"));
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            expContainer.addView(item);
                        }

                        // 6. Gaps / Suggestions
                        LinearLayout gapContainer = getView().findViewById(R.id.container_gaps);
                        JSONArray gaps = null;
                        try {
                            gaps = analysis.getJSONArray("gaps");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        for (int i = 0; i < gaps.length(); i++) {
                            TextView gapTv = new TextView(getContext());
                            try {
                                gapTv.setText("•  " + gaps.getString(i));
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            gapTv.setTextAppearance(android.R.style.TextAppearance_Material_Body2);
                            gapTv.setPadding(0, 4, 0, 4);
                            gapContainer.addView(gapTv);
                        }

                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    // show an error on UI thread if you like
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Parse error", Toast.LENGTH_SHORT).show()
                    );
                }


            }
        });
    }

//    private void analyzeResume(String sessionId) {
//        try {
//            OkHttpClient client = new OkHttpClient();
//            JSONObject json = new JSONObject();
//            json.put("session_id", sessionId);
//            json.put("user_id", "spartan@sjsu.edu");
//
//            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
//            Request request = new Request.Builder()
//                    .url(BASE_URL + "/analyze-resume")
//                    .post(body)
//                    .build();
//
//            Response response = client.newCall(request).execute();
////            if (!response.isSuccessful()) throw new Exception("Analysis failed");
//            if (!response.isSuccessful()) {
//                String bodyMsg = response.body() != null ? response.body().string() : "‹no body›";
//                Log.e("ResumeAnalysis", "analysis failed: code="
//                        + response.code() + " body=" + body);
//                throw new Exception("Analysis failed: " + response.code());
//            }
//
//
//            JSONObject analysis = new JSONObject(response.body().string()).getJSONObject("analysis");
//            resultView.setText(analysis.toString(2)); // Pretty print
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            resultView.setText("Analysis error: " + e.getMessage());
//        }
//    }

    private File createFileFromUri(Uri uri) throws Exception {
        InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("resume", ".pdf", getActivity().getCacheDir());
        OutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buf = new byte[4096];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            outputStream.write(buf, 0, len);
        }
        inputStream.close();
        outputStream.close();
        return tempFile;
    }
}
//package com.example.resuscanner.fragments;
//
//import android.os.Bundle;
//import androidx.fragment.app.Fragment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import com.example.resuscanner.R;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
//public class ResumeAnalysisFragment extends Fragment {
//
//    private Button btnUploadResume;
//    private TextView tvResumeAnalysisResult;
//
//    public ResumeAnalysisFragment() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_resume_analysis, container, false);
//
//        // Initialize views
//        btnUploadResume = view.findViewById(R.id.btn_upload_resume);
//        tvResumeAnalysisResult = view.findViewById(R.id.tv_resume_analysis_result);
//
//        // Upload Button Click
//        btnUploadResume.setOnClickListener(v -> {
//            // TODO: Open file picker to upload resume
//            Toast.makeText(getContext(), "Upload Resume clicked!", Toast.LENGTH_SHORT).show();
//            // For now, let's just simulate analysis output
//            tvResumeAnalysisResult.setText("Standout Features:\n• Leadership\n• Communication\n• Python Developer Skills");
//        });
//
//        return view;
//    }
//}
