package com.example.resuscanner.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.animation.ValueAnimator;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import com.example.resuscanner.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ComparisonScoreFragment extends Fragment {
    private static final String TAG = "ComparisonScoreFragment";
    private static final String BASE_URL = "http://10.0.2.2:5001";
    private static final String PREFS_NAME = "ResuScannerPrefs";
    private static final String KEY_SESSION_ID = "resumeSessionId";

//    private ProgressBar progressMatchScore;
    private CircularProgressIndicator progressMatchScore;
    private TextView tvMatchScore;
    private TextView tvMatchExplanation;
    private LinearLayout containerImprovements;
    private String sessionId;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_comparison_score, container, false);
        progressMatchScore    = view.findViewById(R.id.progress_match_score);
        tvMatchScore          = view.findViewById(R.id.tv_match_score);
        tvMatchExplanation    = view.findViewById(R.id.tv_match_explanation);
        containerImprovements = view.findViewById(R.id.container_improvements);

        // Try arguments first
        if (getArguments() != null) {
            sessionId = getArguments().getString(KEY_SESSION_ID);
        }
        // Fallback to SharedPreferences
        if (sessionId == null) {
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            sessionId = prefs.getString(KEY_SESSION_ID, null);
        }

        if (sessionId == null) {
            Toast.makeText(getContext(),
                    "Session ID not found. Please analyze resume and JD first.",
                    Toast.LENGTH_LONG).show();
        } else {
            fetchMatchScore(sessionId);
        }
        return view;
    }

    private void fetchMatchScore(String sessionId) {
        OkHttpClient client = new OkHttpClient();
        JSONObject payload = new JSONObject();
        try { payload.put("session_id", sessionId); }
        catch (JSONException e) {
            Log.e(TAG, "Failed to build JSON payload", e);
            return;
        }

        RequestBody body = RequestBody.create(
                payload.toString(),
                MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url(BASE_URL + "/match-score")
                .post(body)
                .build();

        Log.d(TAG, "Requesting match-score for session_id=" + sessionId);
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network error in match-score", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Network error fetching match score", Toast.LENGTH_SHORT).show()
                );
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String rawBody = response.body() != null ? response.body().string() : "";
//                String rawBody = "{ \"match_score\": { \"error\": \"No JSON block found\", \"raw\": \"Match Score: 65%\\n\\nExplanation:\\n\\nSkill Match: The candidate's technical skills in programming languages (Python, Java, C++, JavaScript) and their knowledge of AWS, Docker, and Jenkins are highly relevant to the job. While the role prefers knowledge of Siemens NX, the candidate's experience with design tools like Figma and their understanding of UI design could be a potential fit. The soft skills required, such as problem-solving, collaboration, and communication, are also well-aligned with the candidate's resume. \\n\\nTools/Technologies Overlap: There is a moderate overlap in the tools and technologies used. The candidate has experience with Visual Studio, IntelliJ, VS Code, AWS, and Kubernetes, which are all relevant to the role. However, there is no mention of specific hardware or test equipment experience, as preferred in the job description. \\n\\nPast Experience Relevance: The candidate's work experience as a Software Engineer and Software Engineering Intern demonstrates their proficiency in the field. While their role at Sophos Technologies might not directly translate to all responsibilities in the Integration and Test Intern position, their experience in software development and engineering concepts will be valuable. \\n\\nCulture Fit: Based on the provided information, the candidate seems to be a good culture fit. Their experience working with a team, collaborating, and communicating effectively aligns with the job's emphasis on working closely with mentors and other employees. \\n\\nGaps or Missing Elements: The resume lacks a clear summary, which could impact the overall impression. Additionally, the job description prefers a GPA of 3.5 or above, and while the candidate's GPA is not mentioned, this could be a potential gap. The candidate's experience is primarily in software engineering, and while it is valuable, it would have been ideal to see more specific experience in integration and test equipment. \\n\\nOverall, the candidate's resume demonstrates a good match in terms of skills, tools, and culture. However, the gaps and missing elements, along with the moderate relevance of past experience, bring the score to 65%.\" }, \"session_id\": \"0e6aad7e-f86f-49a6-8a00-4092d8c14247\" }";
                Log.d(TAG, "match-score rawBody=" + rawBody);
                try {
                    JSONObject json = new JSONObject(rawBody);
                    Object scoreObj = json.get("match_score");
                    int score = extractPercentage(scoreObj);
                    String explanation = "";
                    if (scoreObj instanceof JSONObject) {
                        explanation = ((JSONObject) scoreObj).optString("raw", "");
                    }
                    final int finalScore = score;
                    final String finalExplanation = explanation;
                    requireActivity().runOnUiThread(() -> {
                        animateScore(finalScore);
                        displayImprovements(finalExplanation);
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parse error", e);
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Parse error in match-score response", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private int extractPercentage(Object scoreObj) {
        if (scoreObj instanceof Number) {
            return ((Number) scoreObj).intValue();
        } else if (scoreObj instanceof JSONObject) {
            String raw = ((JSONObject) scoreObj).optString("raw", "");
            Matcher m = Pattern.compile("(\\d+)%").matcher(raw);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }
        return 0;
    }
private void animateScore(int targetScore) {
    CircularProgressIndicator progressIndicator = progressMatchScore;
    TextView scoreText = tvMatchScore;

    ValueAnimator animator = ValueAnimator.ofInt(0, targetScore);
    animator.setDuration(1200); // duration in milliseconds

    animator.addUpdateListener(animation -> {
        int value = (int) animation.getAnimatedValue();
        progressIndicator.setProgress(value);
        scoreText.setText(value + "%");
    });

    animator.start();
}


//    private void displayImprovements(String rawExplanation) {
//        tvMatchExplanation.setText("Explanation:");
//        containerImprovements.removeAllViews();
//        String[] parts = rawExplanation.split("\\n\\n");
//        for (String part : parts) {
//            TextView tv = new TextView(getContext());
//            tv.setText(part.trim());
//            tv.setPadding(0, 8, 0, 8);
//            tv.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
//            containerImprovements.addView(tv);
//        }
//    }
    private void displayImprovements(String rawExplanation) {
        containerImprovements.removeAllViews();  // clear previous
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View card = inflater.inflate(R.layout.item_explanation, containerImprovements, false);
        TextView tv = card.findViewById(R.id.tv_explanation_point);
        tv.setText(rawExplanation.trim());

        containerImprovements.addView(card);
    }
}
