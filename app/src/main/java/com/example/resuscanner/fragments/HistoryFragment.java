package com.example.resuscanner.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.resuscanner.R;
import com.example.resuscanner.adapters.HistoryAdapter;
import com.example.resuscanner.models.ResumeHistoryItem;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvResumeHistory;
    private HistoryAdapter adapter;
    private List<ResumeHistoryItem> historyList;

    public HistoryFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvResumeHistory = view.findViewById(R.id.rv_resume_history);
        rvResumeHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        historyList = new ArrayList<>();
        // Dummy data
        historyList.add(new ResumeHistoryItem("April 25, 2025", 85));
        historyList.add(new ResumeHistoryItem("April 20, 2025", 78));
        historyList.add(new ResumeHistoryItem("April 15, 2025", 92));

        adapter = new HistoryAdapter(historyList);
        rvResumeHistory.setAdapter(adapter);

        return view;
    }
}
