package com.example.resuscanner.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.resuscanner.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InterviewQuestionsFragment extends Fragment {
    private static final String TAG = "InterviewQuestions";
    private static final String BASE_URL = "http://10.0.2.2:5001";
    private static final String PREFS = "ResuScannerPrefs";
    private static final String KEY_SESSION = "resumeSessionId";

    private LinearLayout containerQuestions;
    private String sessionId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interview_questions,
                container, false);
        containerQuestions = view.findViewById(R.id.container_questions);

        // Retrieve sessionId
        if (getArguments() != null) {
            sessionId = getArguments().getString(KEY_SESSION);
        }
        if (sessionId == null) {
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sessionId = prefs.getString(KEY_SESSION, null);
        }

        if (sessionId == null) {
            Toast.makeText(getContext(),
                    "Session ID not found. Analyze resume & JD first.",
                    Toast.LENGTH_LONG).show();
        } else {
            fetchQuestions(sessionId);
        }
        return view;
    }

    private void fetchQuestions(String sessionId) {
        // Increase timeouts for potentially long-running operation
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        JSONObject payload = new JSONObject();
        try {
            payload.put("session_id", sessionId);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build JSON payload", e);
            return;
        }

        RequestBody body = RequestBody.create(
                payload.toString(), MediaType.parse("application/json")
        );
        Request req = new Request.Builder()
                .url(BASE_URL + "/generate-questions")
                .post(body)
                .build();

        Log.d(TAG, "Requesting questions for session_id=" + sessionId);
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network error generating questions", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Network error fetching questions: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response) throws IOException {
                String raw = response.body() != null
                        ? response.body().string() : "";
                Log.d(TAG, "generate-questions raw response: " + raw);
                try {
                    JSONObject json = new JSONObject(raw);
                    Object qObj = json.get("questions");
                    JSONArray qs = new JSONArray();
                    if (qObj instanceof JSONArray) {
                        qs = (JSONArray) qObj;
                    } else if (qObj instanceof String) {
                        String[] lines = ((String) qObj).split("\\r?\\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                qs.put(line);
                            }
                        }
                    } else if (qObj instanceof JSONObject) {
                        String rawQs = ((JSONObject) qObj).optString("raw", "");
                        String[] lines = rawQs.split("\\r?\\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                qs.put(line);
                            }
                        }
                    }
                    final JSONArray finalQs = qs;
                    requireActivity().runOnUiThread(() -> displayQuestions(finalQs));
                } catch (JSONException e) {
                    Log.e(TAG, "Parse error in questions response", e);
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Parse error in questions response", Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }

    private void displayQuestions(JSONArray questions) {
        containerQuestions.removeAllViews();
        for (int i = 0; i < questions.length(); i++) {
            String q;
            try {
                q = questions.getString(i);
            } catch (JSONException e) {
                q = "";
            }
            TextView tv = new TextView(getContext());
            tv.setText((i + 1) + ". " + q);
            tv.setTextSize(16f);
            tv.setPadding(0, 8, 0, 8);
            containerQuestions.addView(tv);
        }
    }
}
