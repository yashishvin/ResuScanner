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

public class HistoryFragment extends Fragment {
    private static final String TAG = "HistoryFragment";
    private static final String BASE_URL = "http://10.0.2.2:5001";
    private static final String PREFS = "ResuScannerPrefs";
    private static final String KEY_SESSION = "resumeSessionId";

    private LinearLayout containerHistory;
    private String sessionId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        containerHistory = view.findViewById(R.id.container_history);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sessionId = prefs.getString(KEY_SESSION, null);
        if (sessionId == null) {
            Toast.makeText(getContext(),
                    "Session ID not found.", Toast.LENGTH_LONG).show();
        } else {
            fetchHistory(sessionId);
        }
        return view;
    }

    private void fetchHistory(String sessionId) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        JSONObject payload = new JSONObject();
        try { payload.put("session_id", sessionId); } catch (JSONException e) {
            Log.e(TAG, "JSON payload error", e);
            return;
        }

        RequestBody body = RequestBody.create(
                payload.toString(), MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url(BASE_URL + "/history")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "History network error", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Unable to load history", Toast.LENGTH_SHORT).show()
                );
            }
            @Override public void onResponse(@NonNull Call call,
                                             @NonNull Response response)
                    throws IOException {
                String raw = response.body()!=null?response.body().string():"";
                Log.d(TAG, "history raw: "+raw);
                try {
                    JSONArray arr = new JSONArray(raw);
                    requireActivity().runOnUiThread(() -> {
                        containerHistory.removeAllViews();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = null;
                            try {
                                obj = arr.getJSONObject(i);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            String company = obj.optString("company");
                            String role    = obj.optString("role_title");
                            int score      = obj.optInt("match_score");

                            // little box
                            TextView tv = new TextView(getContext());
                            tv.setText(company + " - " + role + " : " + score + "%");
                            tv.setBackgroundResource(R.drawable.card_background);
                            tv.setPadding(24,16,24,16);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                            lp.setMargins(0, 8, 0, 8);
                            tv.setLayoutParams(lp);
                            containerHistory.addView(tv);
                        }
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "History parse error", e);
                }
            }
        });
    }
}