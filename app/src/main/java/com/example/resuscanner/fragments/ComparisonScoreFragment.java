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

    private ProgressBar progressMatchScore;
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
        progressMatchScore.setMax(100);
        final Handler handler = new Handler();
        new Thread(() -> {
            for (int p = 0; p <= targetScore; p++) {
                final int progress = p;
                handler.post(() -> {
                    progressMatchScore.setProgress(progress);
                    tvMatchScore.setText(progress + "%");
                });
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void displayImprovements(String rawExplanation) {
        tvMatchExplanation.setText("Explanation:");
        containerImprovements.removeAllViews();
        String[] parts = rawExplanation.split("\\n\\n");
        for (String part : parts) {
            TextView tv = new TextView(getContext());
            tv.setText(part.trim());
            tv.setPadding(0, 8, 0, 8);
            tv.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
            containerImprovements.addView(tv);
        }
    }
}
