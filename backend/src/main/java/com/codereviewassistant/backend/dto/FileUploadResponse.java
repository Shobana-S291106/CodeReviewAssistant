package com.codereviewassistant.backend.dto;

import java.util.List;

public class FileUploadResponse {

    private String fileName;
    private String message;
    private String report;
    private List<ReviewIssue> issues;
    private int score;

    // Default constructor
    public FileUploadResponse() {
    }

    // Existing 4-parameter constructor
    public FileUploadResponse(
            String fileName,
            String message,
            String report,
            List<ReviewIssue> issues
    ) {
        this.fileName = fileName;
        this.message = message;
        this.report = report;
        this.issues = issues;
        this.score = 0;
    }

    // New 5-parameter constructor
    public FileUploadResponse(
            String fileName,
            String message,
            String report,
            List<ReviewIssue> issues,
            int score
    ) {
        this.fileName = fileName;
        this.message = message;
        this.report = report;
        this.issues = issues;
        this.score = score;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public List<ReviewIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ReviewIssue> issues) {
        this.issues = issues;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}