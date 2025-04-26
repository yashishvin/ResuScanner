package com.example.resuscanner.models;

public class ResumeHistoryItem {
    private String date;
    private int score;

    public ResumeHistoryItem(String date, int score) {
        this.date = date;
        this.score = score;
    }

    public String getDate() {
        return date;
    }

    public int getScore() {
        return score;
    }
}
