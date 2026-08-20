package com.codereviewassistant.backend.dto;

import java.util.List;

public class AnalysisResult {

    private String report;
    private int score;
    private List<ReviewIssue> issues;

    public AnalysisResult() {
    }

    public AnalysisResult(String report, int score, List<ReviewIssue> issues) {
        this.report = report;
        this.score = score;
        this.issues = issues;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<ReviewIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ReviewIssue> issues) {
        this.issues = issues;
    }
}