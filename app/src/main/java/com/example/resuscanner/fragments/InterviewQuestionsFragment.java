package com.example.resuscanner.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.resuscanner.R;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class InterviewQuestionsFragment extends Fragment {

    private ListView lvInterviewQuestions;

    public InterviewQuestionsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interview_questions, container, false);

        lvInterviewQuestions = view.findViewById(R.id.lv_interview_questions);

        String[] questions = {
                "Tell me about yourself.",
                "Why do you want this job?",
                "Describe a challenge you faced.",
                "Where do you see yourself in 5 years?",
                "Explain OOP concepts.",
                "What is REST API?"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, questions);
        lvInterviewQuestions.setAdapter(adapter);

        return view;
    }
}
