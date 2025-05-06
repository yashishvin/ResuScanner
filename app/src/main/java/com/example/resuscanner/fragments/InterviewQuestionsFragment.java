package com.example.resuscanner.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.example.resuscanner.R;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private TextView scrollIndicator;

    // List of category tags to highlight
    private final List<String> categoryTags = Arrays.asList(
            "Behavioral", "Technical", "System Design", "Python", "AWS",
            "Kubernetes", "DevOps", "JavaScript", "Siemens NX", "Missing Skills",
            "Relevant Projects", "Company Cultural Fit"
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interview_questions,
                container, false);
        containerQuestions = view.findViewById(R.id.container_questions);
        scrollIndicator = view.findViewById(R.id.scroll_indicator);

        // Set up scroll indicator animation
        AlphaAnimation fadeAnimation = new AlphaAnimation(1.0f, 0.5f);
        fadeAnimation.setDuration(1000);
        fadeAnimation.setRepeatCount(Animation.INFINITE);
        fadeAnimation.setRepeatMode(Animation.REVERSE);
        scrollIndicator.startAnimation(fadeAnimation);

        // Hide scroll indicator after first scroll
        ((androidx.core.widget.NestedScrollView) view).setOnScrollChangeListener(
                (androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                        (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                            if (scrollY > 0 && scrollIndicator.getVisibility() == View.VISIBLE) {
                                scrollIndicator.clearAnimation();
                                scrollIndicator.setVisibility(View.GONE);
                            }
                        });

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
        // Show loading state
        containerQuestions.removeAllViews();
        addLoadingCard();

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
                requireActivity().runOnUiThread(() -> {
                    containerQuestions.removeAllViews();
                    addErrorCard("Network error: " + e.getMessage());
                    Toast.makeText(getContext(),
                            "Network error fetching questions: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
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
                    List<QuestionItem> questionItems = parseQuestions(qObj);
                    requireActivity().runOnUiThread(() -> displayQuestions(questionItems));
                } catch (JSONException e) {
                    Log.e(TAG, "Parse error in questions response", e);
                    requireActivity().runOnUiThread(() -> {
                        containerQuestions.removeAllViews();
                        addErrorCard("Parse error in questions response");
                        Toast.makeText(getContext(),
                                "Parse error in questions response", Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    // Class to hold question info including type
    private static class QuestionItem {
        enum Type {
            GREETING,
            QUESTION,
            CLOSING,
            OTHER
        }

        String text;
        Type type;
        String category;

        QuestionItem(String text, Type type) {
            this.text = text;
            this.type = type;
            this.category = "";
        }

        QuestionItem(String text, Type type, String category) {
            this.text = text;
            this.type = type;
            this.category = category;
        }
    }

    // Parse questions from different possible response formats
    private List<QuestionItem> parseQuestions(Object qObj) {
        List<QuestionItem> questions = new ArrayList<>();

        try {
            if (qObj instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) qObj;
                for (int i = 0; i < jsonArray.length(); i++) {
                    String text = jsonArray.getString(i);
                    questions.add(parseQuestionText(text, i, jsonArray.length()));
                }
            }
            else if (qObj instanceof String) {
                String fullText = (String) qObj;

                // First check if it starts with a greeting
                if (fullText.startsWith("Sure!") || fullText.contains("interview questions tailored")) {
                    int endOfGreeting = fullText.indexOf("1.");
                    if (endOfGreeting > 0) {
                        String greeting = fullText.substring(0, endOfGreeting).trim();
                        questions.add(new QuestionItem(greeting, QuestionItem.Type.GREETING));

                        // Remove greeting from fullText
                        fullText = fullText.substring(endOfGreeting);
                    }
                }

                // Find numbered questions (starting with 1., 2., etc.)
                Pattern pattern = Pattern.compile("(\\d+\\.)\\s*([^\\d\\n]*?:)?\\s*([^\\d].*?)(?=\\d+\\.|$)", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(fullText);

                int questionCount = 0;

                while (matcher.find()) {
                    questionCount++;
                    String number = matcher.group(1);
                    String possibleCategory = matcher.group(2);
                    String questionText = matcher.group(3);

                    if (questionText != null) {
                        questionText = questionText.replaceAll("\\\\n", " ")
                                .replaceAll("\\s+", " ")
                                .trim();

                        // Check if this is an actual question or closing notes
                        if (questionText.contains("Remember,") ||
                                questionText.contains("Good luck") ||
                                questionText.toLowerCase().contains("interview")) {
                            questions.add(new QuestionItem(questionText, QuestionItem.Type.CLOSING));
                        } else {
                            // Extract category if present
                            String category = "";
                            if (possibleCategory != null && !possibleCategory.isEmpty()) {
                                category = possibleCategory.replace(":", "").trim();
                            } else {
                                // Try to find category in the question text
                                for (String tag : categoryTags) {
                                    if (questionText.startsWith(tag + ":") ||
                                            questionText.contains("(" + tag + ")")) {
                                        category = tag;
                                        // Remove category from question text if it's at the start
                                        if (questionText.startsWith(tag + ":")) {
                                            questionText = questionText.substring(tag.length() + 1).trim();
                                        }
                                        break;
                                    }
                                }
                            }

                            questions.add(new QuestionItem(questionText, QuestionItem.Type.QUESTION, category));
                        }
                    }
                }

                // If no questions found with the regex, add the whole text as OTHER
                if (questionCount == 0) {
                    questions.add(new QuestionItem(fullText, QuestionItem.Type.OTHER));
                }
            }
            else if (qObj instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) qObj;
                String rawQs = jsonObject.optString("raw", "");

                // Process the raw text recursively
                List<QuestionItem> parsedItems = parseQuestions(rawQs);
                questions.addAll(parsedItems);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing questions", e);
            questions.add(new QuestionItem("Error parsing questions: " + e.getMessage(),
                    QuestionItem.Type.OTHER));
        }

        return questions;
    }

    // Parse an individual question text
    private QuestionItem parseQuestionText(String text, int index, int totalItems) {
        // Check if this is a greeting (first item)
        if (index == 0 && (text.startsWith("Sure!") || text.contains("interview questions"))) {
            return new QuestionItem(text, QuestionItem.Type.GREETING);
        }

        // Check if this is a closing note (last item)
        if (index == totalItems - 1 && (text.contains("Good luck") || text.contains("Remember,"))) {
            return new QuestionItem(text, QuestionItem.Type.CLOSING);
        }

        // Check for category prefix
        String category = "";
        String questionText = text;

        // Try to extract category
        for (String tag : categoryTags) {
            if (text.startsWith(tag + ":")) {
                category = tag;
                questionText = text.substring(tag.length() + 1).trim();
                break;
            } else if (text.contains("(" + tag + "):")) {
                category = tag;
                int startIdx = text.indexOf("(" + tag + "):");
                questionText = text.substring(startIdx + tag.length() + 3).trim();
                break;
            } else if (text.matches("\\d+\\.\\s+" + tag + ":.*")) {
                category = tag;
                Pattern p = Pattern.compile("\\d+\\.\\s+" + tag + ":\\s+(.*)");
                Matcher m = p.matcher(text);
                if (m.find()) {
                    questionText = m.group(1);
                }
                break;
            }
        }

        return new QuestionItem(questionText, QuestionItem.Type.QUESTION, category);
    }

    private void displayQuestions(List<QuestionItem> questionItems) {
        containerQuestions.removeAllViews();

        if (questionItems.isEmpty()) {
            addErrorCard("No questions generated. Try analyzing your resume and a job description first.");
            return;
        }

        int questionNumber = 0;

        // Add each question as a card
        for (int i = 0; i < questionItems.size(); i++) {
            QuestionItem item = questionItems.get(i);

            View cardView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_interview_question, containerQuestions, false);

            TextView tvTitle = cardView.findViewById(R.id.tv_question_number);
            TextView tvContent = cardView.findViewById(R.id.tv_question_text);

            // Set appropriate title based on question type
            switch (item.type) {
                case GREETING:
                    tvTitle.setText("Introduction");
                    tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_500));
                    tvContent.setText(item.text);
                    break;

                case CLOSING:
                    tvTitle.setText("Note");
                    tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_500));
                    tvContent.setText(item.text);
                    break;

                case QUESTION:
                    questionNumber++;

                    // Set question title with category if available
                    if (!item.category.isEmpty()) {
                        tvTitle.setText("Question " + questionNumber + " (" + item.category + ")");
                    } else {
                        tvTitle.setText("Question " + questionNumber);
                    }

                    // Format and set question text
                    SpannableString spannableString = new SpannableString(item.text);
                    tvContent.setText(spannableString);
                    break;

                case OTHER:
                default:
                    tvTitle.setText("Content " + (i + 1));
                    tvContent.setText(item.text);
                    break;
            }

            // Add animation for staggered appearance
            cardView.setAlpha(0f);
            cardView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(i * 100L)
                    .start();

            containerQuestions.addView(cardView);
        }
    }

    private void addLoadingCard() {
        View loadingView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_interview_question, containerQuestions, false);

        TextView questionNumber = loadingView.findViewById(R.id.tv_question_number);
        TextView questionContent = loadingView.findViewById(R.id.tv_question_text);

        questionNumber.setText("Preparing Questions");
        questionContent.setText("Generating personalized interview questions based on your resume and the job description...");

        containerQuestions.addView(loadingView);
    }

    private void addErrorCard(String errorMessage) {
        View errorView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_interview_question, containerQuestions, false);

        TextView questionNumber = errorView.findViewById(R.id.tv_question_number);
        TextView questionContent = errorView.findViewById(R.id.tv_question_text);

        questionNumber.setText("Error");
        // Use Color.RED instead of a resource
        questionNumber.setTextColor(Color.RED);
        questionContent.setText(errorMessage);

        containerQuestions.addView(errorView);
    }
}